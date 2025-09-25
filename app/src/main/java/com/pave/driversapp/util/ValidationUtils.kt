package com.pave.driversapp.util

import android.util.Log

object ValidationUtils {
    
    /**
     * Validates if a string is not blank
     */
    fun isNotBlank(value: String?, fieldName: String): Boolean {
        return if (value.isNullOrBlank()) {
            Log.w("ValidationUtils", "❌ $fieldName is blank or null")
            false
        } else {
            true
        }
    }
    
    /**
     * Validates if a number is positive
     */
    fun isPositive(value: Int?, fieldName: String): Boolean {
        return if (value == null || value < 0) {
            Log.w("ValidationUtils", "❌ $fieldName is not positive: $value")
            false
        } else {
            true
        }
    }
    
    /**
     * Validates if a number is within range
     */
    fun isInRange(value: Int?, min: Int, max: Int, fieldName: String): Boolean {
        return if (value == null || value < min || value > max) {
            Log.w("ValidationUtils", "❌ $fieldName is out of range [$min-$max]: $value")
            false
        } else {
            true
        }
    }
    
    /**
     * Validates location coordinates
     */
    fun isValidLocation(lat: Double?, lng: Double?): Boolean {
        return if (lat == null || lng == null || 
                   lat < -90 || lat > 90 || 
                   lng < -180 || lng > 180) {
            Log.w("ValidationUtils", "❌ Invalid location coordinates: lat=$lat, lng=$lng")
            false
        } else {
            true
        }
    }
    
    /**
     * Validates trip data
     */
    fun validateTripData(
        orderId: String?,
        driverId: String?,
        orgId: String?,
        vehicleNumber: String?
    ): Boolean {
        return isNotBlank(orderId, "Order ID") &&
               isNotBlank(driverId, "Driver ID") &&
               isNotBlank(orgId, "Organization ID") &&
               isNotBlank(vehicleNumber, "Vehicle Number")
    }
    
    /**
     * Validates meter reading
     */
    fun validateMeterReading(reading: Int?, previousReading: Int? = null): Boolean {
        if (!isPositive(reading, "Meter Reading")) {
            return false
        }
        
        if (previousReading != null && reading!! < previousReading) {
            Log.w("ValidationUtils", "❌ Meter reading cannot be less than previous reading: $reading < $previousReading")
            return false
        }
        
        return true
    }
    
    /**
     * Validates image URI
     */
    fun isValidImageUri(uri: String?): Boolean {
        return if (uri.isNullOrBlank()) {
            Log.w("ValidationUtils", "❌ Image URI is blank")
            false
        } else if (!uri.startsWith("content://") && !uri.startsWith("file://")) {
            Log.w("ValidationUtils", "❌ Invalid image URI format: $uri")
            false
        } else {
            true
        }
    }
    
    /**
     * Sanitizes string input
     */
    fun sanitizeString(input: String?): String {
        return input?.trim() ?: ""
    }
    
    /**
     * Validates date string format (yyyy-MM-dd)
     */
    fun isValidDateFormat(date: String?): Boolean {
        if (date.isNullOrBlank()) {
            Log.w("ValidationUtils", "❌ Date is blank")
            return false
        }
        
        val regex = Regex("\\d{4}-\\d{2}-\\d{2}")
        return if (regex.matches(date)) {
            true
        } else {
            Log.w("ValidationUtils", "❌ Invalid date format: $date (expected yyyy-MM-dd)")
            false
        }
    }
    
    /**
     * Validates time string format (HH:mm)
     */
    fun isValidTimeFormat(time: String?): Boolean {
        if (time.isNullOrBlank()) {
            Log.w("ValidationUtils", "❌ Time is blank")
            return false
        }
        
        val regex = Regex("\\d{2}:\\d{2}")
        return if (regex.matches(time)) {
            true
        } else {
            Log.w("ValidationUtils", "❌ Invalid time format: $time (expected HH:mm)")
            false
        }
    }
}
