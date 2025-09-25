package com.pave.driversapp.presentation.ui.orders

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.pave.driversapp.domain.model.Order
import com.pave.driversapp.domain.model.ScheduledOrder
import com.pave.driversapp.domain.repository.OrdersRepositoryImpl
import com.pave.driversapp.domain.repository.MembershipRepositoryImpl
import com.google.firebase.firestore.FirebaseFirestore
import com.pave.driversapp.presentation.viewmodel.OrdersViewModel
import com.pave.driversapp.presentation.ui.components.ScheduledOrderCard

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OrdersDashboardScreen(
    orgId: String,
    orgName: String,
    userRole: Int,
    onBack: () -> Unit,
    onOrderClick: (Order) -> Unit = {},
    onScheduledOrderClick: (ScheduledOrder) -> Unit = {}
) {
    val ordersRepository = remember { OrdersRepositoryImpl() }
    val membershipRepository = remember { MembershipRepositoryImpl(FirebaseFirestore.getInstance()) }
    val ordersViewModel: OrdersViewModel = viewModel { 
        OrdersViewModel(ordersRepository, membershipRepository) 
    }
    val uiState by ordersViewModel.uiState.collectAsStateWithLifecycle()
    
    // Initialize the view model
    LaunchedEffect(orgId, userRole) {
        ordersViewModel.initialize(orgId, "current_user_id") // TODO: Get actual user ID
    }
    
    // Tab state for switching between orders and scheduled orders
    var selectedTab by remember { mutableStateOf(0) }
    
    // Load scheduled orders when tab is switched
    LaunchedEffect(selectedTab) {
        if (selectedTab == 1) {
            ordersViewModel.loadScheduledOrders(uiState.selectedDate, uiState.selectedVehicle)
        }
    }
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        // Top App Bar
        TopAppBar(
            title = { 
                Text(
                    text = "$orgName - Orders",
                    color = Color.White,
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                ) 
            },
            navigationIcon = {
                IconButton(onClick = onBack) {
                    Icon(Icons.Filled.ArrowBack, "Back", tint = Color.White)
                }
            },
            actions = {
                IconButton(onClick = { ordersViewModel.refreshOrders() }) {
                    Icon(Icons.Filled.Refresh, "Refresh", tint = Color.White)
                }
            },
            colors = TopAppBarDefaults.topAppBarColors(
                containerColor = Color.Black
            )
        )
        
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Date Selector Row (matching the green calendar design)
            DateSelectorRow(
                ordersViewModel = ordersViewModel,
                userRole = userRole
            )
            
            // Vehicle Filter Chips (matching the design)
            if (ordersViewModel.canUseVehicleFilter()) {
                VehicleFilterChips(
                    availableVehicles = uiState.availableVehicles,
                    selectedVehicle = uiState.selectedVehicle,
                    onVehicleSelected = { ordersViewModel.selectVehicle(it) }
                )
            }
            
            // Tab Row for Orders and Scheduled Orders
            TabRow(
                selectedTabIndex = selectedTab,
                containerColor = Color(0xFF1E1E1E),
                contentColor = Color.White
            ) {
                Tab(
                    selected = selectedTab == 0,
                    onClick = { selectedTab = 0 },
                    text = {
                        Text(
                            text = "Orders",
                            color = if (selectedTab == 0) Color.White else Color.Gray
                        )
                    }
                )
                Tab(
                    selected = selectedTab == 1,
                    onClick = { selectedTab = 1 },
                    text = {
                        Text(
                            text = "SCH Orders",
                            color = if (selectedTab == 1) Color.White else Color.Gray
                        )
                    }
                )
            }
            
            // Content based on selected tab
            when (selectedTab) {
                0 -> {
                    // Regular Orders Grid (Two Column Layout)
                    if (uiState.isLoading) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(color = Color(0xFF4CAF50))
                        }
                    } else if (uiState.orders.isEmpty()) {
                        EmptyOrdersState()
                    } else {
                        OrdersGrid(
                            orders = uiState.orders,
                            onOrderClick = onOrderClick
                        )
                    }
                }
                1 -> {
                    // Scheduled Orders List
                    if (uiState.isLoading) {
                        Box(
                            modifier = Modifier.fillMaxSize(),
                            contentAlignment = Alignment.Center
                        ) {
                            CircularProgressIndicator(color = Color(0xFF4CAF50))
                        }
                    } else if (uiState.scheduledOrders.isEmpty()) {
                        EmptyScheduledOrdersState()
                    } else {
                        ScheduledOrdersList(
                            scheduledOrders = uiState.scheduledOrders,
                            onScheduledOrderClick = onScheduledOrderClick
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun EmptyOrdersState() {
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
                tint = Color.White.copy(alpha = 0.5f),
                modifier = Modifier.size(64.dp)
            )
            Text(
                text = "No orders found for selected date",
                style = MaterialTheme.typography.bodyLarge,
                color = Color.White.copy(alpha = 0.7f)
            )
        }
    }
}

@Composable
fun EmptyScheduledOrdersState() {
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = "📅",
                fontSize = 64.sp
            )
            Text(
                text = "No scheduled orders found for selected date",
                style = MaterialTheme.typography.bodyLarge,
                color = Color.White.copy(alpha = 0.7f)
            )
        }
    }
}

