package com.example.campussafeapplication.models

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.Transient

@Serializable
data class HazardReport(
    val id: String? = null,
    @SerialName("user_id")
    val userId: String? = null,
    val title: String? = null,
    val building: String? = null,
    val floor: String? = null,
    val room: String? = null,
    val description: String? = null,
    val status: String? = "Pending", // "Pending", "In Progress", "Resolved"
    @SerialName("hazard_type")
    val hazardType: String? = "General",
    val location: String? = null,
    val latitude: Double? = 0.0,
    val longitude: Double? = 0.0,
    val severity: String? = "Medium",
    @SerialName("image_url")
    val imageUrl: String? = null,
    @SerialName("created_at")
    val createdAt: String? = null,
    @SerialName("updated_at")
    val updatedAt: String? = null,
    @SerialName("reporter_name")
    val reporterName: String? = null
)
