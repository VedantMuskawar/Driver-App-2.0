package com.pave.driversapp.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pave.driversapp.domain.model.Order
import com.pave.driversapp.domain.model.Trip
import com.pave.driversapp.domain.model.TripStatus
import com.pave.driversapp.domain.model.LocationPoint
import com.pave.driversapp.domain.repository.TripsRepository
import com.pave.driversapp.domain.repository.DepotRepository
import com.pave.driversapp.domain.util.LocationUtils
import com.pave.driversapp.util.LocationManager
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import android.location.Location
import com.google.android.gms.location.FusedLocationProviderClient
import kotlinx.coroutines.delay
import android.content.Context

data class TripUiState(
    val isLoading: Boolean = false,
    val currentTrip: Trip? = null,
    val order: Order? = null,
    val tripStatus: TripStatus? = null,
    val isInsideDepot: Boolean = false,
    val currentLocation: Location? = null,
    val locationPoints: List<LocationPoint> = emptyList(),
    val elapsedTime: Long = 0, // in minutes
    val error: String? = null,
    val showMeterReadingDialog: Boolean = false,
    val showDeliveryPhotoDialog: Boolean = false,
    val showCancelDialog: Boolean = false,
    val showSuccessDialog: Boolean = false,
    val successMessage: String = "",
    val meterReading: String = "",
    val deliveryImageUri: String? = null,
    val isTrackingLocation: Boolean = false,
    val isOffline: Boolean = false
)

