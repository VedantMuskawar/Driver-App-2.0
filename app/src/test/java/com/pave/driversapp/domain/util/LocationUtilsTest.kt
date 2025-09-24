package com.pave.driversapp.domain.util

import com.pave.driversapp.domain.model.DepotLocation
import org.junit.Test
import org.junit.Assert.*

class LocationUtilsTest {
    
    @Test
    fun `calculateDistance should return correct distance between two points`() {
        // Test case: Distance between San Francisco and Los Angeles
        val sf = DepotLocation(37.7749, -122.4194)
        val la = DepotLocation(34.0522, -118.2437)
        
        val distance = LocationUtils.calculateDistance(sf, la)
        
        // Expected distance is approximately 559 km (559,000 meters)
        // Allow for some margin of error (±10km)
        assertTrue("Distance should be approximately 559km", distance in 549000.0..569000.0)
    }
    
    @Test
    fun `calculateDistance should return zero for same location`() {
        val location = DepotLocation(37.7749, -122.4194)
        
        val distance = LocationUtils.calculateDistance(location, location)
        
        assertEquals("Distance should be zero for same location", 0.0, distance, 0.1)
    }
    
    @Test
    fun `isInsideDepot should return true when location is inside radius`() {
        val depotLocation = DepotLocation(37.7749, -122.4194)
        val currentLocation = DepotLocation(37.7750, -122.4195) // Very close to depot
        val radius = 1000 // 1km radius
        
        val isInside = LocationUtils.isInsideDepot(currentLocation, depotLocation, radius)
        
        assertTrue("Location should be inside depot radius", isInside)
    }
    
    @Test
    fun `isInsideDepot should return false when location is outside radius`() {
        val depotLocation = DepotLocation(37.7749, -122.4194)
        val currentLocation = DepotLocation(37.7849, -122.4294) // Far from depot
        val radius = 100 // 100m radius
        
        val isInside = LocationUtils.isInsideDepot(currentLocation, depotLocation, radius)
        
        assertFalse("Location should be outside depot radius", isInside)
    }
    
    @Test
    fun `isInsideDepot should return true when location is exactly at radius boundary`() {
        val depotLocation = DepotLocation(37.7749, -122.4194)
        val currentLocation = DepotLocation(37.7750, -122.4194) // Approximately 100m away
        val radius = 100 // 100m radius
        
        val isInside = LocationUtils.isInsideDepot(currentLocation, depotLocation, radius)
        
        assertTrue("Location should be inside depot radius at boundary", isInside)
    }
    
    @Test
    fun `formatDistance should format meters correctly`() {
        val distance500m = LocationUtils.formatDistance(500.0)
        assertEquals("500m", distance500m)
        
        val distance1500m = LocationUtils.formatDistance(1500.0)
        assertEquals("1.5km", distance1500m)
        
        val distance2500m = LocationUtils.formatDistance(2500.0)
        assertEquals("2.5km", distance2500m)
    }
    
    @Test
    fun `formatDistance should handle edge cases`() {
        val distance0m = LocationUtils.formatDistance(0.0)
        assertEquals("0m", distance0m)
        
        val distance999m = LocationUtils.formatDistance(999.0)
        assertEquals("999m", distance999m)
        
        val distance1000m = LocationUtils.formatDistance(1000.0)
        assertEquals("1.0km", distance1000m)
    }
}
