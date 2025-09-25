package com.pave.driversapp.util

import android.content.Context
import android.util.Log
import com.pave.driversapp.domain.model.Trip
import com.pave.driversapp.domain.model.LocationPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.text.SimpleDateFormat
import java.util.*

/**
 * Local debugging utility to store trip data for offline analysis
 * This helps debug issues when testing on a vehicle without phone connection
 */
class TripDebugLogger(private val context: Context) {
    
    companion object {
        private const val DEBUG_DIR = "trip_debug"
        private const val TRIP_LOG_FILE = "trip_log.txt"
        private const val LOCATION_LOG_FILE = "location_log.txt"
    }
    
    private val debugDir = File(context.filesDir, DEBUG_DIR)
    private val tripLogFile = File(debugDir, TRIP_LOG_FILE)
    private val locationLogFile = File(debugDir, LOCATION_LOG_FILE)
    
    init {
        if (!debugDir.exists()) {
            debugDir.mkdirs()
        }
    }
    
    /**
     * Log trip events for debugging
     */
    suspend fun logTripEvent(event: String, trip: Trip? = null, additionalData: Map<String, Any> = emptyMap()) {
        withContext(Dispatchers.IO) {
            try {
                val timestamp = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.getDefault()).format(Date())
                val logEntry = buildString {
                    appendLine("[$timestamp] $event")
                    trip?.let {
                        appendLine("  Trip ID: ${it.tripId}")
                        appendLine("  Order ID: ${it.orderId}")
                        appendLine("  Driver ID: ${it.driverId}")
                        appendLine("  Status: ${it.status}")
                        appendLine("  Vehicle: ${it.vehicleNumber}")
                        appendLine("  Initial Meter: ${it.initialMeterReading}")
                        appendLine("  Last Meter: ${it.lastMeterReading}")
                    }
                    additionalData.forEach { (key, value) ->
                        appendLine("  $key: $value")
                    }
                    appendLine("---")
                }
                
                tripLogFile.appendText(logEntry)
                Log.d("TripDebugLogger", "📝 Logged trip event: $event")
            } catch (e: Exception) {
                Log.e("TripDebugLogger", "❌ Error logging trip event: ${e.message}")
            }
        }
    }
    
    /**
     * Log location updates for debugging
     */
    suspend fun logLocationUpdate(locationPoint: LocationPoint, tripId: String) {
        withContext(Dispatchers.IO) {
            try {
                val timestamp = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.getDefault()).format(Date())
                val logEntry = "$timestamp,$tripId,${locationPoint.lat},${locationPoint.lng},${locationPoint.timestamp?.seconds}\n"
                
                locationLogFile.appendText(logEntry)
                Log.d("TripDebugLogger", "📍 Logged location: ${locationPoint.lat}, ${locationPoint.lng}")
            } catch (e: Exception) {
                Log.e("TripDebugLogger", "❌ Error logging location: ${e.message}")
            }
        }
    }
    
    /**
     * Log service events
     */
    suspend fun logServiceEvent(event: String, additionalData: Map<String, Any> = emptyMap()) {
        withContext(Dispatchers.IO) {
            try {
                val timestamp = SimpleDateFormat("yyyy-MM-dd HH:mm:ss.SSS", Locale.getDefault()).format(Date())
                val logEntry = buildString {
                    appendLine("[$timestamp] SERVICE: $event")
                    additionalData.forEach { (key, value) ->
                        appendLine("  $key: $value")
                    }
                    appendLine("---")
                }
                
                tripLogFile.appendText(logEntry)
                Log.d("TripDebugLogger", "🔧 Logged service event: $event")
            } catch (e: Exception) {
                Log.e("TripDebugLogger", "❌ Error logging service event: ${e.message}")
            }
        }
    }
    
    /**
     * Get debug files for analysis
     */
    fun getDebugFiles(): List<File> {
        return listOf(tripLogFile, locationLogFile).filter { it.exists() }
    }
    
    /**
     * Clear debug logs
     */
    suspend fun clearLogs() {
        withContext(Dispatchers.IO) {
            try {
                tripLogFile.delete()
                locationLogFile.delete()
                Log.d("TripDebugLogger", "🗑️ Cleared debug logs")
            } catch (e: Exception) {
                Log.e("TripDebugLogger", "❌ Error clearing logs: ${e.message}")
            }
        }
    }
    
    /**
     * Get trip log content for analysis
     */
    suspend fun getTripLogContent(): String {
        return withContext(Dispatchers.IO) {
            try {
                if (tripLogFile.exists()) {
                    tripLogFile.readText()
                } else {
                    "No trip log found"
                }
            } catch (e: Exception) {
                "Error reading trip log: ${e.message}"
            }
        }
    }
    
    /**
     * Get location log content for analysis
     */
    suspend fun getLocationLogContent(): String {
        return withContext(Dispatchers.IO) {
            try {
                if (locationLogFile.exists()) {
                    locationLogFile.readText()
                } else {
                    "No location log found"
                }
            } catch (e: Exception) {
                "Error reading location log: ${e.message}"
            }
        }
    }
}
