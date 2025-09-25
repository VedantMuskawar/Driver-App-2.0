package com.pave.driversapp.domain.repository

import com.pave.driversapp.domain.model.Trip
import com.pave.driversapp.domain.model.LocationPoint
import com.pave.driversapp.domain.model.TripMetrics
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.tasks.await
import com.google.firebase.firestore.ktx.snapshots

interface TripsRepository {
    suspend fun createTrip(trip: Trip): Result<String>
    suspend fun getActiveTrip(driverId: String): Trip?
    suspend fun updateTripStatus(tripId: String, status: com.pave.driversapp.domain.model.TripStatus): Result<Unit>
    suspend fun addLocationPoint(tripId: String, location: LocationPoint): Result<Unit>
    suspend fun completeTrip(tripId: String, finalMeterReading: Int): Result<TripMetrics>
    suspend fun cancelTrip(tripId: String, cancelledBy: String): Result<Unit>
    suspend fun getTripLocations(tripId: String): Flow<List<LocationPoint>>
    suspend fun uploadDeliveryImage(tripId: String, imageUri: String): Result<String>
}

class TripsRepositoryImpl(
    private val firestore: com.google.firebase.firestore.FirebaseFirestore,
    private val context: android.content.Context
) : TripsRepository {
    
    private val tripsCollection = firestore.collection("TRIPS")
    private val locationsCollection = firestore.collection("TRIPS")
    
    override suspend fun createTrip(trip: Trip): Result<String> {
        return try {
            // Validate trip data
            if (trip.orderId.isBlank() || trip.driverId.isBlank() || trip.orgId.isBlank()) {
                return Result.failure(IllegalArgumentException("Invalid trip data: missing required fields"))
            }
            
            val tripDoc = tripsCollection.document()
            val tripWithId = trip.copy(tripId = tripDoc.id)
            
            tripDoc.set(tripWithId)
                .await()
            
            android.util.Log.d("TripsRepository", "✅ Trip created successfully: ${tripDoc.id}")
            Result.success(tripDoc.id)
        } catch (e: Exception) {
            android.util.Log.e("TripsRepository", "❌ Error creating trip: ${e.message}")
            Result.failure(e)
        }
    }
    
    override suspend fun getActiveTrip(driverId: String): Trip? {
        return try {
            if (driverId.isBlank()) {
                android.util.Log.w("TripsRepository", "⚠️ Empty driverId provided")
                return null
            }
            
            val snapshot = tripsCollection
                .whereEqualTo("driverId", driverId)
                .whereEqualTo("active", true)
                .limit(1)
                .get()
                .await()
            
            val trip = snapshot.documents.firstOrNull()?.toObject(Trip::class.java)
            android.util.Log.d("TripsRepository", "📋 Active trip query result: ${if (trip != null) "Found" else "Not found"}")
            trip
        } catch (e: Exception) {
            android.util.Log.e("TripsRepository", "❌ Error getting active trip: ${e.message}")
            null
        }
    }
    
    override suspend fun updateTripStatus(tripId: String, status: com.pave.driversapp.domain.model.TripStatus): Result<Unit> {
        return try {
            val updateData = mutableMapOf<String, Any>(
                "status" to status.name,
                "updatedAt" to com.google.firebase.Timestamp.now()
            )
            
            when (status) {
                com.pave.driversapp.domain.model.TripStatus.DISPATCHED -> {
                    updateData["dispatchedAt"] = com.google.firebase.Timestamp.now()
                }
                com.pave.driversapp.domain.model.TripStatus.DELIVERED -> {
                    updateData["deliveredAt"] = com.google.firebase.Timestamp.now()
                }
                com.pave.driversapp.domain.model.TripStatus.RETURNED -> {
                    updateData["returnedAt"] = com.google.firebase.Timestamp.now()
                    updateData["active"] = false
                }
                com.pave.driversapp.domain.model.TripStatus.CANCELLED -> {
                    updateData["cancelledAt"] = com.google.firebase.Timestamp.now()
                    updateData["active"] = false
                }
            }
            
            tripsCollection.document(tripId)
                .update(updateData)
                .await()
            
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    override suspend fun addLocationPoint(tripId: String, location: LocationPoint): Result<Unit> {
        return try {
            val locationDoc = locationsCollection
                .document(tripId)
                .collection("LOCATIONS")
                .document()
            
            val locationWithTimestamp = location.copy(
                timestamp = com.google.firebase.Timestamp.now()
            )
            
            locationDoc.set(locationWithTimestamp)
                .await()
            
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    override suspend fun completeTrip(tripId: String, finalMeterReading: Int): Result<TripMetrics> {
        return try {
            // Get all location points for this trip
            val locationPoints = locationsCollection
                .document(tripId)
                .collection("LOCATIONS")
                .orderBy("timestamp")
                .get()
                .await()
                .toObjects(LocationPoint::class.java)
            
            // Calculate metrics
            val metrics = calculateTripMetrics(locationPoints)
            
            // Update trip with final data
            tripsCollection.document(tripId)
                .update(
                    mapOf(
                        "lastMeterReading" to finalMeterReading,
                        "distanceTravelled" to metrics.totalDistance,
                        "duration" to metrics.totalDuration,
                        "status" to com.pave.driversapp.domain.model.TripStatus.RETURNED.name,
                        "returnedAt" to com.google.firebase.Timestamp.now(),
                        "active" to false,
                        "updatedAt" to com.google.firebase.Timestamp.now()
                    )
                )
                .await()
            
            Result.success(metrics)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    override suspend fun cancelTrip(tripId: String, cancelledBy: String): Result<Unit> {
        return try {
            tripsCollection.document(tripId)
                .update(
                    mapOf(
                        "status" to com.pave.driversapp.domain.model.TripStatus.CANCELLED.name,
                        "cancelledBy" to cancelledBy,
                        "cancelledAt" to com.google.firebase.Timestamp.now(),
                        "active" to false,
                        "updatedAt" to com.google.firebase.Timestamp.now()
                    )
                )
                .await()
            
            Result.success(Unit)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    override suspend fun getTripLocations(tripId: String): Flow<List<LocationPoint>> {
        return try {
            locationsCollection
                .document(tripId)
                .collection("LOCATIONS")
                .orderBy("timestamp", com.google.firebase.firestore.Query.Direction.ASCENDING)
                .snapshots()
                .map { snapshot ->
                    snapshot.documents.mapNotNull { document ->
                        try {
                            document.toObject(LocationPoint::class.java)
                        } catch (e: Exception) {
                            android.util.Log.e("TripsRepository", "❌ Error parsing location document ${document.id}: ${e.message}")
                            null
                        }
                    }
                }
        } catch (e: Exception) {
            android.util.Log.e("TripsRepository", "❌ Error setting up location listener: ${e.message}")
            kotlinx.coroutines.flow.flowOf(emptyList())
        }
    }
    
    override suspend fun uploadDeliveryImage(tripId: String, imageUri: String): Result<String> {
        return try {
            // For now, return the URI as the reference
            // In a real implementation, you'd upload to Firebase Storage
            Result.success(imageUri)
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
    private fun calculateTripMetrics(locationPoints: List<LocationPoint>): TripMetrics {
        if (locationPoints.size < 2) {
            return TripMetrics(0.0, 0, 0.0, 0.0, locationPoints)
        }
        
        // Simplified implementation - calculate basic metrics
        val duration = if (locationPoints.isNotEmpty()) {
            val startTime = locationPoints.first().timestamp?.seconds ?: 0
            val endTime = locationPoints.last().timestamp?.seconds ?: 0
            (endTime - startTime) / 60 // minutes
        } else 0
        
        // Simplified distance calculation
        val totalDistance = locationPoints.size * 0.1 // Rough estimate
        val averageSpeed = if (duration > 0) {
            totalDistance / (duration / 60.0) // km/h
        } else 0.0
        
        return TripMetrics(
            totalDistance = totalDistance,
            totalDuration = duration,
            averageSpeed = averageSpeed,
            maxSpeed = averageSpeed,
            locationPoints = locationPoints
        )
    }
}

// Extension functions for Firestore - removed for now to fix compilation

private fun com.google.firebase.firestore.QuerySnapshot.toObjects(clazz: Class<LocationPoint>): List<LocationPoint> {
    return documents.mapNotNull { 
        try {
            it.toObject(clazz)
        } catch (e: Exception) {
            null
        }
    }
}

private fun com.google.firebase.firestore.DocumentSnapshot.toObject(clazz: Class<Trip>): Trip? {
    return try {
        toObject(clazz)
    } catch (e: Exception) {
        null
    }
}
