package com.pave.driversapp.presentation.ui.depot

import android.Manifest
import android.content.pm.PackageManager
import android.location.Location
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices
import com.google.android.gms.maps.model.CameraPosition
import com.google.android.gms.maps.model.LatLng
import com.google.maps.android.compose.*
import com.pave.driversapp.domain.model.DepotLocation
import com.pave.driversapp.presentation.viewmodel.DepotViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun DepotSetupScreen(
    orgId: String,
    orgName: String,
    userId: String,
    depotViewModel: DepotViewModel,
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val uiState by depotViewModel.uiState.collectAsStateWithLifecycle()
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()
    
    var selectedLocation by remember { mutableStateOf<LatLng?>(null) }
    var radius by remember { mutableStateOf("100") }
    var hasLocationPermission by remember { 
        mutableStateOf(
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED
        )
    }
    var isLoadingLocation by remember { mutableStateOf(false) }
    
    val fusedLocationClient: FusedLocationProviderClient = remember {
        LocationServices.getFusedLocationProviderClient(context)
    }
    
    val requestPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        hasLocationPermission = permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
                               permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true
        if (hasLocationPermission) {
            Toast.makeText(context, "Location permission granted", Toast.LENGTH_SHORT).show()
            // Get current location immediately after permission is granted
            getCurrentLocationAndSetDepot(fusedLocationClient) { location ->
                selectedLocation = LatLng(location.latitude, location.longitude)
            }
        } else {
            Toast.makeText(context, "Location permission denied. Cannot set depot location.", Toast.LENGTH_LONG).show()
        }
    }
    
    LaunchedEffect(Unit) {
        depotViewModel.loadDepotSettings(orgId)
        if (ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_FINE_LOCATION) != PackageManager.PERMISSION_GRANTED ||
            ContextCompat.checkSelfPermission(context, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) {
            requestPermissionLauncher.launch(
                arrayOf(
                    Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION
                )
            )
        } else {
            // If permissions are already granted, get current location immediately
            getCurrentLocationAndSetDepot(fusedLocationClient) { location ->
                selectedLocation = LatLng(location.latitude, location.longitude)
            }
        }
    }
    
    LaunchedEffect(uiState.errorMessage) {
        uiState.errorMessage?.let { message ->
            snackbarHostState.showSnackbar(message)
            depotViewModel.clearError()
        }
    }
    
    LaunchedEffect(uiState.depotSaved) {
        if (uiState.depotSaved) {
            val message = if (uiState.hasDepotConfigured) {
                if (uiState.depotSettings == null) {
                    "Depot deleted successfully!"
                } else {
                    "Depot updated successfully!"
                }
            } else {
                "Depot saved successfully!"
            }
            snackbarHostState.showSnackbar(message)
            depotViewModel.resetDepotSaved()
        }
    }
    
    // Load existing depot settings
    LaunchedEffect(uiState.depotSettings) {
        uiState.depotSettings?.let { depotSettings ->
            selectedLocation = LatLng(depotSettings.depotLocation.lat, depotSettings.depotLocation.lng)
            radius = depotSettings.radius.toString()
        }
    }
    
    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            TopAppBar(
                title = { Text("Depot Setup", color = Color.White) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.Filled.ArrowBack, "Back", tint = Color.White)
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Black
                )
            )
        },
        containerColor = Color.Black
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            Text(
                text = if (uiState.hasDepotConfigured) "Edit Depot Location and Radius" else "Configure Depot Location and Radius",
                style = MaterialTheme.typography.headlineSmall,
                color = Color.White
            )

            Spacer(modifier = Modifier.height(16.dp))

            // Permission request
            if (!hasLocationPermission) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.errorContainer)
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp)
                    ) {
                        Text(
                            text = "Location Permission Required",
                            style = MaterialTheme.typography.titleMedium,
                            color = Color.White
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Please grant location permission to set depot location",
                            color = Color.White
                        )
                        Spacer(modifier = Modifier.height(16.dp))
                        Button(
                            onClick = { 
                                requestPermissionLauncher.launch(
                                    arrayOf(
                                        Manifest.permission.ACCESS_FINE_LOCATION,
                                        Manifest.permission.ACCESS_COARSE_LOCATION
                                    )
                                )
                            }
                        ) {
                            Text("Grant Permission")
                        }
                    }
                }
            }

            // Map
            if (hasLocationPermission) {
                // Map container with recenter button
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(300.dp)
                ) {
                    Card(
                        modifier = Modifier.fillMaxSize()
                    ) {
                        val cameraPositionState = rememberCameraPositionState {
                            position = CameraPosition.fromLatLngZoom(
                                selectedLocation ?: LatLng(37.7749, -122.4194), // Default to San Francisco
                                15f
                            )
                        }

                        // Update camera position when location changes
                        LaunchedEffect(selectedLocation, uiState.depotSettings) {
                            val locationToCenter = selectedLocation ?: uiState.depotSettings?.let { 
                                LatLng(it.depotLocation.lat, it.depotLocation.lng) 
                            }
                            locationToCenter?.let { location ->
                                cameraPositionState.position = CameraPosition.fromLatLngZoom(location, 15f)
                                android.util.Log.d("DepotSetup", "📷 Camera centered on: ${location.latitude}, ${location.longitude}")
                            }
                        }

                        GoogleMap(
                            modifier = Modifier.fillMaxSize(),
                            cameraPositionState = cameraPositionState,
                            onMapLongClick = { latLng ->
                                selectedLocation = latLng
                                android.util.Log.d("DepotSetup", "📍 Depot location selected: ${latLng.latitude}, ${latLng.longitude}")
                            },
                            onMapLoaded = {
                                android.util.Log.d("DepotSetup", "🗺️ Map loaded successfully")
                                // If we have depot settings, ensure the marker is visible
                                uiState.depotSettings?.let { depotSettings ->
                                    selectedLocation = LatLng(depotSettings.depotLocation.lat, depotSettings.depotLocation.lng)
                                    android.util.Log.d("DepotSetup", "📍 Setting depot marker from saved settings: ${depotSettings.depotLocation.lat}, ${depotSettings.depotLocation.lng}")
                                }
                            },
                            properties = MapProperties(
                                isMyLocationEnabled = true
                            ),
                            uiSettings = MapUiSettings(
                                myLocationButtonEnabled = true
                            )
                        ) {
                            // Show marker for selected location or existing depot
                            val locationToShow = selectedLocation ?: uiState.depotSettings?.let { 
                                LatLng(it.depotLocation.lat, it.depotLocation.lng) 
                            }
                            
                            locationToShow?.let { location ->
                                Marker(
                                    state = MarkerState(position = location),
                                    title = if (uiState.hasDepotConfigured) orgName else "Depot Location"
                                )
                                
                                // Show radius circle
                                Circle(
                                    center = location,
                                    radius = radius.toDoubleOrNull() ?: 100.0,
                                    fillColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.2f),
                                    strokeColor = MaterialTheme.colorScheme.primary,
                                    strokeWidth = 2f
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))

                Text(
                    text = "Long press on map to set depot location",
                    style = MaterialTheme.typography.bodySmall,
                    color = Color.White
                )

                Spacer(modifier = Modifier.height(16.dp))
            }

            // Radius input
            OutlinedTextField(
                value = radius,
                onValueChange = { radius = it },
                label = { Text("Radius (meters)", color = Color.White) },
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                modifier = Modifier.fillMaxWidth(),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedTextColor = Color.White,
                    unfocusedTextColor = Color.White,
                    focusedLabelColor = Color.White,
                    unfocusedLabelColor = Color.White,
                    focusedBorderColor = Color.White,
                    unfocusedBorderColor = Color.White
                )
            )

                Button(
                    onClick = {
                        if (hasLocationPermission) {
                            isLoadingLocation = true
                            getCurrentLocationAndSetDepot(fusedLocationClient) { location ->
                                selectedLocation = LatLng(location.latitude, location.longitude)
                                Toast.makeText(context, "Current location set as ${orgName} depot", Toast.LENGTH_SHORT).show()
                                isLoadingLocation = false
                            }
                        } else {
                            Toast.makeText(context, "Location permissions not granted. Cannot get current location.", Toast.LENGTH_LONG).show()
                            requestPermissionLauncher.launch(
                                arrayOf(
                                    Manifest.permission.ACCESS_FINE_LOCATION,
                                    Manifest.permission.ACCESS_COARSE_LOCATION
                                )
                            )
                        }
                    },
                    modifier = Modifier.fillMaxWidth(),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.White,
                        contentColor = Color.Black
                    )
                ) {
                    if (isLoadingLocation) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp), color = Color.Black)
                    } else {
                        Text("Set Current Location as ${orgName} Depot", color = Color.Black)
                    }
                }

            // Action buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                Button(
                    onClick = {
                        val lat = selectedLocation?.latitude
                        val lng = selectedLocation?.longitude
                        val radiusInt = radius.toIntOrNull()

                        if (lat != null && lng != null && radiusInt != null && radiusInt > 0) {
                            depotViewModel.setDepotLocation(
                                orgId = orgId,
                                depotLocation = DepotLocation(lat, lng),
                                radius = radiusInt,
                                createdBy = userId
                            )
                        } else {
                            coroutineScope.launch {
                                snackbarHostState.showSnackbar("Please select a depot location and enter a positive radius.")
                            }
                        }
                    },
                    enabled = !uiState.isLoading && selectedLocation != null,
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = Color.White,
                        contentColor = Color.Black
                    )
                ) {
                    if (uiState.isLoading) {
                        CircularProgressIndicator(modifier = Modifier.size(24.dp), color = Color.Black)
                    } else {
                        Text(if (uiState.hasDepotConfigured) "Update Depot" else "Save Depot", color = Color.Black)
                    }
                }
                
                // Delete button (only show if depot exists)
                if (uiState.hasDepotConfigured) {
                    Button(
                        onClick = {
                            depotViewModel.deleteDepot(orgId)
                        },
                        enabled = !uiState.isLoading,
                        colors = ButtonDefaults.buttonColors(
                            containerColor = Color.Red,
                            contentColor = Color.White
                        ),
                        modifier = Modifier.weight(1f)
                    ) {
                        if (uiState.isLoading) {
                            CircularProgressIndicator(
                                modifier = Modifier.size(24.dp),
                                color = Color.White
                            )
                        } else {
                            Text("Delete Depot", color = Color.White)
                        }
                    }
                }
            }
        }
    }
}

