package com.pave.driversapp.domain.model

import com.google.firebase.Timestamp
import kotlinx.serialization.Serializable
import kotlinx.serialization.Contextual

@Serializable
data class DepotSettings(
    val depotLocation: DepotLocation,
    val radius: Int, // meters
    val createdBy: String,
    @Contextual val createdAt: Timestamp? = null,
    @Contextual val updatedAt: Timestamp? = null
)

@Serializable
data class DepotLocation(
    val lat: Double,
    val lng: Double
)
