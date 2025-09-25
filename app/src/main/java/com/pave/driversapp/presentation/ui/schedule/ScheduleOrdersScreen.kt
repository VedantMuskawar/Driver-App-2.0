package com.pave.driversapp.presentation.ui.schedule

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.pave.driversapp.domain.model.Order
import com.pave.driversapp.domain.repository.OrdersRepositoryImpl
import com.pave.driversapp.presentation.viewmodel.OrdersViewModel
import com.pave.driversapp.presentation.viewmodel.AuthViewModel
import com.pave.driversapp.util.SafeLaunchedEffect

@Composable
fun ScheduleOrdersScreen(
    authViewModel: AuthViewModel,
    onOrderClick: (Order) -> Unit = {}
) {
    val uiState by authViewModel.uiState.collectAsStateWithLifecycle()
    val user = uiState.authResult?.user
    val orgId = uiState.selectedOrgId
    val orgName = user?.orgName ?: uiState.authResult?.organizations?.find { it.orgID == orgId }?.orgName ?: "Organization"
    val userRole = user?.role ?: 0
    
    android.util.Log.d("ScheduleOrdersScreen", "🔄 ScheduleOrdersScreen initialized")
    android.util.Log.d("ScheduleOrdersScreen", "👤 User: ${user?.name}")
    android.util.Log.d("ScheduleOrdersScreen", "🏢 OrgId: $orgId")
    android.util.Log.d("ScheduleOrdersScreen", "🏢 OrgName: $orgName")
    android.util.Log.d("ScheduleOrdersScreen", "👑 UserRole: $userRole")
    
    val ordersRepository = remember { OrdersRepositoryImpl() }
    val ordersViewModel: OrdersViewModel = viewModel { OrdersViewModel(ordersRepository) }
    val ordersUiState by ordersViewModel.uiState.collectAsStateWithLifecycle()
    
    android.util.Log.d("ScheduleOrdersScreen", "📊 OrdersUiState: isLoading=${ordersUiState.isLoading}, ordersCount=${ordersUiState.orders.size}")
    android.util.Log.d("ScheduleOrdersScreen", "🚗 Available vehicles: ${ordersUiState.availableVehicles.size}")
    android.util.Log.d("ScheduleOrdersScreen", "📅 Selected date: ${ordersUiState.selectedDate}")
    android.util.Log.d("ScheduleOrdersScreen", "🚗 Selected vehicle: ${ordersUiState.selectedVehicle}")
    
    // Initialize the orders view model
    SafeLaunchedEffect(orgId, userRole) {
        android.util.Log.d("ScheduleOrdersScreen", "🚀 SafeLaunchedEffect triggered - initializing OrdersViewModel")
        android.util.Log.d("ScheduleOrdersScreen", "📋 Calling ordersViewModel.initialize($orgId, $userRole)")
        ordersViewModel.initialize(orgId, userRole)
    }
    
    // Monitor ordersUiState changes - only log when orders count changes
    SafeLaunchedEffect(ordersUiState.orders.size, ordersUiState.isLoading) {
        android.util.Log.d("ScheduleOrdersScreen", "📊 OrdersUiState changed:")
        android.util.Log.d("ScheduleOrdersScreen", "   - isLoading: ${ordersUiState.isLoading}")
        android.util.Log.d("ScheduleOrdersScreen", "   - orders count: ${ordersUiState.orders.size}")
        android.util.Log.d("ScheduleOrdersScreen", "   - available vehicles: ${ordersUiState.availableVehicles.size}")
        android.util.Log.d("ScheduleOrdersScreen", "   - selected date: ${ordersUiState.selectedDate}")
        android.util.Log.d("ScheduleOrdersScreen", "   - selected vehicle: ${ordersUiState.selectedVehicle}")
        android.util.Log.d("ScheduleOrdersScreen", "   - error message: ${ordersUiState.errorMessage}")
        
        if (ordersUiState.orders.isNotEmpty()) {
            android.util.Log.d("ScheduleOrdersScreen", "📋 Orders found:")
            ordersUiState.orders.forEachIndexed { index, order ->
                android.util.Log.d("ScheduleOrdersScreen", "   Order $index: ${order.orderId} - ${order.clientName} - ${order.address}")
            }
        }
    }
    
    // Animation states
    val contentAlpha by animateFloatAsState(
        targetValue = if (ordersUiState.isLoading) 0.6f else 1f,
        animationSpec = tween(500),
        label = "contentAlpha"
    )
    
    val loadingScale by animateFloatAsState(
        targetValue = if (ordersUiState.isLoading) 1.1f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "loadingScale"
    )

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .alpha(contentAlpha)
    ) {
        // Header (Uber-style) with slide animation
        AnimatedVisibility(
            visible = true,
            enter = slideInVertically(
                initialOffsetY = { -it },
                animationSpec = tween(600, easing = EaseOutCubic)
            )
        ) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = Color.Black
                ),
                shape = RoundedCornerShape(0.dp)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp)
                ) {
                    Text(
                        text = "Orders",
                        style = MaterialTheme.typography.headlineMedium,
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                    
                    Text(
                        text = "${ordersUiState.orders.size} orders today",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White.copy(alpha = 0.7f)
                    )
                }
            }
        }
        
        // Date Selector Row (Uber-style) with slide animation
        AnimatedVisibility(
            visible = true,
            enter = slideInVertically(
                initialOffsetY = { -it },
                animationSpec = tween(700, easing = EaseOutCubic)
            )
        ) {
            DateSelectorRow(
                ordersViewModel = ordersViewModel,
                userRole = userRole
            )
        }
        
        // Vehicle Filter Chips (Uber-style) with slide animation
        AnimatedVisibility(
            visible = ordersViewModel.canUseVehicleFilter(),
            enter = slideInVertically(
                initialOffsetY = { -it },
                animationSpec = tween(800, easing = EaseOutCubic)
            )
        ) {
            VehicleFilterChips(
                availableVehicles = ordersUiState.availableVehicles,
                selectedVehicle = ordersUiState.selectedVehicle,
                onVehicleSelected = { ordersViewModel.selectVehicle(it) }
            )
        }
        
        // Orders List (Uber-style) with animations
        AnimatedVisibility(
            visible = ordersUiState.isLoading,
            enter = fadeIn(animationSpec = tween(300)) + scaleIn(
                initialScale = 0.8f,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessMedium
                )
            ),
            exit = fadeOut(animationSpec = tween(200))
        ) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    CircularProgressIndicator(
                        color = Color.White,
                        modifier = Modifier.scale(loadingScale)
                    )
                    Text(
                        text = "Loading orders...",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White.copy(alpha = 0.7f)
                    )
                }
            }
        }
        
        AnimatedVisibility(
            visible = !ordersUiState.isLoading && ordersUiState.orders.isEmpty(),
            enter = fadeIn(animationSpec = tween(500)) + scaleIn(
                initialScale = 0.9f,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessLow
                )
            ),
            exit = fadeOut(animationSpec = tween(300))
        ) {
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(16.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.List,
                        contentDescription = "No orders",
                        tint = Color.Gray,
                        modifier = Modifier.size(64.dp)
                    )
                    Text(
                        text = "No orders found",
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.White
                    )
                    Text(
                        text = "Orders will appear here when available",
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.Gray,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
        
        AnimatedVisibility(
            visible = !ordersUiState.isLoading && ordersUiState.orders.isNotEmpty(),
            enter = fadeIn(animationSpec = tween(500)) + slideInVertically(
                initialOffsetY = { it },
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessLow
                )
            ),
            exit = fadeOut(animationSpec = tween(300))
        ) {
            LazyColumn(
                modifier = Modifier.fillMaxSize(),
                contentPadding = PaddingValues(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                items(
                    items = ordersUiState.orders,
                    key = { order -> order.orderId } // Add key for better performance
                ) { order ->
                    AnimatedVisibility(
                        visible = true,
                        enter = fadeIn(animationSpec = tween(300)) + slideInVertically(
                            initialOffsetY = { 50 },
                            animationSpec = spring(
                                dampingRatio = Spring.DampingRatioMediumBouncy,
                                stiffness = Spring.StiffnessMedium
                            )
                        )
                    ) {
                        OrderCard(
                            order = order,
                            onClick = { onOrderClick(order) }
                        )
                    }
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DateSelectorRow(
    ordersViewModel: OrdersViewModel,
    userRole: Int
) {
    val accessibleDates = ordersViewModel.getAccessibleDates()
    val selectedDate = ordersViewModel.uiState.value.selectedDate
    
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF1E1E1E)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Calendar Icon (Uber-style)
            Icon(
                imageVector = Icons.Filled.DateRange,
                contentDescription = "Calendar",
                tint = Color.White,
                modifier = Modifier.size(24.dp)
            )
            
            // Date Chips
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(accessibleDates) { date ->
                    val isSelected = date == selectedDate
                    val isToday = ordersViewModel.isToday(date)
                    val isTomorrow = ordersViewModel.isTomorrow(date)
                    
                    val displayText = when {
                        isToday -> "Today"
                        isTomorrow -> "Tomorrow"
                        else -> {
                            val dayOfWeek = getDayOfWeek(date)
                            val dayOfMonth = getDayOfMonth(date)
                            "$dayOfWeek $dayOfMonth"
                        }
                    }
                    
                    FilterChip(
                        onClick = { ordersViewModel.selectDate(date) },
                        label = {
                            Text(
                                text = displayText,
                                fontSize = 14.sp,
                                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                            )
                        },
                        selected = isSelected,
                        colors = FilterChipDefaults.filterChipColors(
                            selectedContainerColor = Color.White,
                            selectedLabelColor = Color.Black,
                            containerColor = Color.Gray.copy(alpha = 0.2f),
                            labelColor = Color.White
                        )
                    )
                }
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun VehicleFilterChips(
    availableVehicles: List<String>,
    selectedVehicle: String?,
    onVehicleSelected: (String?) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF1E1E1E)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        LazyRow(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            // All Vehicles chip
            item {
                FilterChip(
                    onClick = { onVehicleSelected(null) },
                    label = { Text("All Vehicles", fontSize = 14.sp) },
                    selected = selectedVehicle == null,
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = Color.White,
                        selectedLabelColor = Color.Black,
                        containerColor = Color.Gray.copy(alpha = 0.2f),
                        labelColor = Color.White
                    )
                )
            }
            
            // Individual vehicle chips
            items(availableVehicles) { vehicle ->
                FilterChip(
                    onClick = { onVehicleSelected(vehicle) },
                    label = { Text(vehicle, fontSize = 14.sp) },
                    selected = selectedVehicle == vehicle,
                    colors = FilterChipDefaults.filterChipColors(
                        selectedContainerColor = Color.White,
                        selectedLabelColor = Color.Black,
                        containerColor = Color.Gray.copy(alpha = 0.2f),
                        labelColor = Color.White
                    )
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OrderCard(
    order: Order,
    onClick: () -> Unit
) {
    // Animation states
    var isPressed by remember { mutableStateOf(false) }
    
    val cardScale by animateFloatAsState(
        targetValue = if (isPressed) 0.95f else 1f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessHigh
        ),
        label = "cardScale"
    )
    
    val cardElevation by animateDpAsState(
        targetValue = if (isPressed) 8.dp else 2.dp,
        animationSpec = tween(200),
        label = "cardElevation"
    )
    
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .scale(cardScale),
        onClick = onClick,
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF1E1E1E)
        ),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = cardElevation)
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Status Indicator
            Card(
                modifier = Modifier.size(12.dp),
                colors = CardDefaults.cardColors(
                    containerColor = getOrderStatusColor(order)
                ),
                shape = RoundedCornerShape(6.dp)
            ) {}
            
            Spacer(modifier = Modifier.width(16.dp))
            
            // Order Details
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = order.clientName,
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
                
                Text(
                    text = "${order.address}, ${order.regionName}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.Gray
                )
                
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    Text(
                        text = order.productName,
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White
                    )
                    
                    Text(
                        text = "•",
                        color = Color.Gray
                    )
                    
                    Text(
                        text = order.vehicleNumber,
                        style = MaterialTheme.typography.bodyMedium,
                        color = Color.White,
                        fontWeight = FontWeight.Medium
                    )
                }
                
                Text(
                    text = order.timeSlot,
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray
                )
            }
            
            // Amount and Payment
            Column(
                horizontalAlignment = Alignment.End
            ) {
                Text(
                    text = "Rs ${order.totalAmount.toInt()}",
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.Black,
                    fontWeight = FontWeight.Bold
                )
                
                Text(
                    text = order.paymentMethod,
                    style = MaterialTheme.typography.bodySmall,
                    color = if (order.paymentMethod.contains("CASH", ignoreCase = true)) Color(0xFF4CAF50) else Color.Gray
                )
            }
        }
    }
}

