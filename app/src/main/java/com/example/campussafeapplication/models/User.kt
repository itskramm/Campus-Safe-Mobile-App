package com.example.campussafeapplication.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class User(
    val id: String? = null,
    val email: String,
    @SerialName("full_name")
    val fullName: String? = null,
    @SerialName("phone_number")
    val phoneNumber: String? = null,
    @SerialName("biometric_enabled")
    val biometricEnabled: Boolean = false,
    @SerialName("created_at")
    val createdAt: String? = null
)