class TripsViewModel(
    private val tripsRepository: TripsRepository,
    private val depotRepository: DepotRepository,
    private val fusedLocationClient: FusedLocationProviderClient,
    private val context: Context
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(TripUiState())
    val uiState: StateFlow<TripUiState> = _uiState.asStateFlow()
    
    private var locationTrackingJob: kotlinx.coroutines.Job? = null
    private var elapsedTimeJob: kotlinx.coroutines.Job? = null
    private val locationManager = LocationManager(context)
    
    fun initialize(order: Order, driverId: String, orgId: String) {
        _uiState.value = _uiState.value.copy(
            order = order,
            isLoading = true
        )
        
        viewModelScope.launch {
            try {
                // Check for active trip
                val activeTrip = tripsRepository.getActiveTrip(driverId)
                
                // Check depot location
                val depotResult = depotRepository.getDepot(orgId)
                val depotSettings = depotResult.getOrNull()
                val isInsideDepot = if (depotSettings != null && _uiState.value.currentLocation != null) {
                    LocationUtils.isInsideDepot(
                        com.pave.driversapp.domain.model.DepotLocation(
                            _uiState.value.currentLocation!!.latitude,
                            _uiState.value.currentLocation!!.longitude
                        ),
                        depotSettings.depotLocation,
                        depotSettings.radius
                    )
                } else false
                
                _uiState.value = _uiState.value.copy(
                    currentTrip = activeTrip,
                    tripStatus = activeTrip?.status,
                    isInsideDepot = isInsideDepot,
                    isLoading = false
                )
                
                // Start location updates
                startLocationUpdates()
                
                // Start elapsed time tracking if trip is active
                if (activeTrip != null) {
                    startElapsedTimeTracking(activeTrip)
                }
                
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    error = e.message,
                    isLoading = false
                )
            }
        }
    }
    
    fun dispatchTrip(initialMeterReading: Int) {
        val order = _uiState.value.order ?: return
        val driverId = "current_driver_id" // Get from auth state
        val orgId = order.orgId
        
        viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(isLoading = true)
                
                val trip = Trip(
                    tripId = "", // Will be set by repository
                    orderId = order.orderId,
                    driverId = driverId,
                    orgId = orgId,
                    vehicleNumber = order.vehicleNumber,
                    status = TripStatus.DISPATCHED,
                    initialMeterReading = initialMeterReading,
                    dispatchedAt = com.google.firebase.Timestamp.now(),
                    createdAt = com.google.firebase.Timestamp.now(),
                    updatedAt = com.google.firebase.Timestamp.now()
                )
                
                val result = tripsRepository.createTrip(trip)
                result.fold(
                    onSuccess = { tripId ->
                        val createdTrip = trip.copy(tripId = tripId)
                        _uiState.value = _uiState.value.copy(
                            currentTrip = createdTrip,
                            tripStatus = TripStatus.DISPATCHED,
                            isLoading = false,
                            showMeterReadingDialog = false,
                            meterReading = "",
                            isTrackingLocation = true
                        )
                        
                        // Show success message
                        showSuccessDialog("Trip dispatched successfully! Location tracking started.")
                        
                        // Start location tracking service
                        startLocationTrackingService(tripId)
                        startElapsedTimeTracking(createdTrip)
                    },
                    onFailure = { error ->
                        _uiState.value = _uiState.value.copy(
                            error = error.message,
                            isLoading = false
                        )
                    }
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    error = e.message,
                    isLoading = false
                )
            }
        }
    }
    
    fun markDelivered(imageUri: String) {
        val trip = _uiState.value.currentTrip ?: return
        
        viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(isLoading = true)
                
                // Upload image
                val imageRefResult = tripsRepository.uploadDeliveryImage(trip.tripId, imageUri)
                imageRefResult.fold(
                    onSuccess = { imageRef ->
                        // Update trip status
                        val result = tripsRepository.updateTripStatus(trip.tripId, TripStatus.DELIVERED)
                        result.fold(
                            onSuccess = {
                                _uiState.value = _uiState.value.copy(
                                    tripStatus = TripStatus.DELIVERED,
                                    deliveryImageUri = imageUri,
                                    isLoading = false,
                                    showDeliveryPhotoDialog = false
                                )
                                
                                // Show success message
                                showSuccessDialog("Delivery confirmed! Photo uploaded successfully.")
                            },
                            onFailure = { error ->
                                _uiState.value = _uiState.value.copy(
                                    error = error.message,
                                    isLoading = false
                                )
                            }
                        )
                    },
                    onFailure = { error ->
                        _uiState.value = _uiState.value.copy(
                            error = error.message,
                            isLoading = false
                        )
                    }
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    error = e.message,
                    isLoading = false
                )
            }
        }
    }
    
    fun returnTrip(finalMeterReading: Int) {
        val trip = _uiState.value.currentTrip ?: return
        
        viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(isLoading = true)
                
                // Stop location tracking service
                stopLocationTrackingService()
                
                // Complete trip
                val result = tripsRepository.completeTrip(trip.tripId, finalMeterReading)
                result.fold(
                    onSuccess = { metrics ->
                        _uiState.value = _uiState.value.copy(
                            tripStatus = TripStatus.RETURNED,
                            isLoading = false,
                            isTrackingLocation = false
                        )
                        
                        // Show success message
                        showSuccessDialog("Trip completed successfully! Location tracking stopped.")
                    },
                    onFailure = { error ->
                        _uiState.value = _uiState.value.copy(
                            error = error.message,
                            isLoading = false
                        )
                    }
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    error = e.message,
                    isLoading = false
                )
            }
        }
    }
    
    fun cancelTrip(cancelledBy: String) {
        val trip = _uiState.value.currentTrip ?: return
        
        viewModelScope.launch {
            try {
                _uiState.value = _uiState.value.copy(isLoading = true)
                
                // Stop location tracking service
                stopLocationTrackingService()
                
                val result = tripsRepository.cancelTrip(trip.tripId, cancelledBy)
                result.fold(
                    onSuccess = {
                        _uiState.value = _uiState.value.copy(
                            tripStatus = TripStatus.CANCELLED,
                            isLoading = false,
                            showCancelDialog = false,
                            isTrackingLocation = false
                        )
                    },
                    onFailure = { error ->
                        _uiState.value = _uiState.value.copy(
                            error = error.message,
                            isLoading = false
                        )
                    }
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    error = e.message,
                    isLoading = false
                )
            }
        }
    }
    
    fun showMeterReadingDialog() {
        _uiState.value = _uiState.value.copy(showMeterReadingDialog = true)
    }
    
    fun hideMeterReadingDialog() {
        _uiState.value = _uiState.value.copy(showMeterReadingDialog = false)
    }
    
    fun showDeliveryPhotoDialog() {
        _uiState.value = _uiState.value.copy(showDeliveryPhotoDialog = true)
    }
    
    fun hideDeliveryPhotoDialog() {
        _uiState.value = _uiState.value.copy(showDeliveryPhotoDialog = false)
    }
    
    fun showCancelDialog() {
        _uiState.value = _uiState.value.copy(showCancelDialog = true)
    }
    
    fun hideCancelDialog() {
        _uiState.value = _uiState.value.copy(showCancelDialog = false)
    }
    
    fun updateMeterReading(reading: String) {
        _uiState.value = _uiState.value.copy(meterReading = reading)
    }
    
    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }
    
    fun showSuccessDialog(message: String) {
        _uiState.value = _uiState.value.copy(
            showSuccessDialog = true,
            successMessage = message
        )
    }
    
    fun hideSuccessDialog() {
        _uiState.value = _uiState.value.copy(showSuccessDialog = false)
    }
    
    private fun startLocationTrackingService(tripId: String) {
        android.util.Log.d("TripsViewModel", "🚀 Starting location tracking service for trip: $tripId")
        locationManager.startLocationTrackingService(tripId)
    }
    
    private fun stopLocationTrackingService() {
        android.util.Log.d("TripsViewModel", "🛑 Stopping location tracking service")
        locationManager.stopLocationTrackingService()
    }
    
    fun startLocationUpdates() {
        viewModelScope.launch {
            try {
                // First try to get last known location
                fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                    if (location != null) {
                        _uiState.value = _uiState.value.copy(currentLocation = location)
                        android.util.Log.d("TripsViewModel", "📍 Got last known location: ${location.latitude}, ${location.longitude}")
                    } else {
                        android.util.Log.d("TripsViewModel", "⚠️ No last known location, requesting fresh location")
                        // If no last known location, request fresh location
                        requestCurrentLocation()
                    }
                }
            } catch (e: Exception) {
                android.util.Log.e("TripsViewModel", "❌ Error getting location: ${e.message}")
                _uiState.value = _uiState.value.copy(error = "Location permission required")
            }
        }
    }
    
    private fun requestCurrentLocation() {
        viewModelScope.launch {
            try {
                // Request fresh location update
                fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                    if (location != null) {
                        _uiState.value = _uiState.value.copy(currentLocation = location)
                        android.util.Log.d("TripsViewModel", "📍 Got fresh location: ${location.latitude}, ${location.longitude}")
                    } else {
                        android.util.Log.w("TripsViewModel", "⚠️ Still no location available")
                    }
                }
            } catch (e: Exception) {
                android.util.Log.e("TripsViewModel", "❌ Error requesting fresh location: ${e.message}")
            }
        }
    }
    
    private fun startLocationTracking(tripId: String) {
        locationTrackingJob = viewModelScope.launch {
            while (_uiState.value.isTrackingLocation) {
                try {
                    fusedLocationClient.lastLocation.addOnSuccessListener { location ->
                        if (location != null) {
                            val locationPoint = LocationPoint(
                                lat = location.latitude,
                                lng = location.longitude
                            )
                            
                            // Update UI state with new location point
                            val currentPoints = _uiState.value.locationPoints.toMutableList()
                            currentPoints.add(locationPoint)
                            _uiState.value = _uiState.value.copy(
                                locationPoints = currentPoints,
                                currentLocation = location
                            )
                            
                            viewModelScope.launch {
                                tripsRepository.addLocationPoint(tripId, locationPoint)
                            }
                        }
                    }
                } catch (e: Exception) {
                    // Handle offline case
                    _uiState.value = _uiState.value.copy(isOffline = true)
                }
                
                delay(10000) // 10 seconds
            }
        }
    }
    
    private fun stopLocationTracking() {
        locationTrackingJob?.cancel()
        locationTrackingJob = null
    }
    
    private fun startElapsedTimeTracking(trip: Trip) {
        elapsedTimeJob = viewModelScope.launch {
            val startTime = trip.dispatchedAt?.seconds ?: System.currentTimeMillis() / 1000
            
            while (_uiState.value.tripStatus != TripStatus.RETURNED && _uiState.value.tripStatus != TripStatus.CANCELLED) {
                val currentTime = System.currentTimeMillis() / 1000
                val elapsed = (currentTime - startTime) / 60 // minutes
                
                _uiState.value = _uiState.value.copy(elapsedTime = elapsed)
                
                delay(60000) // Update every minute
            }
        }
    }
    
    override fun onCleared() {
        super.onCleared()
        locationTrackingJob?.cancel()
        elapsedTimeJob?.cancel()
    }
}
