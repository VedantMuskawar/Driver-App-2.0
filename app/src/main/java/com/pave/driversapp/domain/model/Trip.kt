package com.pave.driversapp.domain.model

import kotlinx.serialization.Serializable
import com.google.firebase.Timestamp
import kotlinx.serialization.Contextual

@Serializable
data class Trip(
    val tripId: String,
    val orderId: String,
    val driverId: String,
    val orgId: String,
    val vehicleNumber: String,
    val status: TripStatus,
    val active: Boolean = true,
    val initialMeterReading: Int? = null,
    val lastMeterReading: Int? = null,
    val distanceTravelled: Double? = null,
    val duration: Long? = null, // in minutes
    val deliveryImg: String? = null,
    val deliveryImgRef: String? = null,
    val cancelledBy: String? = null,
    @Contextual val dispatchedAt: Timestamp? = null,
    @Contextual val deliveredAt: Timestamp? = null,
    @Contextual val returnedAt: Timestamp? = null,
    @Contextual val cancelledAt: Timestamp? = null,
    @Contextual val createdAt: Timestamp? = null,
    @Contextual val updatedAt: Timestamp? = null
)

@Serializable
enum class TripStatus {
    DISPATCHED,
    DELIVERED,
    RETURNED,
    CANCELLED
}

@Serializable
data class LocationPoint(
    val lat: Double,
    val lng: Double,
    @Contextual val timestamp: Timestamp? = null
)

@Serializable
data class TripMetrics(
    val totalDistance: Double, // in kilometers
    val totalDuration: Long, // in minutes
    val averageSpeed: Double, // km/h
    val maxSpeed: Double, // km/h
    val locationPoints: List<LocationPoint>
)
