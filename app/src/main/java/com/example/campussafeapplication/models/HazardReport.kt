package com.example.campussafeapplication.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
data class HazardReport(
    val id: String? = null,
    @SerialName("user_id")
    val userId: String,
    val title: String,
    val building: String,
    val floor: String,
    val room: String,
    val description: String,
    val status: String = "Pending", // "Pending", "In Progress", "Resolved"
    @SerialName("hazard_type")
    val hazardType: String = "General",
    val location: String? = null,
    val latitude: Double? = 0.0,
    val longitude: Double? = 0.0,
    val severity: String = "Medium",
    @SerialName("image_url")
    val imageUrl: String? = null,
    @SerialName("created_at")
    val createdAt: String? = null,
    @SerialName("updated_at")
    val updatedAt: String? = null
)
