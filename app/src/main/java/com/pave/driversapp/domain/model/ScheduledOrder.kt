package com.pave.driversapp.domain.model

import com.google.firebase.Timestamp
import kotlinx.serialization.Contextual
import kotlinx.serialization.Serializable

@Serializable
data class ScheduledOrder(
    val orderId: String = "",
    val address: String = "",
    val clientID: String = "",
    val clientName: String = "",
    val clientPhoneNumber: String = "",
    val defOrderID: String = "",
    @Contextual val deliveredTime: Timestamp? = null,
    @Contextual val deliveryDate: Timestamp? = null,
    val deliveryStatus: Boolean = false,
    @Contextual val dispatchEnd: Timestamp? = null,
    @Contextual val dispatchStart: Timestamp? = null,
    val dispatchStatus: Boolean = false,
    @Contextual val dispatchedTime: Timestamp? = null,
    val driverMID: String = "",
    val driverName: String = "",
    val driverPhoneNumber: String = "",
    val orgID: String = "",
    val orgName: String = "",
    val paySchedule: String = "",
    val paymentStatus: Boolean = false,
    val productName: String = "",
    val productQuant: Int = 0,
    val productUnitPrice: Double = 0.0,
    val regionID: String = "",
    val regionName: String = "",
    val toAccount: String = "",
    val userID: String = "",
    val vehicleID: String = "",
    val vehicleNumber: String = "",
    // Additional fields that exist in Firestore but were missing
    val dmNumber: Long = 0L, // Changed from String to Long
    @Contextual val dmGeneratedAt: Timestamp? = null,
    val dmDocumentID: String = "",
    @Contextual val updatedAt: Timestamp? = null,
    val vehicleIdent: Long = 0L,
    val deliveryImgRef: String = "",
    val deliveryImg: String = ""
) {
    // Computed properties for UI
    val totalAmount: Double
        get() = productQuant * productUnitPrice
    
    val isDispatched: Boolean
        get() = dispatchStatus && dispatchedTime != null
    
    val isDelivered: Boolean
        get() = deliveryStatus && deliveredTime != null
    
    val dispatchTimeRange: String
        get() = if (dispatchStart != null && dispatchEnd != null) {
            "${dispatchStart.toDate().toString().substring(11, 16)} - ${dispatchEnd.toDate().toString().substring(11, 16)}"
        } else "Not scheduled"
    
    val deliveryDateFormatted: String
        get() = deliveryDate?.toDate()?.toString()?.substring(0, 10) ?: "Not scheduled"
    
    val status: OrderStatus
        get() = when {
            isDelivered -> OrderStatus.DELIVERED
            isDispatched -> OrderStatus.DISPATCHED
            dispatchStatus -> OrderStatus.READY_FOR_DISPATCH
            else -> OrderStatus.PENDING
        }
}

enum class OrderStatus {
    PENDING,
    READY_FOR_DISPATCH,
    DISPATCHED,
    DELIVERED,
    CANCELLED
}
