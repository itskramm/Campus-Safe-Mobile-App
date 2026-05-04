package com.example.campussafeapplication.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class SafetyTip(
    val id: String? = null,
    val title: String,
    val description: String,
    val category: String, // "Fire", "Flood", "Earthquake", "Medical", "Security", "General"
    @SerialName("icon_name")
    val iconName: String? = null,
    @SerialName("created_at")
    val createdAt: String? = null
)
