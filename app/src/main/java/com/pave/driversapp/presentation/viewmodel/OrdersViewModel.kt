package com.pave.driversapp.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pave.driversapp.domain.model.Order
import com.pave.driversapp.domain.model.ScheduledOrder
import com.pave.driversapp.domain.model.OrdersUiState
import com.pave.driversapp.domain.model.UserRole
import com.pave.driversapp.domain.repository.OrdersRepository
import com.pave.driversapp.domain.repository.MembershipRepository
import com.pave.driversapp.util.MembershipTestData
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.*

class OrdersViewModel(
    private val ordersRepository: OrdersRepository,
    private val membershipRepository: MembershipRepository
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(OrdersUiState())
    val uiState: StateFlow<OrdersUiState> = _uiState.asStateFlow()
    
    private var currentOrgId: String = ""
    private var currentUserId: String = ""
    private var currentUserRole: UserRole? = null
    private var driverVehicleId: String? = null
    
    fun initialize(orgId: String, userId: String) {
        android.util.Log.d("OrdersViewModel", "🚀 initialize() called with orgId: $orgId, userId: $userId")
        currentOrgId = orgId
        currentUserId = userId
        
        viewModelScope.launch {
            try {
                // Check if membership data exists, create if not
                val membershipExists = MembershipTestData.checkMembershipData(userId, orgId)
                if (!membershipExists) {
                    android.util.Log.d("OrdersViewModel", "🔧 Creating sample membership data for testing...")
                    MembershipTestData.createSampleMembershipData()
                }
                
                // Get user role and permissions
                currentUserRole = membershipRepository.getUserRole(userId, orgId)
                driverVehicleId = membershipRepository.getDriverVehicleId(userId, orgId)
                
                android.util.Log.d("OrdersViewModel", "👤 User role: ${currentUserRole?.displayName}")
                android.util.Log.d("OrdersViewModel", "🚗 Driver vehicle ID: $driverVehicleId")
                
                // Set initial date to today
                val today = getCurrentDateString()
                android.util.Log.d("OrdersViewModel", "📅 Setting initial date to: $today")
                _uiState.value = _uiState.value.copy(selectedDate = today)
                
                android.util.Log.d("OrdersViewModel", "📋 Calling loadOrders($today)")
                loadOrders(today)
                android.util.Log.d("OrdersViewModel", "🚗 Calling loadAvailableVehicles()")
                loadAvailableVehicles()
            } catch (e: Exception) {
                android.util.Log.e("OrdersViewModel", "❌ Error initializing: ${e.message}")
                _uiState.value = _uiState.value.copy(errorMessage = "Failed to initialize: ${e.message}")
            }
        }
    }
    
    fun selectDate(date: String) {
        if (isDateAccessible(date)) {
            _uiState.value = _uiState.value.copy(selectedDate = date)
            loadOrders(date)
            loadAvailableVehicles() // Reload vehicles for the new date
        }
    }
    
    fun selectVehicle(vehicleNumber: String?) {
        _uiState.value = _uiState.value.copy(selectedVehicle = vehicleNumber)
        loadOrders(_uiState.value.selectedDate, vehicleNumber)
    }
    
    private fun loadOrders(date: String, vehicleNumber: String? = null) {
        android.util.Log.d("OrdersViewModel", "📋 loadOrders() called with date: $date, vehicle: $vehicleNumber, orgId: $currentOrgId")
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
            android.util.Log.d("OrdersViewModel", "⏳ Set loading = true")
            
            try {
                android.util.Log.d("OrdersViewModel", "📡 Starting to collect orders from repository...")
                ordersRepository.getOrders(
                    orgId = currentOrgId,
                    date = date,
                    vehicleNumber = vehicleNumber,
                    userRole = currentUserRole,
                    driverVehicleId = driverVehicleId
                ).collect { orders ->
                    android.util.Log.d("OrdersViewModel", "📊 Received ${orders.size} orders from repository")
                    orders.forEachIndexed { index, order ->
                        android.util.Log.d("OrdersViewModel", "   Order $index: ${order.orderId} - ${order.clientName} - ${order.address}")
                    }
                    _uiState.value = _uiState.value.copy(
                        orders = orders,
                        isLoading = false
                    )
                    android.util.Log.d("OrdersViewModel", "✅ Updated UI state with ${orders.size} orders, loading = false")
                }
            } catch (e: Exception) {
                android.util.Log.e("OrdersViewModel", "💥 Error in loadOrders: ${e.message}")
                android.util.Log.e("OrdersViewModel", "💥 Exception type: ${e.javaClass.simpleName}")
                android.util.Log.e("OrdersViewModel", "💥 Stack trace: ${e.stackTrace.joinToString("\n")}")
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = e.message ?: "Failed to load orders"
                )
            }
        }
    }
    
    private fun loadAvailableVehicles() {
        android.util.Log.d("OrdersViewModel", "🚗 loadAvailableVehicles() called for orgId: $currentOrgId, date: ${_uiState.value.selectedDate}")
        viewModelScope.launch {
            try {
                android.util.Log.d("OrdersViewModel", "📡 Calling ordersRepository.getAvailableVehicles($currentOrgId, ${_uiState.value.selectedDate})")
                val vehicles = ordersRepository.getAvailableVehicles(currentOrgId, _uiState.value.selectedDate)
                android.util.Log.d("OrdersViewModel", "🚗 Received ${vehicles.size} vehicles from repository")
                vehicles.forEachIndexed { index, vehicle ->
                    android.util.Log.d("OrdersViewModel", "   Vehicle $index: $vehicle")
                }
                _uiState.value = _uiState.value.copy(availableVehicles = vehicles)
                android.util.Log.d("OrdersViewModel", "✅ Updated UI state with ${vehicles.size} vehicles")
            } catch (e: Exception) {
                android.util.Log.e("OrdersViewModel", "💥 Error in loadAvailableVehicles: ${e.message}")
                android.util.Log.e("OrdersViewModel", "💥 Exception type: ${e.javaClass.simpleName}")
                android.util.Log.e("OrdersViewModel", "💥 Stack trace: ${e.stackTrace.joinToString("\n")}")
                // Handle error silently for vehicles
            }
        }
    }
    
    fun refreshOrders() {
        loadOrders(_uiState.value.selectedDate, _uiState.value.selectedVehicle)
        loadAvailableVehicles() // Reload vehicles for the current date
    }
    
    // SCH_ORDERS methods
    fun loadScheduledOrders(date: String, vehicleNumber: String? = null) {
        android.util.Log.d("OrdersViewModel", "🔄 loadScheduledOrders called with date: $date, vehicle: $vehicleNumber, orgId: $currentOrgId")
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
            
            try {
                android.util.Log.d("OrdersViewModel", "📡 Starting to collect SCH_ORDERS from repository...")
                ordersRepository.getScheduledOrders(currentOrgId, date, vehicleNumber).collect { scheduledOrders ->
                    android.util.Log.d("OrdersViewModel", "📊 Received ${scheduledOrders.size} scheduled orders from repository")
                    scheduledOrders.forEach { order ->
                        android.util.Log.d("OrdersViewModel", "📋 ViewModel received order: ${order.orderId} - ${order.clientName}")
                    }
                    _uiState.value = _uiState.value.copy(
                        scheduledOrders = scheduledOrders,
                        isLoading = false
                    )
                }
            } catch (e: Exception) {
                android.util.Log.e("OrdersViewModel", "💥 Error in loadScheduledOrders: ${e.message}")
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = e.message ?: "Failed to load scheduled orders"
                )
            }
        }
    }
    
    fun refreshScheduledOrders() {
        loadScheduledOrders(_uiState.value.selectedDate, _uiState.value.selectedVehicle)
    }
    
    fun getScheduledOrderById(orderId: String) {
        viewModelScope.launch {
            try {
                val scheduledOrder = ordersRepository.getScheduledOrderById(orderId)
                _uiState.value = _uiState.value.copy(selectedScheduledOrder = scheduledOrder)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    errorMessage = e.message ?: "Failed to load scheduled order details"
                )
            }
        }
    }
    
    // Role-based access control
    fun isDateAccessible(date: String): Boolean {
        return when (currentUserRole) {
            UserRole.ADMIN -> true // Admin can access any date
            UserRole.MANAGER, UserRole.DRIVER -> { // Manager and Driver can only access today and tomorrow
                val today = getCurrentDateString()
                val tomorrow = getTomorrowDateString()
                date == today || date == tomorrow
            }
            null -> false
        }
    }
    
    fun canUseVehicleFilter(): Boolean {
        return currentUserRole == UserRole.ADMIN || currentUserRole == UserRole.MANAGER
    }
    
    fun canUseCalendar(): Boolean {
        return currentUserRole == UserRole.ADMIN
    }
    
    fun getAccessibleDates(): List<String> {
        val today = getCurrentDateString()
        val tomorrow = getTomorrowDateString()
        
        return when (currentUserRole) {
            UserRole.ADMIN -> {
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
            UserRole.MANAGER, UserRole.DRIVER -> listOf(today, tomorrow)
            null -> emptyList()
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
