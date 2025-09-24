package com.pave.driversapp.presentation.ui.orders

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.*
import com.pave.driversapp.domain.model.Order
import com.pave.driversapp.domain.model.TripStatus
import com.pave.driversapp.data.repository.DepotRepositoryImpl
import com.pave.driversapp.domain.repository.TripsRepositoryImpl
import com.pave.driversapp.presentation.viewmodel.TripsViewModel
import com.google.firebase.firestore.FirebaseFirestore
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OrderDetailsScreen(
    order: Order,
    onBack: () -> Unit,
    driverId: String,
    orgId: String
) {
    val context = LocalContext.current
    val firestore = FirebaseFirestore.getInstance()
    val fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)
    
    val tripsRepository = remember { TripsRepositoryImpl(firestore, context) }
    val depotRepository = remember { DepotRepositoryImpl(firestore, context) }
    
    val tripsViewModel: TripsViewModel = viewModel {
        TripsViewModel(tripsRepository, depotRepository, fusedLocationClient, context)
    }
    
    val uiState by tripsViewModel.uiState.collectAsStateWithLifecycle()
    
    // Load depot settings
    var depotSettings by remember { mutableStateOf<com.pave.driversapp.domain.model.DepotSettings?>(null) }
    
    LaunchedEffect(orgId) {
        depotRepository.getDepot(orgId).fold(
            onSuccess = { depot ->
                depotSettings = depot
            },
            onFailure = { error ->
                android.util.Log.e("OrderDetails", "Failed to load depot settings: ${error.message}")
            }
        )
    }
    
    // Initialize the view model
    LaunchedEffect(order) {
        tripsViewModel.initialize(order, driverId, orgId)
    }
    
    // Force location update when screen opens
    LaunchedEffect(Unit) {
        tripsViewModel.startLocationUpdates()
    }
    
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        // Map Background
        OrderDetailsMap(
            order = order,
            currentLocation = uiState.currentLocation,
            locationPoints = uiState.locationPoints,
            depotSettings = depotSettings
        )
        
        // Top App Bar
        OrderDetailsTopBar(
            order = order,
            onBack = onBack
        )
        
        // Bottom Action Card - positioned at bottom
        Box(
            modifier = Modifier.align(Alignment.BottomCenter)
        ) {
            OrderDetailsBottomCard(
                order = order,
                uiState = uiState,
                onDispatch = { tripsViewModel.showMeterReadingDialog() },
                onDelivered = { tripsViewModel.showDeliveryPhotoDialog() },
                onReturn = { tripsViewModel.showMeterReadingDialog() },
                onCancel = { tripsViewModel.showCancelDialog() }
            )
        }
        
        // Dialogs
        if (uiState.showMeterReadingDialog) {
            MeterReadingDialog(
                isInitial = uiState.tripStatus == null,
                meterReading = uiState.meterReading,
                onMeterReadingChange = tripsViewModel::updateMeterReading,
                onConfirm = {
                    if (uiState.tripStatus == null) {
                        tripsViewModel.dispatchTrip(uiState.meterReading.toIntOrNull() ?: 0)
                    } else {
                        tripsViewModel.returnTrip(uiState.meterReading.toIntOrNull() ?: 0)
                    }
                },
                onDismiss = tripsViewModel::hideMeterReadingDialog
            )
        }
        
        if (uiState.showDeliveryPhotoDialog) {
            DeliveryPhotoDialog(
                onPhotoSelected = { imageUri ->
                    tripsViewModel.markDelivered(imageUri)
                },
                onDismiss = tripsViewModel::hideDeliveryPhotoDialog
            )
        }
        
        if (uiState.showCancelDialog) {
            CancelTripDialog(
                onConfirm = { tripsViewModel.cancelTrip(driverId) },
                onDismiss = tripsViewModel::hideCancelDialog
            )
        }
        
        if (uiState.showSuccessDialog) {
            SuccessDialog(
                message = uiState.successMessage,
                onDismiss = tripsViewModel::hideSuccessDialog
            )
        }
        
        // Error Snackbar
        uiState.error?.let { error ->
            LaunchedEffect(error) {
                // Show error message
                tripsViewModel.clearError()
            }
        }
    }
}

