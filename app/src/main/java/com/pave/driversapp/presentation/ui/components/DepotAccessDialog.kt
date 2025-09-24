package com.pave.driversapp.presentation.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.pave.driversapp.domain.service.DepotAccessResult
import com.pave.driversapp.domain.util.LocationUtils

@Composable
fun DepotAccessDialog(
    accessResult: DepotAccessResult,
    onDismiss: () -> Unit,
    onRetry: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Text(
                text = when (accessResult) {
                    is DepotAccessResult.AccessDenied -> "Outside Depot Area"
                    is DepotAccessResult.NoDepotConfigured -> "Depot Not Configured"
                    is DepotAccessResult.LocationUnavailable -> "Location Unavailable"
                    is DepotAccessResult.Error -> "Error"
                    is DepotAccessResult.AccessGranted -> "Access Granted"
                }
            )
        },
        text = {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                when (accessResult) {
                    is DepotAccessResult.AccessDenied -> {
                        Text(
                            text = "You must be inside the depot to perform this action.",
                            textAlign = TextAlign.Center
                        )
                        Spacer(modifier = Modifier.height(8.dp))
                        Text(
                            text = "Current distance from depot: ${LocationUtils.formatDistance(
                                LocationUtils.calculateDistance(
                                    accessResult.currentLocation,
                                    accessResult.depotSettings.depotLocation
                                )
                            )}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            text = "Depot radius: ${accessResult.depotSettings.radius}m",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                    is DepotAccessResult.NoDepotConfigured -> {
                        Text(
                            text = "Depot not configured for your organization. Please contact your administrator.",
                            textAlign = TextAlign.Center
                        )
                    }
                    is DepotAccessResult.LocationUnavailable -> {
                        Text(
                            text = "Unable to get your current location. Please check location permissions and try again.",
                            textAlign = TextAlign.Center
                        )
                    }
                    is DepotAccessResult.Error -> {
                        Text(
                            text = accessResult.message,
                            textAlign = TextAlign.Center
                        )
                    }
                    is DepotAccessResult.AccessGranted -> {
                        Text(
                            text = "You are inside the depot area. Action allowed.",
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }
        },
        confirmButton = {
            when (accessResult) {
                is DepotAccessResult.AccessDenied,
                is DepotAccessResult.LocationUnavailable -> {
                    Button(onClick = onRetry) {
                        Text("Retry")
                    }
                }
                is DepotAccessResult.NoDepotConfigured,
                is DepotAccessResult.Error,
                is DepotAccessResult.AccessGranted -> {
                    Button(onClick = onDismiss) {
                        Text("OK")
                    }
                }
            }
        },
        dismissButton = {
            when (accessResult) {
                is DepotAccessResult.AccessDenied,
                is DepotAccessResult.LocationUnavailable -> {
                    TextButton(onClick = onDismiss) {
                        Text("Cancel")
                    }
                }
                else -> {
                    // No dismiss button for other cases
                }
            }
        }
    )
}
