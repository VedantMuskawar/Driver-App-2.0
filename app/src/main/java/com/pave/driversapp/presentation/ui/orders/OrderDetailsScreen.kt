package com.pave.driversapp.presentation.ui.orders

import androidx.compose.animation.*
import androidx.compose.animation.core.*
import androidx.compose.foundation.background
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.layout.systemBarsPadding
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
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import android.Manifest
import android.content.pm.PackageManager
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.android.gms.maps.model.Dash
import com.google.android.gms.maps.model.Gap
import com.google.maps.android.compose.*
import com.pave.driversapp.domain.model.Order
import com.pave.driversapp.domain.model.TripStatus
import com.pave.driversapp.data.repository.DepotRepositoryImpl
import com.pave.driversapp.domain.repository.TripsRepositoryImpl
import com.pave.driversapp.domain.repository.OrdersRepositoryImpl
import com.pave.driversapp.domain.model.ScheduledOrder
import com.pave.driversapp.presentation.viewmodel.TripsViewModel
import com.google.firebase.firestore.FirebaseFirestore
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.pave.driversapp.util.SafeLaunchedEffect
import kotlinx.coroutines.launch

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
    
    SafeLaunchedEffect(orgId) {
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
    SafeLaunchedEffect(order) {
        tripsViewModel.initialize(order, driverId, orgId)
    }
    
    // Force location update when screen opens
    SafeLaunchedEffect(Unit) {
        tripsViewModel.startLocationUpdates()
    }
    
    // Animation states
    val mapAlpha by animateFloatAsState(
        targetValue = if (uiState.currentLocation != null) 1f else 0.7f,
        animationSpec = tween(800, easing = EaseInOutCubic),
        label = "mapAlpha"
    )
    
    val mapScale by animateFloatAsState(
        targetValue = if (uiState.currentLocation != null) 1f else 0.95f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessLow
        ),
        label = "mapScale"
    )
    
    val bottomCardOffset by animateDpAsState(
        targetValue = if (uiState.showMeterReadingDialog || uiState.showDeliveryPhotoDialog || uiState.showCancelDialog || uiState.showSuccessDialog) 0.dp else 0.dp,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "bottomCardOffset"
    )

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .systemBarsPadding() // Add padding for mobile navigation bar
    ) {
        // Map Background with animations
        Box(
            modifier = Modifier
                .fillMaxSize()
                .alpha(mapAlpha)
                .scale(mapScale)
        ) {
            OrderDetailsMap(
                order = order,
                currentLocation = uiState.currentLocation,
                locationPoints = uiState.locationPoints,
                depotSettings = depotSettings,
                username = "Ramesh Dingalwar" // Use the username from the image as example
            )
        }
        
        // Top App Bar with slide animation
        AnimatedVisibility(
            visible = true,
            enter = slideInVertically(
                initialOffsetY = { -it },
                animationSpec = tween(600, easing = EaseOutCubic)
            ),
            exit = slideOutVertically(
                targetOffsetY = { -it },
                animationSpec = tween(300)
            )
        ) {
            OrderDetailsTopBar(
                order = order,
                onBack = onBack
            )
        }
        
        // Bottom Action Card - positioned at bottom with slide animation
        AnimatedVisibility(
            visible = true,
            enter = slideInVertically(
                initialOffsetY = { it },
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessLow
                )
            ),
            exit = slideOutVertically(
                targetOffsetY = { it },
                animationSpec = tween(400)
            ),
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 16.dp) // Add padding from mobile navigation bar
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
        
        // Dialogs with animations
        AnimatedVisibility(
            visible = uiState.showMeterReadingDialog,
            enter = fadeIn(animationSpec = tween(300)) + scaleIn(
                initialScale = 0.8f,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessMedium
                )
            ),
            exit = fadeOut(animationSpec = tween(200)) + scaleOut(
                targetScale = 0.8f,
                animationSpec = tween(200)
            )
        ) {
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
        
        AnimatedVisibility(
            visible = uiState.showDeliveryPhotoDialog,
            enter = fadeIn(animationSpec = tween(300)) + scaleIn(
                initialScale = 0.8f,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessMedium
                )
            ),
            exit = fadeOut(animationSpec = tween(200)) + scaleOut(
                targetScale = 0.8f,
                animationSpec = tween(200)
            )
        ) {
            DeliveryPhotoDialog(
                onPhotoSelected = { imageUri ->
                    tripsViewModel.markDelivered(imageUri)
                },
                onDismiss = tripsViewModel::hideDeliveryPhotoDialog
            )
        }
        
        AnimatedVisibility(
            visible = uiState.showCancelDialog,
            enter = fadeIn(animationSpec = tween(300)) + scaleIn(
                initialScale = 0.8f,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessMedium
                )
            ),
            exit = fadeOut(animationSpec = tween(200)) + scaleOut(
                targetScale = 0.8f,
                animationSpec = tween(200)
            )
        ) {
            CancelTripDialog(
                onConfirm = { tripsViewModel.cancelTrip(driverId) },
                onDismiss = tripsViewModel::hideCancelDialog
            )
        }
        
        AnimatedVisibility(
            visible = uiState.showSuccessDialog,
            enter = fadeIn(animationSpec = tween(300)) + scaleIn(
                initialScale = 0.8f,
                animationSpec = spring(
                    dampingRatio = Spring.DampingRatioMediumBouncy,
                    stiffness = Spring.StiffnessMedium
                )
            ),
            exit = fadeOut(animationSpec = tween(200)) + scaleOut(
                targetScale = 0.8f,
                animationSpec = tween(200)
            )
        ) {
            SuccessDialog(
                message = uiState.successMessage,
                onDismiss = tripsViewModel::hideSuccessDialog
            )
        }
        
        // Error Snackbar
        uiState.error?.let { error ->
            SafeLaunchedEffect(error) {
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
    depotSettings: com.pave.driversapp.domain.model.DepotSettings?,
    username: String = "Driver" // Add username parameter
) {
    val context = LocalContext.current
    
    // Check location permissions
    val hasLocationPermission = remember {
        ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_FINE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED ||
        ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.ACCESS_COARSE_LOCATION
        ) == PackageManager.PERMISSION_GRANTED
    }
    
    // Debug logging for location permission
    android.util.Log.d("OrderDetailsMap", "🔐 Location permission check: $hasLocationPermission")
    android.util.Log.d("OrderDetailsMap", "📍 Current location: ${currentLocation?.latitude}, ${currentLocation?.longitude}")
    android.util.Log.d("OrderDetailsMap", "📍 Current location accuracy: ${currentLocation?.accuracy}")
    
    // Always prioritize current location - wait for it if not available
    val cameraPositionState = rememberCameraPositionState {
        // Start with depot location as initial position
        position = CameraPosition.fromLatLngZoom(
            LatLng(19.97154, 79.23927), // Depot coordinates
            15f
        )
    }
    
    // Update camera position when current location becomes available
    SafeLaunchedEffect(currentLocation) {
        if (currentLocation != null) {
            val latLng = LatLng(currentLocation.latitude, currentLocation.longitude)
            cameraPositionState.position = CameraPosition.fromLatLngZoom(latLng, 16f) // Higher zoom for better detail
            android.util.Log.d("OrderDetailsMap", "📍 Camera updated to current location: ${currentLocation.latitude}, ${currentLocation.longitude}")
        } else {
            android.util.Log.d("OrderDetailsMap", "⏳ Waiting for current location...")
        }
    }
    
    // Current location for markers
    val currentLatLng = currentLocation?.let { LatLng(it.latitude, it.longitude) }
    android.util.Log.d("OrderDetailsMap", "📍 Current LatLng: $currentLatLng")
    
    GoogleMap(
        modifier = Modifier
            .fillMaxSize()
            .padding(bottom = 220.dp), // Increased padding to account for system bars
        cameraPositionState = cameraPositionState,
        properties = MapProperties(
            isMyLocationEnabled = true // Always enable - let Google Maps handle permissions
        ),
        uiSettings = MapUiSettings(
            myLocationButtonEnabled = true, // Always enable recenter button
            zoomControlsEnabled = true, // Enable zoom controls for debugging
            compassEnabled = true,
            mapToolbarEnabled = true // Enable map toolbar
        )
    ) {
        // Current location marker - always show when available
        currentLatLng?.let { latLng ->
            android.util.Log.d("OrderDetailsMap", "🎯 Rendering user location marker at: ${latLng.latitude}, ${latLng.longitude}")
            
            // Add a prominent circle around the marker like in the image
            Circle(
                center = latLng,
                radius = 100.0, // Larger radius for better visibility
                fillColor = Color.Blue.copy(alpha = 0.15f),
                strokeColor = Color.Blue.copy(alpha = 0.4f),
                strokeWidth = 3f
            )
            
            // The actual marker with username
            Marker(
                state = MarkerState(position = latLng),
                title = username, // Use username as title
                snippet = "Current Position"
            )
        }
        
        // Show a fallback marker if no current location but we have location permission
        if (currentLatLng == null && hasLocationPermission) {
            android.util.Log.d("OrderDetailsMap", "🎯 Rendering fallback depot marker")
            // Show depot location as fallback
            val fallbackLocation = LatLng(19.97154, 79.23927)
            Circle(
                center = fallbackLocation,
                radius = 50.0,
                fillColor = Color.Gray.copy(alpha = 0.1f),
                strokeColor = Color.Gray.copy(alpha = 0.3f),
                strokeWidth = 2f
            )
            Marker(
                state = MarkerState(position = fallbackLocation),
                title = "Depot Location",
                snippet = "Waiting for GPS..."
            )
        }
        
        // Debug: Always show a test marker to verify markers are working
        val testLocation = LatLng(19.97154, 79.23927)
        android.util.Log.d("OrderDetailsMap", "🎯 Rendering test marker at depot location")
        Circle(
            center = testLocation,
            radius = 30.0,
            fillColor = Color.Red.copy(alpha = 0.2f),
            strokeColor = Color.Red.copy(alpha = 0.5f),
            strokeWidth = 2f
        )
        Marker(
            state = MarkerState(position = testLocation),
            title = "Test Marker",
            snippet = "Depot Location - Always Visible"
        )
        
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
        
        // Trip route polylines (connect location points)
        if (locationPoints.size > 1) {
            val routePoints = locationPoints.map { LatLng(it.lat, it.lng) }
            Polyline(
                points = routePoints,
                color = Color.Blue,
                width = 4f,
                pattern = listOf(Dash(10f), Gap(5f))
            )
        }
        
        // Small dots for location points - clean and minimal
        locationPoints.forEach { point ->
            Circle(
                center = LatLng(point.lat, point.lng),
                radius = 3.0, // Small radius for minimal visual impact
                fillColor = Color.Red.copy(alpha = 0.6f), // Semi-transparent red
                strokeColor = Color.Red.copy(alpha = 0.8f), // Slightly more opaque border
                strokeWidth = 1f
            )
        }
    }
    
    // Custom Recenter Button - Floating Action Button
    FloatingActionButton(
        onClick = {
            // Recenter to current location or depot
            val targetLocation = currentLatLng ?: LatLng(19.97154, 79.23927)
            cameraPositionState.position = CameraPosition.fromLatLngZoom(targetLocation, 16f)
            android.util.Log.d("OrderDetailsMap", "🔄 Custom recenter button clicked - recentering to: ${targetLocation.latitude}, ${targetLocation.longitude}")
        },
        modifier = Modifier
            .padding(start = 16.dp, top = 80.dp), // Add top padding to move below header
        containerColor = Color.White,
        contentColor = Color.Blue
    ) {
        Icon(
            imageVector = Icons.Filled.LocationOn,
            contentDescription = "Recenter to my location",
            tint = Color.Blue
        )
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
                fontWeight = FontWeight.Bold,
                fontSize = 18.sp
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
        actions = {
            IconButton(onClick = { /* Compass/Orientation functionality */ }) {
                Icon(
                    imageVector = Icons.Filled.Settings,
                    contentDescription = "Settings",
                    tint = Color.White
                )
            }
        },
        colors = TopAppBarDefaults.topAppBarColors(
            containerColor = Color(0xFF2C2C2C)
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
            .padding(horizontal = 16.dp, vertical = 8.dp),
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF1E1E1E)
        ),
        shape = RoundedCornerShape(20.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
        border = BorderStroke(
            width = 1.dp,
            color = Color(0xFF2C2C2C)
        )
    ) {
        Column(
            modifier = Modifier.padding(24.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // Trip Status Header with enhanced styling
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                val statusText = when {
                    order.deliveryStatus -> "Order Delivered"
                    order.dispatchStatus && !order.deliveryStatus -> "In Transit"
                    uiState.tripStatus == TripStatus.DISPATCHED -> "In Transit"
                    uiState.tripStatus == TripStatus.DELIVERED -> "Delivered"
                    uiState.tripStatus == TripStatus.RETURNED -> "Completed"
                    uiState.tripStatus == TripStatus.CANCELLED -> "Cancelled"
                    else -> "Ready to Dispatch"
                }
                
                val statusColor = when {
                    order.deliveryStatus -> Color(0xFF38A169)
                    order.dispatchStatus && !order.deliveryStatus -> Color(0xFFFF9800)
                    uiState.tripStatus == TripStatus.DISPATCHED -> Color(0xFF2196F3)
                    uiState.tripStatus == TripStatus.DELIVERED -> Color(0xFF38A169)
                    uiState.tripStatus == TripStatus.RETURNED -> Color(0xFF9C27B0)
                    uiState.tripStatus == TripStatus.CANCELLED -> Color(0xFFE53E3E)
                    else -> Color(0xFF6B7280)
                }
                
                Row(
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        modifier = Modifier
                            .size(8.dp)
                            .background(statusColor, RoundedCornerShape(4.dp))
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = statusText,
                        color = Color.White,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
                
                if (uiState.elapsedTime > 0) {
                    Text(
                        text = "${uiState.elapsedTime}m",
                        color = Color.Gray,
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Medium
                    )
                }
            }
            
            // Order Details with enhanced layout
            OrderDetailsInfo(order = order)
            
            // Primary Action Button
            TripActionButton(
                tripStatus = uiState.tripStatus,
                order = order,
                isInsideDepot = uiState.isInsideDepot,
                isLoading = uiState.isLoading,
                onDispatch = onDispatch,
                onDelivered = onDelivered,
                onReturn = onReturn
            )
            
            // Secondary Actions
            if (!order.deliveryStatus && uiState.tripStatus != null && uiState.tripStatus != TripStatus.RETURNED) {
                OutlinedButton(
                    onClick = onCancel,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(48.dp),
                    colors = ButtonDefaults.outlinedButtonColors(
                        contentColor = Color(0xFFE53E3E),
                        containerColor = Color.Transparent
                    ),
                    border = androidx.compose.foundation.BorderStroke(
                        width = 1.dp,
                        color = Color(0xFFE53E3E)
                    ),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    Text(
                        text = "Cancel Trip",
                        fontWeight = FontWeight.Medium
                    )
                }
            }
        }
    }
}

@Composable
fun OrderDetailsInfo(order: Order) {
    Column(
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        InfoRow(
            icon = Icons.Filled.Home,
            label = "Vehicle",
            value = order.vehicleNumber,
            iconColor = Color(0xFF4CAF50)
        )
        
        InfoRow(
            icon = Icons.Filled.LocationOn,
            label = "Address",
            value = "${order.address}, ${order.regionName}",
            iconColor = Color(0xFFFF9800)
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
        horizontalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Card(
            colors = CardDefaults.cardColors(
                containerColor = iconColor.copy(alpha = 0.15f)
            ),
            shape = RoundedCornerShape(10.dp)
        ) {
            Icon(
                imageVector = icon,
                contentDescription = label,
                tint = iconColor,
                modifier = Modifier
                    .size(24.dp)
                    .padding(8.dp)
            )
        }
        
        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = label,
                color = Color.Gray,
                fontSize = 12.sp,
                fontWeight = FontWeight.Medium
            )
            Text(
                text = value,
                color = Color.White,
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 2
            )
        }
    }
}

