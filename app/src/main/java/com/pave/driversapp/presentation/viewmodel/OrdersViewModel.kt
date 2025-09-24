package com.pave.driversapp.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pave.driversapp.domain.model.Order
import com.pave.driversapp.domain.model.OrdersUiState
import com.pave.driversapp.domain.repository.OrdersRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class OrdersViewModel(
    private val ordersRepository: OrdersRepository
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(OrdersUiState())
    val uiState: StateFlow<OrdersUiState> = _uiState.asStateFlow()
    
    private var currentOrgId: String = ""
    private var currentUserRole: Int = 0
    
    fun initialize(orgId: String, userRole: Int) {
        currentOrgId = orgId
        currentUserRole = userRole
        
        // Set initial date to today
        val today = getCurrentDateString()
        _uiState.value = _uiState.value.copy(selectedDate = today)
        
        loadOrders(today)
        loadAvailableVehicles()
    }
    
    fun selectDate(date: String) {
        if (isDateAccessible(date)) {
            _uiState.value = _uiState.value.copy(selectedDate = date)
            loadOrders(date)
        }
    }
    
    fun selectVehicle(vehicleNumber: String?) {
        _uiState.value = _uiState.value.copy(selectedVehicle = vehicleNumber)
        loadOrders(_uiState.value.selectedDate, vehicleNumber)
    }
    
    private fun loadOrders(date: String, vehicleNumber: String? = null) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
            
            try {
                ordersRepository.getOrders(currentOrgId, date, vehicleNumber).collect { orders ->
                    _uiState.value = _uiState.value.copy(
                        orders = orders,
                        isLoading = false
                    )
                }
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = e.message ?: "Failed to load orders"
                )
            }
        }
    }
    
    private fun loadAvailableVehicles() {
        viewModelScope.launch {
            try {
                val vehicles = ordersRepository.getAvailableVehicles(currentOrgId)
                _uiState.value = _uiState.value.copy(availableVehicles = vehicles)
            } catch (e: Exception) {
                // Handle error silently for vehicles
            }
        }
    }
    
    fun refreshOrders() {
        loadOrders(_uiState.value.selectedDate, _uiState.value.selectedVehicle)
    }
    
    // Role-based access control
    fun isDateAccessible(date: String): Boolean {
        return when (currentUserRole) {
            0 -> true // Admin can access any date
            1, 2 -> { // Manager and Driver can only access today and tomorrow
                val today = getCurrentDateString()
                val tomorrow = getTomorrowDateString()
                date == today || date == tomorrow
            }
            else -> false
        }
    }
    
    fun canUseVehicleFilter(): Boolean {
        return currentUserRole == 0 || currentUserRole == 1
    }
    
    fun canUseCalendar(): Boolean {
        return currentUserRole == 0
    }
    
    fun getAccessibleDates(): List<String> {
        val today = getCurrentDateString()
        val tomorrow = getTomorrowDateString()
        
        return when (currentUserRole) {
            0 -> {
                // Admin can see 4 previous days + today + tomorrow
                val dates = mutableListOf<String>()
                val calendar = Calendar.getInstance()
                
                // Add 4 previous days
                repeat(4) {
                    calendar.add(Calendar.DAY_OF_MONTH, -1)
                    dates.add(0, getDateString(calendar.time))
                }
                
                // Add today and tomorrow
                calendar.time = Date()
                dates.add(getDateString(calendar.time))
                calendar.add(Calendar.DAY_OF_MONTH, 1)
                dates.add(getDateString(calendar.time))
                
                dates
            }
            1, 2 -> listOf(today, tomorrow)
            else -> emptyList()
        }
    }
    
    private fun getCurrentDateString(): String {
        return getDateString(Date())
    }
    
    private fun getTomorrowDateString(): String {
        val calendar = Calendar.getInstance()
        calendar.add(Calendar.DAY_OF_MONTH, 1)
        return getDateString(calendar.time)
    }
    
    private fun getDateString(date: Date): String {
        val formatter = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
        return formatter.format(date)
    }
    
    fun getFormattedDate(dateString: String): String {
        return try {
            val inputFormat = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
            val outputFormat = SimpleDateFormat("MMM dd", Locale.getDefault())
            val date = inputFormat.parse(dateString)
            outputFormat.format(date ?: Date())
        } catch (e: Exception) {
            dateString
        }
    }
    
    fun isToday(dateString: String): Boolean {
        return dateString == getCurrentDateString()
    }
    
    fun isTomorrow(dateString: String): Boolean {
        return dateString == getTomorrowDateString()
    }
}