@Composable
fun OrderDetailsMap(
    order: Order,
    currentLocation: android.location.Location?,
    locationPoints: List<com.pave.driversapp.domain.model.LocationPoint>,
    depotSettings: com.pave.driversapp.domain.model.DepotSettings?
) {
    val defaultLocation = LatLng(20.0, 77.0) // Default to India
    val currentLatLng = currentLocation?.let { LatLng(it.latitude, it.longitude) } ?: defaultLocation
    
    val cameraPositionState = rememberCameraPositionState {
        position = CameraPosition.fromLatLngZoom(currentLatLng, 15f)
    }
    
    // Update camera position when current location changes
    LaunchedEffect(currentLocation) {
        currentLocation?.let { location ->
            val latLng = LatLng(location.latitude, location.longitude)
            cameraPositionState.position = CameraPosition.fromLatLngZoom(latLng, 15f)
            android.util.Log.d("OrderDetailsMap", "📍 Camera updated to current location: ${location.latitude}, ${location.longitude}")
        }
    }
    
    GoogleMap(
        modifier = Modifier
            .fillMaxSize()
            .padding(bottom = 200.dp), // Add padding to avoid overlap with bottom card
        cameraPositionState = cameraPositionState,
        properties = MapProperties(
            isMyLocationEnabled = true
        ),
        uiSettings = MapUiSettings(
            myLocationButtonEnabled = true,
            zoomControlsEnabled = false,
            compassEnabled = true
        )
    ) {
        // Current location marker
        currentLatLng?.let { latLng ->
            Marker(
                state = MarkerState(position = latLng),
                title = "Your Location"
            )
        }
        
        // Depot circle
        depotSettings?.let { depot ->
            Circle(
                center = LatLng(19.97154, 79.23927), // Simplified for now - will fix depot access later
                radius = depot.radius.toDouble(),
                fillColor = Color.Blue.copy(alpha = 0.2f),
                strokeColor = Color.Blue,
                strokeWidth = 2f
            )
        }
        
        // Trip route markers (location dots)
        locationPoints.forEachIndexed { index, point ->
            Marker(
                state = MarkerState(position = LatLng(point.lat, point.lng)),
                title = "Location ${index + 1}",
                snippet = "Tracked at ${point.timestamp?.toDate()?.toString() ?: "Unknown time"}"
            )
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OrderDetailsTopBar(
    order: Order,
    onBack: () -> Unit
) {
    TopAppBar(
        title = {
            Text(
                text = order.clientName,
                color = Color.White,
                fontWeight = FontWeight.Bold
            )
        },
        navigationIcon = {
            IconButton(onClick = onBack) {
                Icon(
                    imageVector = Icons.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = Color.White
                )
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = Color.Black.copy(alpha = 0.8f)
        )
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OrderDetailsBottomCard(
    order: Order,
    uiState: com.pave.driversapp.presentation.viewmodel.TripUiState,
    onDispatch: () -> Unit,
    onDelivered: () -> Unit,
    onReturn: () -> Unit,
    onCancel: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 16.dp, vertical = 16.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF2C2C2C)
        ),
        shape = RoundedCornerShape(topStart = 16.dp, topEnd = 16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 12.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // Trip Status Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = when (uiState.tripStatus) {
                        null -> "Ready to Dispatch"
                        TripStatus.DISPATCHED -> "In Transit"
                        TripStatus.DELIVERED -> "Delivered"
                        TripStatus.RETURNED -> "Completed"
                        TripStatus.CANCELLED -> "Cancelled"
                    },
                    color = Color.White,
                    fontSize = 20.sp,
                    fontWeight = FontWeight.Bold
                )
                
                if (uiState.elapsedTime > 0) {
                    Text(
                        text = "${uiState.elapsedTime}m",
                        color = Color.Gray,
                        fontSize = 14.sp
                    )
                }
            }
            
            // Order Details
            OrderDetailsInfo(order = order)
            
            // Primary Action Button
            TripActionButton(
                tripStatus = uiState.tripStatus,
                isInsideDepot = uiState.isInsideDepot,
                isLoading = uiState.isLoading,
                onDispatch = onDispatch,
                onDelivered = onDelivered,
                onReturn = onReturn
            )
            
            // Secondary Actions (Cancel)
            if (uiState.tripStatus != null && uiState.tripStatus != TripStatus.RETURNED) {
                TextButton(
                    onClick = onCancel,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = "Cancel Trip",
                        color = Color.Red
                    )
                }
            }
        }
    }
}

