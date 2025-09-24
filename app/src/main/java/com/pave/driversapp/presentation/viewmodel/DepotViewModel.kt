package com.pave.driversapp.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pave.driversapp.domain.model.DepotLocation
import com.pave.driversapp.domain.model.DepotSettings
import com.pave.driversapp.domain.repository.DepotRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class DepotViewModel(
    private val depotRepository: DepotRepository
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(DepotUiState())
    val uiState: StateFlow<DepotUiState> = _uiState.asStateFlow()
    
    fun loadDepotSettings(orgId: String) {
        viewModelScope.launch {
            android.util.Log.d("DepotViewModel", "🔄 Loading depot settings for org: $orgId")
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
            
            // Try to get cached depot first
            val cachedDepot = depotRepository.getCachedDepot(orgId)
            if (cachedDepot != null) {
                android.util.Log.d("DepotViewModel", "📱 Using cached depot settings")
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    depotSettings = cachedDepot,
                    hasDepotConfigured = true
                )
            }
            
            // Then try to fetch from Firestore
            val result = depotRepository.getDepot(orgId)
            if (result.isSuccess) {
                val depotSettings = result.getOrNull()
                android.util.Log.d("DepotViewModel", "📊 Depot settings result: ${depotSettings != null}")
                
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    depotSettings = depotSettings,
                    hasDepotConfigured = depotSettings != null
                )
            } else {
                android.util.Log.e("DepotViewModel", "❌ Failed to load depot settings: ${result.exceptionOrNull()?.message}")
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = result.exceptionOrNull()?.message ?: "Failed to load depot settings"
                )
            }
        }
    }
    
    fun setDepotLocation(orgId: String, depotLocation: DepotLocation, radius: Int, createdBy: String) {
        viewModelScope.launch {
            android.util.Log.d("DepotViewModel", "💾 Setting depot location: ${depotLocation.lat}, ${depotLocation.lng}, radius: $radius")
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
            
            val result = depotRepository.setDepot(orgId, depotLocation, radius, createdBy)
            if (result.isSuccess) {
                val depotSettings = DepotSettings(
                    depotLocation = depotLocation,
                    radius = radius,
                    createdBy = createdBy
                )
                
                android.util.Log.d("DepotViewModel", "✅ Depot location saved successfully")
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    depotSettings = depotSettings,
                    hasDepotConfigured = true,
                    depotSaved = true
                )
            } else {
                android.util.Log.e("DepotViewModel", "❌ Failed to save depot location: ${result.exceptionOrNull()?.message}")
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = result.exceptionOrNull()?.message ?: "Failed to save depot location"
                )
            }
        }
    }
    
    fun deleteDepot(orgId: String) {
        viewModelScope.launch {
            android.util.Log.d("DepotViewModel", "🗑️ Deleting depot for org: $orgId")
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
            
            val result = depotRepository.deleteDepot(orgId)
            if (result.isSuccess) {
                android.util.Log.d("DepotViewModel", "✅ Depot deleted successfully")
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    depotSettings = null,
                    hasDepotConfigured = false,
                    depotSaved = true // Use this to show success message
                )
            } else {
                android.util.Log.e("DepotViewModel", "❌ Failed to delete depot: ${result.exceptionOrNull()?.message}")
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = result.exceptionOrNull()?.message ?: "Failed to delete depot"
                )
            }
        }
    }
    
    suspend fun checkLocationInsideDepot(currentLocation: DepotLocation): Boolean {
        val depotSettings = _uiState.value.depotSettings
        return if (depotSettings != null) {
            val isInside = depotRepository.isInsideDepot(currentLocation, depotSettings)
            android.util.Log.d("DepotViewModel", "📍 Location check result: $isInside")
            isInside
        } else {
            android.util.Log.w("DepotViewModel", "⚠️ No depot settings available for location check")
            false
        }
    }
    
    fun clearError() {
        _uiState.value = _uiState.value.copy(errorMessage = null)
    }
    
    fun resetDepotSaved() {
        _uiState.value = _uiState.value.copy(depotSaved = false)
    }
}

data class DepotUiState(
    val isLoading: Boolean = false,
    val depotSettings: DepotSettings? = null,
    val hasDepotConfigured: Boolean = false,
    val depotSaved: Boolean = false,
    val errorMessage: String? = null
)
