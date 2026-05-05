package com.example.campussafeapplication.repository

import com.example.campussafeapplication.models.User
import com.example.campussafeapplication.supabase.SupabaseClient
import io.github.jan.supabase.gotrue.auth
import io.github.jan.supabase.gotrue.providers.builtin.Email
import io.github.jan.supabase.postgrest.from

class AuthRepository {
    
    private val client = SupabaseClient.client
    
    /**
     * Sign up a new user with email and password
     */
    suspend fun signUp(email: String, password: String, fullName: String, hardwareId: String): Result<User> {
        return try {
            val normalizedHardwareId = hardwareId.trim()
            if (normalizedHardwareId.isBlank()) {
                return Result.failure(Exception("Unable to identify this device. Please try again."))
            }

            val hardwareAlreadyLinked = try {
                isHardwareIdLinkedToAnotherAccount(normalizedHardwareId)
            } catch (e: Exception) {
                // Signup may run before auth is established, so RLS can block this pre-check.
                // Database uniqueness still enforces one-device-per-account when profile is written.
                if (isRlsReadRestrictionError(e)) {
                    false
                } else {
                    throw e
                }
            }

            if (hardwareAlreadyLinked) {
                return Result.failure(Exception("Only one account per device is allowed."))
            }

            // Create auth user
            client.auth.signUpWith(Email) {
                this.email = email
                this.password = password
            }
            
            // Get the created user
            val authUser = client.auth.currentUserOrNull()
            
            if (authUser != null) {
                // Ensure profile always stores this device ID (covers trigger-created rows too).
                val user = upsertUserProfile(
                    userId = authUser.id,
                    email = email,
                    fullName = fullName,
                    hardwareId = normalizedHardwareId
                )
                Result.success(user)
            } else {
                Result.failure(Exception("Failed to create user"))
            }
        } catch (e: Exception) {
            if (isHardwareConflictError(e)) {
                Result.failure(Exception("Only one account per device is allowed."))
            } else {
                Result.failure(e)
            }
        }
    }
    
    /**
     * Sign in with email and password
     */
    suspend fun signIn(email: String, password: String, hardwareId: String): Result<User> {
        return try {
            val normalizedHardwareId = hardwareId.trim()
            if (normalizedHardwareId.isBlank()) {
                return Result.failure(Exception("Unable to identify this device. Please try again."))
            }

            client.auth.signInWith(Email) {
                this.email = email
                this.password = password
            }
            
            val authUser = client.auth.currentUserOrNull()
            
            if (authUser != null) {
                // Fetch user profile from users table
                val users = client.from("users")
                    .select {
                        filter {
                            eq("id", authUser.id)
                        }
                    }
                    .decodeList<User>()
                
                if (users.isNotEmpty()) {
                    var user = users.first()
                    // Verify hardware ID if it exists
                    if (!user.hardwareId.isNullOrBlank() && user.hardwareId != normalizedHardwareId) {
                        client.auth.signOut()
                        return Result.failure(Exception("This account is linked to another device."))
                    }

                    if (user.hardwareId.isNullOrBlank()) {
                        if (isHardwareIdLinkedToAnotherAccount(normalizedHardwareId, authUser.id)) {
                            client.auth.signOut()
                            return Result.failure(Exception("Only one account per device is allowed."))
                        }
                        user = updateUserHardwareId(authUser.id, normalizedHardwareId)
                    }

                    Result.success(user)
                } else {
                    // If auth succeeds but profile is missing, create a basic one with hardwareId
                    if (isHardwareIdLinkedToAnotherAccount(normalizedHardwareId, authUser.id)) {
                        client.auth.signOut()
                        return Result.failure(Exception("Only one account per device is allowed."))
                    }

                    val newUser = User(
                        id = authUser.id,
                        email = authUser.email ?: email,
                        fullName = authUser.userMetadata?.get("full_name")?.toString() ?: "User",
                        hardwareId = normalizedHardwareId
                    )
                    val insertResult = try {
                        client.from("users").insert(newUser)
                        Result.success(Unit)
                    } catch (e: Exception) {
                        if (isDuplicateKeyError(e)) {
                            Result.success(Unit)
                        } else {
                            Result.failure(e)
                        }
                    }
                    insertResult.onFailure { error ->
                        return Result.failure(
                            Exception(error.message ?: "Failed to create user profile")
                        )
                    }
                    Result.success(newUser)
                }
            } else {
                Result.failure(Exception("Authentication failed: User is null"))
            }
        } catch (e: Exception) {
            e.printStackTrace()
            val errorMessage = when {
                isHardwareConflictError(e) ->
                    "Only one account per device is allowed."
                e.message?.contains("Email not confirmed", ignoreCase = true) == true -> 
                    "Please confirm your email address before logging in."
                e.message?.contains("Invalid login credentials", ignoreCase = true) == true -> 
                    "Invalid email or password."
                else -> e.message ?: "Sign in failed"
            }
            Result.failure(Exception(errorMessage))
        }
    }
    