private fun getOrderStatusColor(order: Order): Color {
    return when {
        !order.dispatchStatus && !order.deliveryStatus -> Color.Red
        order.dispatchStatus && !order.deliveryStatus -> Color(0xFFFF9800)
        order.dispatchStatus && order.deliveryStatus && !order.returnStatus -> Color(0xFF4CAF50)
        order.dispatchStatus && order.deliveryStatus && order.returnStatus -> Color(0xFF2196F3)
        else -> Color.Gray
    }
}

// Helper functions
private fun getDayOfWeek(dateString: String): String {
    return try {
        val inputFormat = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())
        val date = inputFormat.parse(dateString)
        val outputFormat = java.text.SimpleDateFormat("EEE", java.util.Locale.getDefault())
        outputFormat.format(date ?: java.util.Date()).uppercase()
    } catch (e: Exception) {
        "MON"
    }
}

private fun getDayOfMonth(dateString: String): String {
    return try {
        val inputFormat = java.text.SimpleDateFormat("yyyy-MM-dd", java.util.Locale.getDefault())
        val date = inputFormat.parse(dateString)
        val outputFormat = java.text.SimpleDateFormat("dd", java.util.Locale.getDefault())
        outputFormat.format(date ?: java.util.Date())
    } catch (e: Exception) {
        "01"
    }
}