@Composable
fun OrderDetailsInfo(order: Order) {
    Column(
        verticalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        InfoRow(
            icon = Icons.Filled.Home,
            label = "Vehicle",
            value = order.vehicleNumber,
            iconColor = Color(0xFF4CAF50)
        )
        
        InfoRow(
            icon = Icons.Filled.Build,
            label = "Product",
            value = "${order.productName} (${order.productQuant} units)",
            iconColor = Color(0xFF2196F3)
        )
        
        InfoRow(
            icon = Icons.Filled.LocationOn,
            label = "Address",
            value = "${order.address}, ${order.regionName}",
            iconColor = Color(0xFFFF9800)
        )
        
        InfoRow(
            icon = Icons.Filled.List,
            label = "Time Slot",
            value = "${order.dispatchStart} - ${order.dispatchEnd}",
            iconColor = Color(0xFF9C27B0)
        )
    }
}

@Composable
fun InfoRow(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    value: String,
    iconColor: Color = Color.Gray
) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        Icon(
            imageVector = icon,
            contentDescription = label,
            tint = iconColor,
            modifier = Modifier.size(20.dp)
        )
        
        Column {
            Text(
                text = label,
                color = Color.Gray,
                fontSize = 12.sp
            )
            Text(
                text = value,
                color = Color.White,
                fontSize = 14.sp,
                fontWeight = FontWeight.Medium
            )
        }
    }
}

@Composable
fun TripActionButton(
    tripStatus: TripStatus?,
    isInsideDepot: Boolean,
    isLoading: Boolean,
    onDispatch: () -> Unit,
    onDelivered: () -> Unit,
    onReturn: () -> Unit
) {
    val buttonData = when (tripStatus) {
        null -> {
            val canDispatch = isInsideDepot
            ButtonData(
                text = "Dispatch",
                color = if (canDispatch) Color.Green else Color.Gray,
                onClick = onDispatch,
                enabled = canDispatch
            )
        }
        TripStatus.DISPATCHED -> ButtonData(
            text = "Mark Delivered",
            color = Color(0xFFFF9800), // Orange
            onClick = onDelivered,
            enabled = true
        )
        TripStatus.DELIVERED -> {
            val canReturn = isInsideDepot
            ButtonData(
                text = "Return to Depot",
                color = if (canReturn) Color.Blue else Color.Gray,
                onClick = onReturn,
                enabled = canReturn
            )
        }
        TripStatus.RETURNED -> ButtonData(
            text = "Trip Completed",
            color = Color.Gray,
            onClick = { },
            enabled = false
        )
        TripStatus.CANCELLED -> ButtonData(
            text = "Trip Cancelled",
            color = Color.Red,
            onClick = { },
            enabled = false
        )
    }
    
    Button(
        onClick = buttonData.onClick,
        enabled = buttonData.enabled && !isLoading,
        modifier = Modifier.fillMaxWidth(),
        colors = ButtonDefaults.buttonColors(
            containerColor = buttonData.color,
            disabledContainerColor = Color.Gray
        ),
        shape = RoundedCornerShape(12.dp),
        elevation = ButtonDefaults.buttonElevation(defaultElevation = 4.dp)
    ) {
        if (isLoading) {
            CircularProgressIndicator(
                modifier = Modifier.size(20.dp),
                color = Color.White
            )
        } else {
            Text(
                text = buttonData.text,
                color = Color.White,
                fontWeight = FontWeight.Bold,
                fontSize = 16.sp
            )
        }
    }
}

