package com.pave.driversapp.presentation.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.pave.driversapp.domain.service.DepotAccessResult
import com.pave.driversapp.domain.service.LocationService
import com.pave.driversapp.data.repository.DepotRepositoryImpl
import com.google.firebase.firestore.FirebaseFirestore

@Composable
fun GeofenceEnforcedButton(
    text: String,
    onClick: () -> Unit,
    orgId: String,
    modifier: Modifier = Modifier,
    enabled: Boolean = true,
    depotRepository: com.pave.driversapp.domain.repository.DepotRepository
) {
    val context = LocalContext.current
    var showDialog by remember { mutableStateOf(false) }
    var accessResult by remember { mutableStateOf<DepotAccessResult?>(null) }
    var isChecking by remember { mutableStateOf(false) }
    
    val locationService = remember { LocationService(context, depotRepository) }
    
    // Handle the async check
    LaunchedEffect(isChecking) {
        if (isChecking) {
            val result = locationService.checkDepotAccess(orgId)
            accessResult = result
            showDialog = true
            isChecking = false
            
            // If access is granted, proceed with the action
            if (result is DepotAccessResult.AccessGranted) {
                onClick()
                showDialog = false
            }
        }
    }
    
    Button(
        onClick = {
            if (!isChecking) {
                isChecking = true
            }
        },
        enabled = enabled && !isChecking,
        modifier = modifier
    ) {
        if (isChecking) {
            CircularProgressIndicator(
                modifier = Modifier.size(16.dp),
                color = MaterialTheme.colorScheme.onPrimary
            )
            Spacer(modifier = Modifier.width(8.dp))
        }
        Text(text)
    }
    
    // Show dialog for access denied or other issues
    accessResult?.let { result ->
        if (showDialog && result !is DepotAccessResult.AccessGranted) {
            DepotAccessDialog(
                accessResult = result,
                onDismiss = { 
                    showDialog = false
                    accessResult = null
                },
                onRetry = {
                    showDialog = false
                    accessResult = null
                    isChecking = false
                    // Retry will be triggered by the button click again
                }
            )
        }
    }
}

@Composable
fun GeofenceEnforcedDispatchButton(
    orgId: String,
    depotRepository: com.pave.driversapp.domain.repository.DepotRepository,
    onDispatch: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    GeofenceEnforcedButton(
        text = "Dispatch",
        onClick = onDispatch,
        orgId = orgId,
        depotRepository = depotRepository,
        modifier = modifier,
        enabled = enabled
    )
}

@Composable
fun GeofenceEnforcedReturnButton(
    orgId: String,
    depotRepository: com.pave.driversapp.domain.repository.DepotRepository,
    onReturn: () -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    GeofenceEnforcedButton(
        text = "Return",
        onClick = onReturn,
        orgId = orgId,
        depotRepository = depotRepository,
        modifier = modifier,
        enabled = enabled
    )
}