@Composable
fun ScheduledOrdersList(
    scheduledOrders: List<ScheduledOrder>,
    onScheduledOrderClick: (ScheduledOrder) -> Unit
) {
    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(scheduledOrders) { scheduledOrder ->
            ScheduledOrderCard(
                scheduledOrder = scheduledOrder,
                onClick = { onScheduledOrderClick(scheduledOrder) }
            )
        }
    }
}

@Composable
fun DateSelectorRow(
    ordersViewModel: OrdersViewModel,
    userRole: Int
) {
    val accessibleDates = ordersViewModel.getAccessibleDates()
    val selectedDate = ordersViewModel.uiState.value.selectedDate
    
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        // Calendar Icon (Green)
        Icon(
            imageVector = Icons.Filled.DateRange,
            contentDescription = "Calendar",
            tint = Color(0xFF4CAF50), // Green color
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
                    isToday -> "TODAY"
                    isTomorrow -> "TOMORROW"
                    else -> {
                        val dayOfWeek = getDayOfWeek(date)
                        val dayOfMonth = getDayOfMonth(date)
                        "$dayOfWeek $dayOfMonth"
                    }
                }
                
                Card(
                    modifier = Modifier.clickable { 
                        ordersViewModel.selectDate(date) 
                    },
                    colors = CardDefaults.cardColors(
                        containerColor = if (isSelected) Color.White else Color.Transparent
                    ),
                    shape = RoundedCornerShape(8.dp)
                ) {
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally,
                        modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp)
                    ) {
                        Text(
                            text = "SEP", // Month abbreviation
                            fontSize = 10.sp,
                            color = if (isSelected) Color.Black else Color(0xFF4CAF50),
                            fontWeight = FontWeight.Medium
                        )
                        Text(
                            text = displayText,
                            fontSize = 12.sp,
                            color = if (isSelected) Color.Black else Color.White,
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
                        )
                    }
                }
            }
        }
    }
}

@Composable
fun VehicleFilterChips(
    availableVehicles: List<String>,
    selectedVehicle: String?,
    onVehicleSelected: (String?) -> Unit
) {
    LazyRow(
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        // All Vehicles chip
        item {
            VehicleChip(
                text = "All Vehicles",
                isSelected = selectedVehicle == null,
                onClick = { onVehicleSelected(null) }
            )
        }
        
        // Individual vehicle chips
        items(availableVehicles) { vehicle ->
            VehicleChip(
                text = vehicle,
                isSelected = selectedVehicle == vehicle,
                onClick = { onVehicleSelected(vehicle) }
            )
        }
    }
}

@Composable
fun VehicleChip(
    text: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Card(
        modifier = Modifier.clickable { onClick() },
        colors = CardDefaults.cardColors(
            containerColor = if (isSelected) Color.White else Color.Transparent
        ),
        shape = RoundedCornerShape(8.dp)
    ) {
        Text(
            text = text,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
            color = if (isSelected) Color.Black else Color.White,
            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal,
            fontSize = 14.sp
        )
    }
}

@Composable
fun OrdersGrid(
    orders: List<Order>,
    onOrderClick: (Order) -> Unit
) {
    LazyColumn(
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        items(orders.chunked(2)) { orderPair ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Left Column - Vehicle/Time Card
                VehicleTimeCard(
                    order = orderPair[0],
                    onClick = { onOrderClick(orderPair[0]) },
                    modifier = Modifier.weight(1f)
                )
                
                // Right Column - Order Details Card
                OrderDetailsCard(
                    order = orderPair[0],
                    onClick = { onOrderClick(orderPair[0]) },
                    modifier = Modifier.weight(1f)
                )
            }
            
            // Second row if there's a second order
            if (orderPair.size > 1) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    VehicleTimeCard(
                        order = orderPair[1],
                        onClick = { onOrderClick(orderPair[1]) },
                        modifier = Modifier.weight(1f)
                    )
                    
                    OrderDetailsCard(
                        order = orderPair[1],
                        onClick = { onOrderClick(orderPair[1]) },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

@Composable
fun VehicleTimeCard(
    order: Order,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.clickable { onClick() },
        colors = CardDefaults.cardColors(
            containerColor = Color.Black
        ),
        shape = RoundedCornerShape(8.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color.Blue)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Text(
                text = order.vehicleNumber,
                color = Color.White,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                textAlign = TextAlign.Center
            )
            
            Text(
                text = order.timeSlot,
                color = Color.White,
                fontSize = 14.sp,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
fun OrderDetailsCard(
    order: Order,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier.clickable { onClick() },
        colors = CardDefaults.cardColors(
            containerColor = Color.Black
        ),
        shape = RoundedCornerShape(8.dp),
        border = androidx.compose.foundation.BorderStroke(1.dp, Color.Blue)
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            // Name
            Text(
                text = order.clientName,
                color = Color.White,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold
            )
            
            // Address
            Text(
                text = "${order.address}, ${order.regionName}",
                color = Color.White.copy(alpha = 0.8f),
                fontSize = 12.sp
            )
            
            // Product
            Text(
                text = order.productName,
                color = Color.White,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium
            )
            
            // Quantity
            Text(
                text = order.productQuant.toString(),
                color = Color.White,
                fontSize = 14.sp
            )
            
            // Payment Method (with color coding)
            val paymentColor = when {
                order.paymentMethod.contains("CASH", ignoreCase = true) -> Color.Green
                else -> Color.White
            }
            Text(
                text = order.paymentMethod,
                color = paymentColor,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium
            )
            
            // Amount
            Text(
                text = "Rs ${order.totalAmount.toInt()}",
                color = Color.White,
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold
            )
        }
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
