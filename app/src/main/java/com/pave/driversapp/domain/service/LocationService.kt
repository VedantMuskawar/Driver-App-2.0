package com.pave.driversapp.domain.service

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Location
import androidx.core.content.ContextCompat
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.pave.driversapp.domain.model.DepotLocation
import com.pave.driversapp.domain.model.DepotSettings
import com.pave.driversapp.domain.repository.DepotRepository
import kotlinx.coroutines.tasks.await

class LocationService(
    private val context: Context,
    private val depotRepository: DepotRepository
) {
    
    private val fusedLocationClient: FusedLocationProviderClient = 
        LocationServices.getFusedLocationProviderClient(context)
    
    suspend fun getCurrentLocation(): DepotLocation? {
        return try {
            if (!hasLocationPermission()) {
                android.util.Log.w("LocationService", "⚠️ Location permission not granted")
                return null
            }
            
            val location = fusedLocationClient.lastLocation.await()
            if (location != null) {
                val depotLocation = DepotLocation(location.latitude, location.longitude)
                android.util.Log.d("LocationService", "📍 Current location: ${depotLocation.lat}, ${depotLocation.lng}")
                depotLocation
            } else {
                android.util.Log.w("LocationService", "⚠️ No location available")
                null
            }
        } catch (e: SecurityException) {
            android.util.Log.e("LocationService", "❌ Security exception getting location", e)
            null
        } catch (e: Exception) {
            android.util.Log.e("LocationService", "❌ Error getting location", e)
            null
        }
    }
    
    suspend fun checkDepotAccess(orgId: String): DepotAccessResult {
        return try {
            android.util.Log.d("LocationService", "🔍 Checking depot access for org: $orgId")
            
            // Get depot settings
            val depotResult = depotRepository.getDepot(orgId)
            if (depotResult.isFailure) {
                android.util.Log.e("LocationService", "❌ Failed to get depot settings")
                return DepotAccessResult.Error("Failed to load depot settings")
            }
            
            val depotSettings = depotResult.getOrNull()
            if (depotSettings == null) {
                android.util.Log.w("LocationService", "⚠️ No depot configured for org: $orgId")
                return DepotAccessResult.NoDepotConfigured
            }
            
            // Get current location
            val currentLocation = getCurrentLocation()
            if (currentLocation == null) {
                android.util.Log.w("LocationService", "⚠️ Could not get current location")
                return DepotAccessResult.LocationUnavailable
            }
            
            // Check if inside depot
            val isInside = depotRepository.isInsideDepot(currentLocation, depotSettings)
            android.util.Log.d("LocationService", "📍 Depot access check result: $isInside")
            
            if (isInside) {
                DepotAccessResult.AccessGranted(currentLocation, depotSettings)
            } else {
                DepotAccessResult.AccessDenied(currentLocation, depotSettings)
            }
            
        } catch (e: Exception) {
            android.util.Log.e("LocationService", "❌ Error checking depot access", e)
            DepotAccessResult.Error("Error checking depot access: ${e.message}")
        }
    }
    
    private fun hasLocationPermission(): Boolean {
        return ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    }
}

sealed class DepotAccessResult {
    data class AccessGranted(
        val currentLocation: DepotLocation,
        val depotSettings: DepotSettings
    ) : DepotAccessResult()
    
    data class AccessDenied(
        val currentLocation: DepotLocation,
        val depotSettings: DepotSettings
    ) : DepotAccessResult()
    
    object NoDepotConfigured : DepotAccessResult()
    object LocationUnavailable : DepotAccessResult()
    data class Error(val message: String) : DepotAccessResult()
}
