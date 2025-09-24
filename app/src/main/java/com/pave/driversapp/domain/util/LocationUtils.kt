package com.pave.driversapp.domain.util

import com.pave.driversapp.domain.model.DepotLocation
import kotlin.math.*

object LocationUtils {
    
    /**
     * Calculate distance between two points using Haversine formula
     * @param point1 First location
     * @param point2 Second location
     * @return Distance in meters
     */
    fun calculateDistance(point1: DepotLocation, point2: DepotLocation): Double {
        val earthRadius = 6371000.0 // Earth's radius in meters
        
        val lat1Rad = Math.toRadians(point1.lat)
        val lat2Rad = Math.toRadians(point2.lat)
        val deltaLatRad = Math.toRadians(point2.lat - point1.lat)
        val deltaLngRad = Math.toRadians(point2.lng - point1.lng)
        
        val a = sin(deltaLatRad / 2).pow(2) + 
                cos(lat1Rad) * cos(lat2Rad) * sin(deltaLngRad / 2).pow(2)
        val c = 2 * atan2(sqrt(a), sqrt(1 - a))
        
        return earthRadius * c
    }
    
    /**
     * Check if a location is inside a circular geofence
     * @param currentLocation Current location
     * @param depotLocation Depot center location
     * @param radius Radius in meters
     * @return true if inside the geofence, false otherwise
     */
    fun isInsideDepot(currentLocation: DepotLocation, depotLocation: DepotLocation, radius: Int): Boolean {
        val distance = calculateDistance(currentLocation, depotLocation)
        return distance <= radius
    }
    
    /**
     * Format distance for display
     * @param distanceInMeters Distance in meters
     * @return Formatted distance string
     */
    fun formatDistance(distanceInMeters: Double): String {
        return when {
            distanceInMeters < 1000 -> "${distanceInMeters.toInt()}m"
            else -> "${(distanceInMeters / 1000).let { "%.1f".format(it) }}km"
        }
    }
}
