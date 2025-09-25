package com.pave.driversapp.domain.repository

import com.pave.driversapp.domain.model.Order
import com.pave.driversapp.domain.model.ScheduledOrder
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.tasks.await

interface OrdersRepository {
    suspend fun getOrders(orgId: String, date: String, vehicleNumber: String? = null, userRole: com.pave.driversapp.domain.model.UserRole? = null, driverVehicleId: String? = null): Flow<List<Order>>
    suspend fun getScheduledOrders(orgId: String, date: String, vehicleNumber: String? = null, userRole: com.pave.driversapp.domain.model.UserRole? = null, driverVehicleId: String? = null): Flow<List<ScheduledOrder>>
    suspend fun getAvailableVehicles(orgId: String, date: String, userRole: com.pave.driversapp.domain.model.UserRole? = null, driverVehicleId: String? = null): List<String>
    suspend fun getOrderById(orderId: String): Order?
    suspend fun getScheduledOrderById(orderId: String): ScheduledOrder?
}

class OrdersRepositoryImpl : OrdersRepository {
    
    override suspend fun getOrders(orgId: String, date: String, vehicleNumber: String?, userRole: com.pave.driversapp.domain.model.UserRole?, driverVehicleId: String?): Flow<List<Order>> = flow {
        android.util.Log.d("OrdersRepository", "🔄 Starting ORDERS fetch for orgId: $orgId, date: $date, vehicle: $vehicleNumber, role: ${userRole?.displayName}")
        emit(emptyList()) // Show loading
        
        try {
            val firestore = com.google.firebase.firestore.FirebaseFirestore.getInstance()
            val collectionRef = firestore.collection("SCH_ORDERS")
            
            android.util.Log.d("OrdersRepository", "📡 Querying SCH_ORDERS collection using optimized indexes...")
            
            // Convert date string to Timestamp for proper indexing
            val dateFormat = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())
            val startOfDay = dateFormat.parse(date) ?: java.util.Date()
            val endOfDay = java.util.Calendar.getInstance().apply {
                time = startOfDay
                add(java.util.Calendar.DAY_OF_MONTH, 1)
            }.time
            
            val startTimestamp = com.google.firebase.Timestamp(startOfDay)
            val endTimestamp = com.google.firebase.Timestamp(endOfDay)
            
            android.util.Log.d("OrdersRepository", "📅 Using deliveryDate range query: $startTimestamp to $endTimestamp")
            
            // Use simpler query to avoid complex composite index requirements
            // First filter by orgID and date range, then filter by vehicle in memory if needed
            var query = collectionRef
                .whereEqualTo("orgID", orgId) // Indexed field
                .whereGreaterThanOrEqualTo("deliveryDate", startTimestamp) // Indexed field
                .whereLessThan("deliveryDate", endTimestamp) // Indexed field
            
            android.util.Log.d("OrdersRepository", "⏳ Executing Firestore query with basic indexes (orgID + deliveryDate)...")
            val snapshot = query.get().await()
            
            android.util.Log.d("OrdersRepository", "📊 Query completed. Found ${snapshot.documents.size} documents")
            
            val orders = snapshot.documents.mapNotNull { document ->
                try {
                    android.util.Log.d("OrdersRepository", "📄 Processing document: ${document.id}")
                    val scheduledOrder = document.toObject(ScheduledOrder::class.java)
                    if (scheduledOrder != null) {
                        // Role-based filtering
                        when (userRole) {
                            com.pave.driversapp.domain.model.UserRole.DRIVER -> {
                                // Drivers can only see orders for their assigned vehicle
                                if (driverVehicleId != null && scheduledOrder.vehicleID != driverVehicleId) {
                                    android.util.Log.d("OrdersRepository", "🚗 Driver filtering: ${scheduledOrder.vehicleNumber} (not driver's vehicle)")
                                    return@mapNotNull null
                                }
                            }
                            com.pave.driversapp.domain.model.UserRole.MANAGER, 
                            com.pave.driversapp.domain.model.UserRole.ADMIN -> {
                                // Managers and Admins can see all orders, but apply vehicle filter if specified
                                if (vehicleNumber != null && scheduledOrder.vehicleNumber != vehicleNumber) {
                                    android.util.Log.d("OrdersRepository", "🚗 Filtering out vehicle: ${scheduledOrder.vehicleNumber} (not matching $vehicleNumber)")
                                    return@mapNotNull null
                                }
                            }
                            null -> {
                                // No role specified, apply vehicle filter if provided
                                if (vehicleNumber != null && scheduledOrder.vehicleNumber != vehicleNumber) {
                                    android.util.Log.d("OrdersRepository", "🚗 Filtering out vehicle: ${scheduledOrder.vehicleNumber} (not matching $vehicleNumber)")
                                    return@mapNotNull null
                                }
                            }
                        }
                        
                        // Convert ScheduledOrder to Order format
                        val order = Order(
                            orderId = document.id,
                            orgId = scheduledOrder.orgID,
                            clientName = scheduledOrder.clientName,
                            address = scheduledOrder.address,
                            regionName = scheduledOrder.regionName,
                            productName = scheduledOrder.productName,
                            productQuant = scheduledOrder.productQuant,
                            productUnitPrice = scheduledOrder.productUnitPrice,
                            vehicleNumber = scheduledOrder.vehicleNumber,
                            dispatchStart = scheduledOrder.dispatchStart?.toDate()?.let { 
                                java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault()).format(it) 
                            } ?: "00:00",
                            dispatchEnd = scheduledOrder.dispatchEnd?.toDate()?.let { 
                                java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault()).format(it) 
                            } ?: "23:59",
                            dispatchDate = date,
                            paymentMethod = scheduledOrder.paySchedule,
                            dispatchStatus = scheduledOrder.dispatchStatus,
                            deliveryStatus = scheduledOrder.deliveryStatus,
                            returnStatus = scheduledOrder.paymentStatus // Using payment status as return status
                        )
                        android.util.Log.d("OrdersRepository", "✅ Successfully converted order: ${order.orderId} - ${order.clientName}")
                        order
                    } else {
                        android.util.Log.w("OrdersRepository", "⚠️ ScheduledOrder is null for document: ${document.id}")
                        null
                    }
                } catch (e: Exception) {
                    android.util.Log.e("OrdersRepository", "❌ Error converting SCH_ORDER to ORDER document ${document.id}: ${e.message}")
                    null
                }
            }
            
            android.util.Log.d("OrdersRepository", "🎯 Final result: ${orders.size} ORDERS converted from SCH_ORDERS (using compound indexes)")
            orders.forEach { order ->
                android.util.Log.d("OrdersRepository", "📋 Order: ${order.orderId} - ${order.clientName} - ${order.address}")
            }
            
            emit(orders)
        } catch (e: Exception) {
            android.util.Log.e("OrdersRepository", "💥 Error fetching ORDERS from SCH_ORDERS: ${e.message}")
            android.util.Log.e("OrdersRepository", "💥 Exception type: ${e.javaClass.simpleName}")
            android.util.Log.e("OrdersRepository", "💥 Stack trace: ${e.stackTrace.joinToString("\n")}")
            // No fallback - just emit empty list
            emit(emptyList())
        }
    }
    
    override suspend fun getScheduledOrders(orgId: String, date: String, vehicleNumber: String?, userRole: com.pave.driversapp.domain.model.UserRole?, driverVehicleId: String?): Flow<List<ScheduledOrder>> = flow {
        android.util.Log.d("OrdersRepository", "🔄 Starting SCH_ORDERS fetch for orgId: $orgId, date: $date, vehicle: $vehicleNumber")
        emit(emptyList()) // Show loading
        
        try {
            val firestore = com.google.firebase.firestore.FirebaseFirestore.getInstance()
            val collectionRef = firestore.collection("SCH_ORDERS")
            
            android.util.Log.d("OrdersRepository", "📡 Querying SCH_ORDERS collection using optimized indexes...")
            
            // Convert date string to Timestamp for proper indexing
            val dateFormat = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())
            val startOfDay = dateFormat.parse(date) ?: java.util.Date()
            val endOfDay = java.util.Calendar.getInstance().apply {
                time = startOfDay
                add(java.util.Calendar.DAY_OF_MONTH, 1)
            }.time
            
            val startTimestamp = com.google.firebase.Timestamp(startOfDay)
            val endTimestamp = com.google.firebase.Timestamp(endOfDay)
            
            android.util.Log.d("OrdersRepository", "📅 Using deliveryDate range query: $startTimestamp to $endTimestamp")
            
            // Use simpler query to avoid complex composite index requirements
            // First filter by orgID and date range, then filter by vehicle in memory if needed
            var query = collectionRef
                .whereEqualTo("orgID", orgId) // Indexed field
                .whereGreaterThanOrEqualTo("deliveryDate", startTimestamp) // Indexed field
                .whereLessThan("deliveryDate", endTimestamp) // Indexed field
            
            android.util.Log.d("OrdersRepository", "⏳ Executing Firestore query with basic indexes (orgID + deliveryDate)...")
            val snapshot = query.get().await()
            
            android.util.Log.d("OrdersRepository", "📊 Query completed. Found ${snapshot.documents.size} documents")
            
            val scheduledOrders = snapshot.documents.mapNotNull { document ->
                try {
                    android.util.Log.d("OrdersRepository", "📄 Processing document: ${document.id}")
                    val scheduledOrder = document.toObject(ScheduledOrder::class.java)
                    if (scheduledOrder != null) {
                        // Apply vehicle filter in memory if specified
                        if (vehicleNumber != null && scheduledOrder.vehicleNumber != vehicleNumber) {
                            android.util.Log.d("OrdersRepository", "🚗 Filtering out vehicle: ${scheduledOrder.vehicleNumber} (not matching $vehicleNumber)")
                            return@mapNotNull null
                        }
                        
                        val order = scheduledOrder.copy(orderId = document.id)
                        android.util.Log.d("OrdersRepository", "✅ Successfully parsed order: ${order.orderId} - ${order.clientName}")
                        order
                    } else {
                        null
                    }
                } catch (e: Exception) {
                    android.util.Log.e("OrdersRepository", "❌ Error parsing SCH_ORDER document ${document.id}: ${e.message}")
                    null
                }
            }
            
            android.util.Log.d("OrdersRepository", "🎯 Final result: ${scheduledOrders.size} SCH_ORDERS from Firestore (using basic indexes + memory filtering)")
            scheduledOrders.forEach { order ->
                android.util.Log.d("OrdersRepository", "📋 Order: ${order.orderId} - ${order.clientName} - ${order.address}")
            }
            
            emit(scheduledOrders)
        } catch (e: Exception) {
            android.util.Log.e("OrdersRepository", "💥 Error fetching SCH_ORDERS: ${e.message}")
            android.util.Log.e("OrdersRepository", "💥 Exception type: ${e.javaClass.simpleName}")
            android.util.Log.e("OrdersRepository", "💥 Stack trace: ${e.stackTrace.joinToString("\n")}")
            // No fallback - just emit empty list
            emit(emptyList())
        }
    }
    
    override suspend fun getAvailableVehicles(orgId: String, date: String, userRole: com.pave.driversapp.domain.model.UserRole?, driverVehicleId: String?): List<String> {
        android.util.Log.d("OrdersRepository", "🔄 Fetching available vehicles for orgId: $orgId, date: $date, role: ${userRole?.displayName}")
        
        return try {
            val firestore = com.google.firebase.firestore.FirebaseFirestore.getInstance()
            val collectionRef = firestore.collection("SCH_ORDERS")
            
            android.util.Log.d("OrdersRepository", "📡 Querying SCH_ORDERS collection using optimized indexes...")
            
            // Convert date string to Timestamp for proper indexing
            val dateFormat = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())
            val startOfDay = dateFormat.parse(date) ?: java.util.Date()
            val endOfDay = java.util.Calendar.getInstance().apply {
                time = startOfDay
                add(java.util.Calendar.DAY_OF_MONTH, 1)
            }.time
            
            val startTimestamp = com.google.firebase.Timestamp(startOfDay)
            val endTimestamp = com.google.firebase.Timestamp(endOfDay)
            
            android.util.Log.d("OrdersRepository", "📅 Using deliveryDate range query: $startTimestamp to $endTimestamp")
            
            // Use compound index: orgID + deliveryDate
            val query = collectionRef
                .whereEqualTo("orgID", orgId) // Indexed field
                .whereGreaterThanOrEqualTo("deliveryDate", startTimestamp) // Indexed field
                .whereLessThan("deliveryDate", endTimestamp) // Indexed field
            
            android.util.Log.d("OrdersRepository", "⏳ Executing Firestore query with compound indexes (orgID + deliveryDate)...")
            val snapshot = query.get().await()
            
            android.util.Log.d("OrdersRepository", "📊 Query completed. Found ${snapshot.documents.size} documents")
            
            // Extract unique vehicle numbers from the filtered results with role-based filtering
            val vehiclesForDate = snapshot.documents.mapNotNull { document ->
                try {
                    val scheduledOrder = document.toObject(ScheduledOrder::class.java)
                    if (scheduledOrder != null) {
                        // Role-based filtering for vehicles
                        when (userRole) {
                            com.pave.driversapp.domain.model.UserRole.DRIVER -> {
                                // Drivers only see their assigned vehicle
                                if (driverVehicleId != null && scheduledOrder.vehicleID == driverVehicleId) {
                                    scheduledOrder.vehicleNumber
                                } else {
                                    null
                                }
                            }
                            com.pave.driversapp.domain.model.UserRole.MANAGER,
                            com.pave.driversapp.domain.model.UserRole.ADMIN -> {
                                // Managers and Admins see all vehicles
                                scheduledOrder.vehicleNumber
                            }
                            null -> {
                                // No role specified, show all vehicles
                                scheduledOrder.vehicleNumber
                            }
                        }
                    } else {
                        null
                    }
                } catch (e: Exception) {
                    android.util.Log.e("OrdersRepository", "❌ Error processing vehicle document ${document.id}: ${e.message}")
                    null
                }
            }.distinct().sorted()
            
            android.util.Log.d("OrdersRepository", "🚗 Found ${vehiclesForDate.size} vehicles for role ${userRole?.displayName}: $vehiclesForDate")
            vehiclesForDate
            
        } catch (e: Exception) {
            android.util.Log.e("OrdersRepository", "💥 Error fetching vehicles: ${e.message}")
            emptyList()
        }
    }
    
    override suspend fun getOrderById(orderId: String): Order? {
        android.util.Log.d("OrdersRepository", "🔄 Fetching order by ID: $orderId")
        
        return try {
            val firestore = com.google.firebase.firestore.FirebaseFirestore.getInstance()
            val document = firestore.collection("SCH_ORDERS").document(orderId).get().await()
            
            if (document.exists()) {
                val scheduledOrder = document.toObject(ScheduledOrder::class.java)
                if (scheduledOrder != null) {
                    // Convert ScheduledOrder to Order format
                    val order = Order(
                        orderId = document.id,
                        orgId = scheduledOrder.orgID,
                        clientName = scheduledOrder.clientName,
                        address = scheduledOrder.address,
                        regionName = scheduledOrder.regionName,
                        productName = scheduledOrder.productName,
                        productQuant = scheduledOrder.productQuant,
                        productUnitPrice = scheduledOrder.productUnitPrice,
                        vehicleNumber = scheduledOrder.vehicleNumber,
                        dispatchStart = scheduledOrder.dispatchStart?.toDate()?.let { 
                            java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault()).format(it) 
                        } ?: "00:00",
                        dispatchEnd = scheduledOrder.dispatchEnd?.toDate()?.let { 
                            java.text.SimpleDateFormat("HH:mm", java.util.Locale.getDefault()).format(it) 
                        } ?: "23:59",
                        dispatchDate = scheduledOrder.deliveryDate?.toDate()?.let { 
                            java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault()).format(it) 
                        } ?: "",
                        paymentMethod = scheduledOrder.paySchedule,
                        dispatchStatus = scheduledOrder.dispatchStatus,
                        deliveryStatus = scheduledOrder.deliveryStatus,
                        returnStatus = scheduledOrder.paymentStatus
                    )
                    android.util.Log.d("OrdersRepository", "✅ Found and converted order: ${order.orderId} - ${order.clientName}")
                    order
                } else {
                    null
                }
            } else {
                android.util.Log.w("OrdersRepository", "⚠️ Order not found: $orderId")
                null
            }
        } catch (e: Exception) {
            android.util.Log.e("OrdersRepository", "💥 Error fetching order by ID: ${e.message}")
            null
        }
    }
    
    override suspend fun getScheduledOrderById(orderId: String): ScheduledOrder? {
        android.util.Log.d("OrdersRepository", "🔄 Fetching scheduled order by ID: $orderId")
        
        return try {
            val firestore = com.google.firebase.firestore.FirebaseFirestore.getInstance()
            val document = firestore.collection("SCH_ORDERS").document(orderId).get().await()
            
            if (document.exists()) {
                val order = document.toObject(ScheduledOrder::class.java)?.copy(orderId = document.id)
                android.util.Log.d("OrdersRepository", "✅ Found scheduled order: ${order?.orderId} - ${order?.clientName}")
                order
            } else {
                android.util.Log.w("OrdersRepository", "⚠️ Scheduled order not found: $orderId")
                null
            }
        } catch (e: Exception) {
            android.util.Log.e("OrdersRepository", "💥 Error fetching scheduled order by ID: ${e.message}")
            null
        }
    }
}