@Composable
fun MeterReadingDialog(
    isInitial: Boolean,
    meterReading: String,
    onMeterReadingChange: (String) -> Unit,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = if (isInitial) "Initial Meter Reading" else "Final Meter Reading",
                color = Color.White
            )
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                OutlinedTextField(
                    value = meterReading,
                    onValueChange = onMeterReadingChange,
                    label = { Text("Meter Reading (km)", color = Color.Gray) },
                    placeholder = { Text("Enter reading", color = Color.Gray) },
                    keyboardOptions = androidx.compose.foundation.text.KeyboardOptions(
                        keyboardType = androidx.compose.ui.text.input.KeyboardType.Number
                    ),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedTextColor = Color.White,
                        unfocusedTextColor = Color.White,
                        focusedBorderColor = Color.White,
                        unfocusedBorderColor = Color.Gray,
                        focusedLabelColor = Color.Gray,
                        unfocusedLabelColor = Color.Gray
                    ),
                    modifier = Modifier.fillMaxWidth()
                )
                
                if (meterReading.isNotEmpty() && meterReading.toIntOrNull() == null) {
                    Text(
                        text = "Please enter a valid number",
                        color = Color.Red,
                        fontSize = 12.sp
                    )
                } else if (meterReading.isNotEmpty()) {
                    val reading = meterReading.toIntOrNull() ?: 0
                    if (reading < 0) {
                        Text(
                            text = "Meter reading cannot be negative",
                            color = Color.Red,
                            fontSize = 12.sp
                        )
                    } else if (reading > 999999) {
                        Text(
                            text = "Meter reading seems too high",
                            color = Color(0xFFFF9800),
                            fontSize = 12.sp
                        )
                    } else {
                        Text(
                            text = "✓ Valid reading",
                            color = Color(0xFF4CAF50),
                            fontSize = 12.sp
                        )
                    }
                }
            }
        },
        confirmButton = {
            val isValid = meterReading.isNotEmpty() && 
                         meterReading.toIntOrNull() != null && 
                         meterReading.toIntOrNull()!! >= 0 &&
                         meterReading.toIntOrNull()!! <= 999999
            
            TextButton(
                onClick = onConfirm,
                enabled = isValid
            ) {
                Text(
                    text = if (isInitial) "Dispatch Trip" else "Complete Trip",
                    color = if (isValid) Color.White else Color.Gray
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = Color.Gray)
            }
        },
        containerColor = Color(0xFF1E1E1E)
    )
}

@Composable
fun DeliveryPhotoDialog(
    onPhotoSelected: (String) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("Delivery Photo", color = Color.White)
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Text(
                    text = "Please take a photo of the delivered goods",
                    color = Color.Gray
                )
                
                // Photo capture button
                Button(
                    onClick = { 
                        // For now, simulate photo capture with a placeholder
                        // In a real implementation, this would launch the camera
                        onPhotoSelected("delivery_photo_${System.currentTimeMillis()}.jpg")
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color(0xFF2196F3)
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Icon(
                        imageVector = Icons.Filled.Add,
                        contentDescription = "Take Photo",
                        tint = Color.White
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Take Photo", color = Color.White, fontWeight = FontWeight.Medium)
                }
                
                Text(
                    text = "Note: Camera integration will be added in future updates",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.Gray,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel", color = Color.Gray)
            }
        },
        containerColor = Color(0xFF1E1E1E)
    )
}

@Composable
fun CancelTripDialog(
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text("Cancel Trip", color = Color.White)
        },
        text = {
            Column(
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                Text(
                    text = "Are you sure you want to cancel this trip?",
                    color = Color.Gray,
                    fontSize = 16.sp
                )
                
                Text(
                    text = "⚠️ This action cannot be undone",
                    color = Color(0xFFFF9800),
                    fontSize = 14.sp,
                    fontWeight = FontWeight.Medium
                )
                
                Text(
                    text = "• Location tracking will stop\n• Trip data will be marked as cancelled\n• You'll need to create a new trip",
                    color = Color.Gray,
                    fontSize = 14.sp
                )
            }
        },
        confirmButton = {
            TextButton(onClick = onConfirm) {
                Text("Cancel Trip", color = Color.Red)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Keep Trip", color = Color.Gray)
            }
        },
        containerColor = Color(0xFF1E1E1E)
    )
}

@Composable
fun SuccessDialog(
    message: String,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.CheckCircle,
                    contentDescription = "Success",
                    tint = Color(0xFF4CAF50),
                    modifier = Modifier.size(24.dp)
                )
                Text("Success", color = Color.White, fontWeight = FontWeight.Bold)
            }
        },
        text = {
            Text(
                text = message,
                color = Color.Gray,
                fontSize = 16.sp
            )
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text("OK", color = Color(0xFF4CAF50), fontWeight = FontWeight.Medium)
            }
        },
        containerColor = Color(0xFF1E1E1E)
    )
}

private fun getTripStatusText(tripStatus: TripStatus?): String {
    return when (tripStatus) {
        null -> "Ready to Dispatch"
        TripStatus.DISPATCHED -> "In Progress"
        TripStatus.DELIVERED -> "Delivered"
        TripStatus.RETURNED -> "Completed"
        TripStatus.CANCELLED -> "Cancelled"
    }
}

data class ButtonData(
    val text: String,
    val color: Color,
    val onClick: () -> Unit,
    val enabled: Boolean
)