    /**
     * Sign out current user
     */
    suspend fun signOut(): Result<Unit> {
        return try {
            client.auth.signOut()
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Get current authenticated user
     */
    suspend fun getCurrentUser(): User? {
        return try {
            val authUser = client.auth.currentUserOrNull() ?: return null
            
            val users = client.from("users")
                .select {
                    filter {
                        eq("id", authUser.id)
                    }
                }
                .decodeList<User>()
            
            users.firstOrNull()
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Update current user's profile fields
     */
    suspend fun updateProfile(
        fullName: String,
        email: String,
        phoneNumber: String
    ): Result<User> {
        return try {
            val authUser = client.auth.currentUserOrNull()
                ?: return Result.failure(Exception("No authenticated user"))

            val updates = mapOf(
                "full_name" to fullName,
                "email" to email,
                "phone_number" to phoneNumber
            )

            val user = client.from("users")
                .update(updates) {
                    filter {
                        eq("id", authUser.id)
                    }
                    select()
                }
                .decodeSingle<User>()

            Result.success(user)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /**
     * Update biometric preference for current user
     */
    suspend fun updateBiometricSetting(enabled: Boolean): Result<User> {
        return try {
            val authUser = client.auth.currentUserOrNull()
                ?: return Result.failure(Exception("No authenticated user"))

            val user = client.from("users")
                .update(mapOf("biometric_enabled" to enabled)) {
                    filter {
                        eq("id", authUser.id)
                    }
                    select()
                }
                .decodeSingle<User>()

            Result.success(user)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Check if user is logged in
     */
    fun isLoggedIn(): Boolean {
        return client.auth.currentUserOrNull() != null
    }
    
    /**
     * Reset password
     */
    suspend fun resetPassword(email: String): Result<Unit> {
        return try {
            client.auth.resetPasswordForEmail(email)
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    private fun isDuplicateKeyError(error: Exception): Boolean {
        return error.message?.contains("duplicate key", ignoreCase = true) == true
    }

    private fun isHardwareConflictError(error: Exception): Boolean {
        val message = error.message?.lowercase().orEmpty()
        return "hardware_id" in message &&
            ("duplicate key" in message || "unique constraint" in message)
    }

    private fun isRlsReadRestrictionError(error: Exception): Boolean {
        val message = error.message?.lowercase().orEmpty()
        return "row-level security" in message ||
            "permission denied" in message ||
            "not allowed" in message
    }

    private suspend fun isHardwareIdLinkedToAnotherAccount(
        hardwareId: String,
        currentUserId: String? = null
    ): Boolean {
        val usersWithHardware = client.from("users")
            .select {
                filter {
                    eq("hardware_id", hardwareId)
                }
            }
            .decodeList<User>()

        return usersWithHardware.any { user ->
            currentUserId == null || user.id != currentUserId
        }
    }

    private suspend fun updateUserHardwareId(userId: String, hardwareId: String): User {
        return client.from("users")
            .update(mapOf("hardware_id" to hardwareId)) {
                filter {
                    eq("id", userId)
                }
                select()
            }
            .decodeSingle<User>()
    }

    private suspend fun upsertUserProfile(
        userId: String,
        email: String,
        fullName: String,
        hardwareId: String
    ): User {
        val updates = mapOf(
            "email" to email,
            "full_name" to fullName,
            "hardware_id" to hardwareId
        )

        val updatedRows = client.from("users")
            .update(updates) {
                filter {
                    eq("id", userId)
                }
                select()
            }
            .decodeList<User>()

        if (updatedRows.isNotEmpty()) {
            return updatedRows.first()
        }

        val newUser = User(
            id = userId,
            email = email,
            fullName = fullName,
            hardwareId = hardwareId
        )
        client.from("users").insert(newUser)
        return newUser
    }
}