@Composable
fun TripActionButton(
    tripStatus: TripStatus?,
    order: Order, // Add order parameter to check actual order status
    isInsideDepot: Boolean,
    isLoading: Boolean,
    onDispatch: () -> Unit,
    onDelivered: () -> Unit,
    onReturn: () -> Unit
) {
    val buttonData = when {
        // If order is already delivered, show appropriate status
        order.deliveryStatus -> {
            ButtonData(
                text = "Order Delivered",
                color = Color(0xFF4CAF50), // Green
                onClick = { },
                enabled = false
            )
        }
        // If order is already dispatched but not delivered
        order.dispatchStatus && !order.deliveryStatus -> {
            ButtonData(
                text = "Mark Delivered",
                color = Color(0xFF2196F3), // Blue for deliver
                onClick = onDelivered,
                enabled = true
            )
        }
        // If trip is in progress (dispatched)
        tripStatus == TripStatus.DISPATCHED -> {
            ButtonData(
                text = "Mark Delivered",
                color = Color(0xFF2196F3), // Blue for deliver
                onClick = onDelivered,
                enabled = true
            )
        }
        // If trip is delivered
        tripStatus == TripStatus.DELIVERED -> {
            val canReturn = isInsideDepot
            ButtonData(
                text = "Return to Depot",
                color = if (canReturn) Color(0xFFFF9800) else Color.Gray, // Orange for return
                onClick = onReturn,
                enabled = canReturn
            )
        }
        // If trip is returned
        tripStatus == TripStatus.RETURNED -> {
            ButtonData(
                text = "Trip Completed",
                color = Color.Gray,
                onClick = { },
                enabled = false
            )
        }
        // If trip is cancelled
        tripStatus == TripStatus.CANCELLED -> {
            ButtonData(
                text = "Trip Cancelled",
                color = Color(0xFFF44336), // Red for Cancel
                onClick = { },
                enabled = false
            )
        }
        // Default case: order is ready to dispatch (not dispatched yet)
        else -> {
            val canDispatch = isInsideDepot && !order.dispatchStatus
            ButtonData(
                text = "Dispatch",
                color = if (canDispatch) Color(0xFF4CAF50) else Color.Gray, // Green for dispatch
                onClick = onDispatch,
                enabled = canDispatch
            )
        }
    }
    
    // Animation for button state changes
    val buttonScale by animateFloatAsState(
        targetValue = if (buttonData.enabled && !isLoading) 1f else 0.95f,
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "buttonScale"
    )
    
    val buttonAlpha by animateFloatAsState(
        targetValue = if (buttonData.enabled && !isLoading) 1f else 0.7f,
        animationSpec = tween(300),
        label = "buttonAlpha"
    )
    
    Button(
        onClick = buttonData.onClick,
        enabled = buttonData.enabled && !isLoading,
        modifier = Modifier
            .fillMaxWidth()
            .height(48.dp)
            .scale(buttonScale)
            .alpha(buttonAlpha),
        colors = ButtonDefaults.buttonColors(
            containerColor = buttonData.color,
            disabledContainerColor = Color.Gray.copy(alpha = 0.3f)
        ),
        shape = RoundedCornerShape(14.dp),
        elevation = ButtonDefaults.buttonElevation(
            defaultElevation = if (buttonData.enabled && !isLoading) 8.dp else 0.dp
        )
    ) {
        if (isLoading) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                CircularProgressIndicator(
                    modifier = Modifier.size(20.dp),
                    color = Color.White,
                    strokeWidth = 2.dp
                )
                Text(
                    text = "Processing...",
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 16.sp
                )
            }
        } else {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                when (buttonData.text) {
                    "Dispatch", "Order Ready for Dispatch" -> {
                        Icon(
                            imageVector = Icons.Filled.PlayArrow,
                            contentDescription = "Dispatch",
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            text = buttonData.text,
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                    }
                    "Mark Delivered", "Order Delivered" -> {
                        Icon(
                            imageVector = Icons.Filled.CheckCircle,
                            contentDescription = "Delivered",
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            text = buttonData.text,
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                    }
                    "Return to Depot", "Trip Completed" -> {
                        Icon(
                            imageVector = Icons.Filled.Home,
                            contentDescription = "Return",
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                        Text(
                            text = buttonData.text,
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                    }
                    else -> Icon(
                        imageVector = Icons.Filled.Close,
                        contentDescription = "Cancel",
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }
            }
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

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OrderDetailsScreenWithData(
    orderId: String,
    orgId: String,
    driverId: String,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val firestore = FirebaseFirestore.getInstance()
    val fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)
    
    val tripsRepository = remember { TripsRepositoryImpl(firestore, context) }
    val depotRepository = remember { DepotRepositoryImpl(firestore, context) }
    val ordersRepository = remember { OrdersRepositoryImpl() }
    
    // State for the fetched order
    var order by remember { mutableStateOf<Order?>(null) }
    var isLoading by remember { mutableStateOf(true) }
    var error by remember { mutableStateOf<String?>(null) }
    
    // Fetch order data from SCH_ORDERS
    SafeLaunchedEffect(orderId) {
        try {
            android.util.Log.d("OrderDetailsScreenWithData", "🔄 Fetching order data for orderId: $orderId")
            
            // First try to get as ScheduledOrder
            val scheduledOrder = ordersRepository.getScheduledOrderById(orderId)
            if (scheduledOrder != null) {
                android.util.Log.d("OrderDetailsScreenWithData", "✅ Found ScheduledOrder: ${scheduledOrder.clientName}")
                
                // Convert ScheduledOrder to Order format
                order = Order(
                    orderId = scheduledOrder.orderId,
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
                isLoading = false
            } else {
                // Fallback: try to get as regular Order
                val regularOrder = ordersRepository.getOrderById(orderId)
                if (regularOrder != null) {
                    android.util.Log.d("OrderDetailsScreenWithData", "✅ Found regular Order: ${regularOrder.clientName}")
                    order = regularOrder
                    isLoading = false
                } else {
                    android.util.Log.e("OrderDetailsScreenWithData", "❌ Order not found: $orderId")
                    error = "Order not found"
                    isLoading = false
                }
            }
        } catch (e: Exception) {
            android.util.Log.e("OrderDetailsScreenWithData", "💥 Error fetching order: ${e.message}")
            error = "Error loading order: ${e.message}"
            isLoading = false
        }
    }
    
    // Show loading state
    if (isLoading) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                CircularProgressIndicator(color = Color.White)
                Text(
                    text = "Loading order details...",
                    color = Color.White,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }
    // Show error state
    else if (error != null) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(Color.Black),
            contentAlignment = Alignment.Center
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Icon(
                    imageVector = Icons.Filled.Warning,
                    contentDescription = "Error",
                    tint = Color.Red,
                    modifier = Modifier.size(64.dp)
                )
                Text(
                    text = error ?: "Unknown error",
                    color = Color.White,
                    style = MaterialTheme.typography.bodyMedium,
                    textAlign = androidx.compose.ui.text.style.TextAlign.Center
                )
                Button(
                    onClick = onBack,
                    colors = ButtonDefaults.buttonColors(containerColor = Color.Blue)
                ) {
                    Text("Go Back", color = Color.White)
                }
            }
        }
    }
    // Show order details
    else if (order != null) {
        OrderDetailsScreen(
            order = order!!,
            onBack = onBack,
            driverId = driverId,
            orgId = orgId
        )
    }
}

data class ButtonData(
    val text: String,
    val color: Color,
    val onClick: () -> Unit,
    val enabled: Boolean
)
