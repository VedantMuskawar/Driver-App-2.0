package com.pave.driversapp.service

import android.app.*
import android.content.Context
import android.content.Intent
import android.location.Location
import android.os.Binder
import android.os.Build
import android.os.IBinder
import android.os.Looper
import androidx.core.app.NotificationCompat
import com.google.android.gms.location.*
import com.pave.driversapp.R
import com.pave.driversapp.domain.model.LocationPoint
import com.pave.driversapp.domain.repository.TripsRepository
import kotlinx.coroutines.*
import java.util.concurrent.TimeUnit

class LocationTrackingService : Service() {
    
    private val binder = LocationBinder()
    private var fusedLocationClient: FusedLocationProviderClient? = null
    private var locationCallback: LocationCallback? = null
    private var tripsRepository: TripsRepository? = null
    private var currentTripId: String? = null
    private var isTracking = false
    private var serviceScope = CoroutineScope(Dispatchers.IO + SupervisorJob())
    
    // Location tracking parameters
    private val locationRequest = LocationRequest.Builder(
        Priority.PRIORITY_HIGH_ACCURACY,
        TimeUnit.SECONDS.toMillis(10) // Update every 10 seconds
    ).apply {
        setMinUpdateIntervalMillis(TimeUnit.SECONDS.toMillis(5)) // Minimum 5 seconds
        setMaxUpdateDelayMillis(TimeUnit.SECONDS.toMillis(15)) // Maximum 15 seconds delay
        setWaitForAccurateLocation(false)
    }.build()
    
    inner class LocationBinder : Binder() {
        fun getService(): LocationTrackingService = this@LocationTrackingService
    }
    
    override fun onCreate() {
        super.onCreate()
        android.util.Log.d("LocationService", "🚀 LocationTrackingService created")
        
        fusedLocationClient = LocationServices.getFusedLocationProviderClient(this)
        createNotificationChannel()
        
        locationCallback = object : LocationCallback() {
            override fun onLocationResult(locationResult: LocationResult) {
                super.onLocationResult(locationResult)
                locationResult.lastLocation?.let { location ->
                    handleLocationUpdate(location)
                }
            }
        }
    }
    
    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        android.util.Log.d("LocationService", "📡 LocationTrackingService started")
        
        try {
            val notification = createNotification()
            startForeground(NOTIFICATION_ID, notification)
            android.util.Log.d("LocationService", "✅ Foreground service started successfully")
        } catch (e: SecurityException) {
            android.util.Log.e("LocationService", "❌ Failed to start foreground service: ${e.message}")
            // Continue as regular service if foreground fails
            android.util.Log.w("LocationService", "⚠️ Running as regular service without foreground notification")
        } catch (e: Exception) {
            android.util.Log.e("LocationService", "❌ Unexpected error starting service: ${e.message}")
        }
        
        return START_STICKY
    }
    
    override fun onBind(intent: Intent?): IBinder = binder
    
    override fun onDestroy() {
        super.onDestroy()
        android.util.Log.d("LocationService", "🛑 LocationTrackingService destroyed")
        stopLocationUpdates()
        serviceScope.cancel()
    }
    
    fun startTracking(tripId: String, tripsRepository: TripsRepository) {
        android.util.Log.d("LocationService", "📍 Starting location tracking for trip: $tripId")
        
        this.currentTripId = tripId
        this.tripsRepository = tripsRepository
        this.isTracking = true
        
        startLocationUpdates()
    }
    
    fun stopTracking() {
        android.util.Log.d("LocationService", "🛑 Stopping location tracking")
        
        this.isTracking = false
        this.currentTripId = null
        this.tripsRepository = null
        
        stopLocationUpdates()
    }
    
    private fun startLocationUpdates() {
        if (fusedLocationClient == null || locationCallback == null) {
            android.util.Log.e("LocationService", "❌ Location client or callback not initialized")
            return
        }
        
        try {
            fusedLocationClient!!.requestLocationUpdates(
                locationRequest,
                locationCallback!!,
                Looper.getMainLooper()
            )
            android.util.Log.d("LocationService", "✅ Location updates started")
        } catch (e: SecurityException) {
            android.util.Log.e("LocationService", "❌ Location permission not granted: ${e.message}")
        } catch (e: Exception) {
            android.util.Log.e("LocationService", "❌ Error starting location updates: ${e.message}")
        }
    }
    
    private fun stopLocationUpdates() {
        locationCallback?.let { callback ->
            fusedLocationClient?.removeLocationUpdates(callback)
            android.util.Log.d("LocationService", "🛑 Location updates stopped")
        }
    }
    
    private fun handleLocationUpdate(location: Location) {
        if (!isTracking || currentTripId == null) {
            return
        }
        
        android.util.Log.d("LocationService", "📍 Location update: ${location.latitude}, ${location.longitude}")
        
        val locationPoint = LocationPoint(
            lat = location.latitude,
            lng = location.longitude,
            timestamp = com.google.firebase.Timestamp.now()
        )
        
        // Save location point to repository
        serviceScope.launch {
            try {
                tripsRepository?.addLocationPoint(currentTripId!!, locationPoint)
                android.util.Log.d("LocationService", "✅ Location point saved")
            } catch (e: Exception) {
                android.util.Log.e("LocationService", "❌ Error saving location point: ${e.message}")
            }
        }
    }
    
    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Location Tracking",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Tracks driver location during trips"
                setShowBadge(false)
            }
            
            val notificationManager = getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            notificationManager.createNotificationChannel(channel)
        }
    }
    
    private fun createNotification(): Notification {
        val intent = Intent(this, com.pave.driversapp.presentation.MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }
        
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Location Tracking Active")
            .setContentText("Tracking your location for trip")
            .setSmallIcon(android.R.drawable.ic_menu_mylocation)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .build()
    }
    
    companion object {
        private const val CHANNEL_ID = "location_tracking_channel"
        private const val NOTIFICATION_ID = 1001
    }
}
