package com.pave.driversapp.domain.repository

import com.pave.driversapp.domain.model.Order
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import java.text.SimpleDateFormat
import java.util.*

interface OrdersRepository {
    suspend fun getOrders(orgId: String, date: String, vehicleNumber: String? = null): Flow<List<Order>>
    suspend fun getAvailableVehicles(orgId: String): List<String>
    suspend fun getOrderById(orderId: String): Order?
}

class OrdersRepositoryImpl : OrdersRepository {
    
    // Test data generator
    private fun generateTestOrders(orgId: String, date: String): List<Order> {
        val orders = mutableListOf<Order>()
        val vehicles = listOf("MH34 AP0148", "MH34 BG2138", "MH34 AP0147", "MH34 CD1234", "MH34 EF5678")
        val clients = listOf("Shubham Bhagat", "Someshwar Udakwar", "Sunil Tapase", "Rajesh Kumar", "Priya Sharma")
        val addresses = listOf(
            "Dadmahal, Chandrapur",
            "Marki, Mukutban", 
            "Gol bazar, Chandrapur",
            "Station Road, Nagpur",
            "Civil Lines, Mumbai"
        )
        val regions = listOf("Chandrapur", "Mukutban", "Nagpur", "Mumbai", "Pune")
        val products = listOf("BRICKS", "CEMENT", "STEEL", "SAND", "AGGREGATE")
        val paymentMethods = listOf("Pay on Delivery", "CASH", "Pay Later", "Online", "Cheque")
        
        val timeSlots = listOf(
            "06:00" to "09:00",
            "06:00" to "12:00", 
            "09:00" to "12:00",
            "12:00" to "15:00",
            "15:00" to "18:00"
        )
        
        // Generate 3-4 orders per day
        val orderCount = 4 // Fixed count for now
        
        repeat(orderCount) { index ->
            val vehicle = vehicles[index % vehicles.size]
            val client = clients[index % clients.size]
            val address = addresses[index % addresses.size]
            val region = regions[index % regions.size]
            val product = products[index % products.size]
            val timeSlot = timeSlots[index % timeSlots.size]
            val quantity = 1000 + (index * 500) % 4000
            val unitPrice = 3.0 + (index * 0.5) % 5.0
            val paymentMethod = paymentMethods[index % paymentMethods.size]
            
            // Status logic: simulate different order states
            val dispatchStatus = when {
                index < orderCount * 0.2 -> false  // 20% not dispatched
                else -> true
            }
            
            val deliveryStatus = when {
                !dispatchStatus -> false
                index < orderCount * 0.6 -> false  // 40% dispatched but not delivered
                else -> true
            }
            
            val returnStatus = when {
                !deliveryStatus -> false
                index < orderCount * 0.8 -> false  // 20% delivered but not returned
                else -> true
            }
            
            orders.add(
                Order(
                    orderId = "ORD_${orgId}_${date}_${index + 1}",
                    orgId = orgId,
                    clientName = client,
                    address = address,
                    regionName = region,
                    productName = product,
                    productQuant = quantity,
                    productUnitPrice = unitPrice,
                    vehicleNumber = vehicle,
                    dispatchStart = timeSlot.first,
                    dispatchEnd = timeSlot.second,
                    dispatchDate = date,
                    paymentMethod = paymentMethod,
                    dispatchStatus = dispatchStatus,
                    deliveryStatus = deliveryStatus,
                    returnStatus = returnStatus
                )
            )
        }
        
        return orders.sortedBy { it.dispatchStart }
    }
    
    override suspend fun getOrders(orgId: String, date: String, vehicleNumber: String?): Flow<List<Order>> = flow {
        emit(emptyList()) // Show loading
        
        // Simulate network delay
        kotlinx.coroutines.delay(1000)
        
        val allOrders = generateTestOrders(orgId, date)
        val filteredOrders = if (vehicleNumber != null) {
            allOrders.filter { it.vehicleNumber == vehicleNumber }
        } else {
            allOrders
        }
        
        emit(filteredOrders)
    }
    
    override suspend fun getAvailableVehicles(orgId: String): List<String> {
        // Simulate network delay
        kotlinx.coroutines.delay(500)
        
        return listOf("MH34 AP0148", "MH34 BG2138", "MH34 AP0147", "MH34 CD1234", "MH34 EF5678")
    }
    
    override suspend fun getOrderById(orderId: String): Order? {
        // Simulate network delay
        kotlinx.coroutines.delay(300)
        
        // For now, return null - we'll implement this when we add order details
        return null
    }
}
