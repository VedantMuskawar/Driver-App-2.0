package com.pave.driversapp.domain.model

import kotlinx.serialization.Serializable

@Serializable
data class Order(
    val orderId: String,
    val orgId: String,
    val clientName: String,
    val address: String,
    val regionName: String,
    val productName: String,
    val productQuant: Int,
    val productUnitPrice: Double,
    val vehicleNumber: String,
    val dispatchStart: String, // Format: "HH:mm"
    val dispatchEnd: String,   // Format: "HH:mm"
    val dispatchDate: String,  // Format: "yyyy-MM-dd"
    val paymentMethod: String = "Pay on Delivery",
    val dispatchStatus: Boolean = false,
    val deliveryStatus: Boolean = false,
    val returnStatus: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis()
) {
    val totalAmount: Double
        get() = productQuant * productUnitPrice
    
    val timeSlot: String
        get() = "$dispatchStart - $dispatchEnd"
}

@Serializable
data class OrdersFilter(
    val orgId: String,
    val selectedDate: String,
    val vehicleNumber: String? = null
)

@Serializable
data class OrdersUiState(
    val isLoading: Boolean = false,
    val orders: List<Order> = emptyList(),
    val availableVehicles: List<String> = emptyList(),
    val selectedDate: String = "",
    val selectedVehicle: String? = null,
    val isOffline: Boolean = false,
    val errorMessage: String? = null
)
