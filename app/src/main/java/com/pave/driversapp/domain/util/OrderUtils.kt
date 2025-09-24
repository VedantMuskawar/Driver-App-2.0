package com.pave.driversapp.domain.util

import androidx.compose.ui.graphics.Color
import com.pave.driversapp.domain.model.Order

object OrderUtils {
    
    /**
     * Get the background color for an order card based on its status
     */
    fun getOrderCardColor(order: Order): Color {
        return when {
            // Not dispatched and not delivered -> RED
            !order.dispatchStatus && !order.deliveryStatus -> Color.Red.copy(alpha = 0.1f)
            
            // Dispatched but not delivered -> ORANGE  
            order.dispatchStatus && !order.deliveryStatus -> Color(0xFFFF9800).copy(alpha = 0.1f)
            
            // Dispatched and delivered but not returned -> GREEN
            order.dispatchStatus && order.deliveryStatus && !order.returnStatus -> Color.Green.copy(alpha = 0.1f)
            
            // Dispatched, delivered and returned -> BLUE
            order.dispatchStatus && order.deliveryStatus && order.returnStatus -> Color.Blue.copy(alpha = 0.1f)
            
            // Default case
            else -> Color.Gray.copy(alpha = 0.1f)
        }
    }
    
    /**
     * Get the border color for an order card based on its status
     */
    fun getOrderCardBorderColor(order: Order): Color {
        return when {
            !order.dispatchStatus && !order.deliveryStatus -> Color.Red
            order.dispatchStatus && !order.deliveryStatus -> Color(0xFFFF9800)
            order.dispatchStatus && order.deliveryStatus && !order.returnStatus -> Color.Green
            order.dispatchStatus && order.deliveryStatus && order.returnStatus -> Color.Blue
            else -> Color.Gray
        }
    }
    
    /**
     * Get the status text for an order
     */
    fun getOrderStatusText(order: Order): String {
        return when {
            !order.dispatchStatus && !order.deliveryStatus -> "Pending Dispatch"
            order.dispatchStatus && !order.deliveryStatus -> "In Transit"
            order.dispatchStatus && order.deliveryStatus && !order.returnStatus -> "Delivered"
            order.dispatchStatus && order.deliveryStatus && order.returnStatus -> "Completed"
            else -> "Unknown"
        }
    }
    
    /**
     * Format currency amount
     */
    fun formatCurrency(amount: Double): String {
        return "$${String.format("%.2f", amount)}"
    }
}