private fun getCurrentLocationAndSetDepot(
    fusedLocationClient: FusedLocationProviderClient,
    onLocationReceived: (Location) -> Unit
) {
    try {
        fusedLocationClient.lastLocation
            .addOnSuccessListener { location: Location? ->
                if (location != null) {
                    onLocationReceived(location)
                } else {
                    // If last location is null, request a fresh one
                    val locationRequest = com.google.android.gms.location.LocationRequest.Builder(
                        com.google.android.gms.location.Priority.PRIORITY_HIGH_ACCURACY,
                        10000
                    ).build()
                    
                    val locationCallback = object : com.google.android.gms.location.LocationCallback() {
                        override fun onLocationResult(locationResult: com.google.android.gms.location.LocationResult) {
                            locationResult.lastLocation?.let { newLocation ->
                                fusedLocationClient.removeLocationUpdates(this)
                                onLocationReceived(newLocation)
                            }
                        }
                    }
                    
                    fusedLocationClient.requestLocationUpdates(
                        locationRequest,
                        locationCallback,
                        android.os.Looper.getMainLooper()
                    )
                }
            }
            .addOnFailureListener { e ->
                android.util.Log.e("DepotSetup", "Error getting location: ${e.message}")
            }
    } catch (e: SecurityException) {
        android.util.Log.e("DepotSetup", "Location permission not granted", e)
    }
}