package com.pave.driversapp.util

import android.Manifest
import android.app.Activity
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.location.Location
import android.os.Build
import androidx.core.app.ActivityCompat
import androidx.core.content.ContextCompat
import com.google.android.gms.location.*
import com.pave.driversapp.service.LocationTrackingService
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

class LocationManager(
    private val context: Context
) {
    
    private val fusedLocationClient: FusedLocationProviderClient = 
        LocationServices.getFusedLocationProviderClient(context)
    
    companion object {
        const val LOCATION_PERMISSION_REQUEST_CODE = 1001
        const val BACKGROUND_LOCATION_PERMISSION_REQUEST_CODE = 1002
        
        val REQUIRED_PERMISSIONS = arrayOf(
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION
        )
        
        val BACKGROUND_PERMISSIONS = arrayOf(
            Manifest.permission.ACCESS_BACKGROUND_LOCATION
        )
    }
    
    fun hasLocationPermissions(): Boolean {
        return REQUIRED_PERMISSIONS.all { permission ->
            ContextCompat.checkSelfPermission(context, permission) == PackageManager.PERMISSION_GRANTED
        }
    }
    
    fun hasBackgroundLocationPermission(): Boolean {
        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ContextCompat.checkSelfPermission(
                context, 
                Manifest.permission.ACCESS_BACKGROUND_LOCATION
            ) == PackageManager.PERMISSION_GRANTED
        } else {
            true // Background location permission not required for older versions
        }
    }
    
    fun requestLocationPermissions(activity: Activity) {
        ActivityCompat.requestPermissions(
            activity,
            REQUIRED_PERMISSIONS,
            LOCATION_PERMISSION_REQUEST_CODE
        )
    }
    
    fun requestBackgroundLocationPermission(activity: Activity) {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ActivityCompat.requestPermissions(
                activity,
                BACKGROUND_PERMISSIONS,
                BACKGROUND_LOCATION_PERMISSION_REQUEST_CODE
            )
        }
    }
    
    suspend fun getCurrentLocation(): Location? = suspendCancellableCoroutine { continuation ->
        if (!hasLocationPermissions()) {
            continuation.resumeWithException(SecurityException("Location permissions not granted"))
            return@suspendCancellableCoroutine
        }
        
        fusedLocationClient.lastLocation.addOnSuccessListener { location ->
            continuation.resume(location)
        }.addOnFailureListener { exception ->
            continuation.resumeWithException(exception)
        }
        
        continuation.invokeOnCancellation {
            // Handle cancellation if needed
        }
    }
    
    suspend fun getCurrentLocationWithTimeout(timeoutMs: Long = 10000): Location? = suspendCancellableCoroutine { continuation ->
        if (!hasLocationPermissions()) {
            continuation.resumeWithException(SecurityException("Location permissions not granted"))
            return@suspendCancellableCoroutine
        }
        
        val locationRequest = LocationRequest.Builder(
            Priority.PRIORITY_HIGH_ACCURACY,
            1000 // 1 second
        ).apply {
            setMaxUpdateDelayMillis(timeoutMs)
        }.build()
        
        val locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                super.onLocationResult(locationResult)
                locationResult.lastLocation?.let { location ->
                    fusedLocationClient.removeLocationUpdates(this)
                    continuation.resume(location)
                }
            }
        }
        
        try {
            fusedLocationClient.requestLocationUpdates(
                locationRequest,
                locationCallback,
                android.os.Looper.getMainLooper()
            )
        } catch (e: SecurityException) {
            continuation.resumeWithException(e)
        }
        
        continuation.invokeOnCancellation {
            fusedLocationClient.removeLocationUpdates(locationCallback)
        }
    }
    
    fun startLocationTrackingService(tripId: String) {
        android.util.Log.d("LocationManager", "🚀 Starting location tracking service for trip: $tripId")
        
        val intent = Intent(context, LocationTrackingService::class.java).apply {
            putExtra("tripId", tripId)
        }
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            context.startForegroundService(intent)
        } else {
            context.startService(intent)
        }
    }
    
    fun stopLocationTrackingService() {
        android.util.Log.d("LocationManager", "🛑 Stopping location tracking service")
        
        val intent = Intent(context, LocationTrackingService::class.java)
        context.stopService(intent)
    }
    
    fun isLocationTrackingServiceRunning(): Boolean {
        val activityManager = context.getSystemService(Context.ACTIVITY_SERVICE) as android.app.ActivityManager
        val runningServices = activityManager.getRunningServices(Integer.MAX_VALUE)
        
        return runningServices.any { serviceInfo ->
            serviceInfo.service.className == LocationTrackingService::class.java.name
        }
    }
}
