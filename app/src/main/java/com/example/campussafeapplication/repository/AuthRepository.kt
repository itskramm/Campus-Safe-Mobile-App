package com.example.campussafeapplication.repository

import com.example.campussafeapplication.models.User
import com.example.campussafeapplication.supabase.SupabaseClient
import io.github.jan.supabase.gotrue.auth
import io.github.jan.supabase.gotrue.providers.builtin.Email
import io.github.jan.supabase.gotrue.providers.builtin.IDToken
import io.github.jan.supabase.postgrest.from

class AuthRepository {
    
    private val client = SupabaseClient.client
    
    /**
     * Sign up a new user with email and password
     */
    suspend fun signUp(email: String, password: String, fullName: String): Result<User> {
        return try {
            // Create auth user
            client.auth.signUpWith(Email) {
                this.email = email
                this.password = password
            }
            
            // Get the created user
            val authUser = client.auth.currentUserOrNull()
            
            if (authUser != null) {
                // Create user profile in users table
                val user = User(
                    id = authUser.id,
                    email = email,
                    fullName = fullName
                )

                try {
                    client.from("users").insert(user)
                } catch (e: Exception) {
                    if (!isDuplicateKeyError(e)) {
                        return Result.failure(e)
                    }
                }

                Result.success(user)
            } else {
                Result.failure(Exception("Failed to create user"))
            }
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    /**
     * Sign in with email and password
     */
    suspend fun signIn(email: String, password: String): Result<User> {
        return try {
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
                    Result.success(users.first())
                } else {
                    // If auth succeeds but profile is missing, create a basic one
                    val newUser = User(
                        id = authUser.id,
                        email = authUser.email ?: email,
                        fullName = authUser.userMetadata?.get("full_name")?.toString() ?: "User"
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
     * Sign in with Google ID Token
     */
    suspend fun signInWithGoogle(idToken: String): Result<User> {
        return try {
            client.auth.signInWith(IDToken) {
                this.idToken = idToken
                this.provider = io.github.jan.supabase.gotrue.providers.Google
            }

            val authUser = client.auth.currentUserOrNull()

            if (authUser != null) {
                // Check if profile exists
                val users = client.from("users")
                    .select {
                        filter {
                            eq("id", authUser.id)
                        }
                    }
                    .decodeList<User>()

                if (users.isNotEmpty()) {
                    Result.success(users.first())
                } else {
                    // Create new profile for Google user
                    val user = User(
                        id = authUser.id,
                        email = authUser.email ?: "",
                        fullName = authUser.userMetadata?.get("full_name")?.toString() ?: "Google User"
                    )
                    client.from("users").insert(user)
                    Result.success(user)
                }
            } else {
                Result.failure(Exception("Authentication failed"))
            }
        } catch (e: Exception) {
            Result.failure(e)
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
}
