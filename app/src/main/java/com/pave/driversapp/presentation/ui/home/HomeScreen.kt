package com.pave.driversapp.presentation.ui.home

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
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.pave.driversapp.presentation.ui.components.GeofenceEnforcedDispatchButton
import com.pave.driversapp.presentation.ui.components.GeofenceEnforcedReturnButton
import com.pave.driversapp.data.repository.DepotRepositoryImpl
import com.google.firebase.firestore.FirebaseFirestore
import com.pave.driversapp.presentation.viewmodel.AuthViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onLogout: () -> Unit,
    onSettings: () -> Unit,
    authRepository: com.pave.driversapp.domain.repository.AuthRepository,
    viewModel: AuthViewModel
) {
    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val user = uiState.authResult?.user
    val context = LocalContext.current
    val orgId = uiState.selectedOrgId

    // Create depot repository for geofence enforcement
    val depotRepository = remember { DepotRepositoryImpl(FirebaseFirestore.getInstance(), context) }

    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Welcome Header (Uber-style)
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = Color.Black
            ),
            shape = RoundedCornerShape(16.dp)
        ) {
            Column(
                modifier = Modifier.padding(24.dp)
            ) {
                Text(
                    text = "Good ${getGreeting()}!",
                    style = MaterialTheme.typography.headlineMedium,
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
                
                Spacer(modifier = Modifier.height(8.dp))
                
                Text(
                    text = user?.name ?: "Driver",
                    style = MaterialTheme.typography.titleLarge,
                    color = Color.White,
                    fontWeight = FontWeight.Medium
                )
                
                Text(
                    text = "${user?.orgName ?: "Organization"} • ${getRoleText(user?.role ?: 0)}",
                    style = MaterialTheme.typography.bodyMedium,
                    color = Color.White.copy(alpha = 0.7f)
                )
            }
        }

        // Quick Actions (Uber-style)
        Text(
            text = "Quick Actions",
            style = MaterialTheme.typography.titleLarge,
            color = Color.White,
            fontWeight = FontWeight.Bold
        )

        // Driver actions with geofence enforcement
        if (user?.role == 2) { // Driver role
            Card(
                modifier = Modifier.fillMaxWidth(),
                colors = CardDefaults.cardColors(
                    containerColor = Color(0xFF1E1E1E)
                ),
                shape = RoundedCornerShape(16.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier.padding(20.dp)
                ) {
                    Text(
                        text = "Driver Actions",
                        style = MaterialTheme.typography.titleMedium,
                        color = Color.White,
                        fontWeight = FontWeight.Bold
                    )
                    
                    Spacer(modifier = Modifier.height(16.dp))
                    
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        GeofenceEnforcedDispatchButton(
                            orgId = orgId,
                            depotRepository = depotRepository,
                            onDispatch = {
                                // Handle dispatch action
                                android.util.Log.d("HomeScreen", "🚚 Dispatch action triggered")
                            },
                            modifier = Modifier.weight(1f)
                        )

                        GeofenceEnforcedReturnButton(
                            orgId = orgId,
                            depotRepository = depotRepository,
                            onReturn = {
                                // Handle return action
                                android.util.Log.d("HomeScreen", "🏠 Return action triggered")
                            },
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }
        }

        // Stats Cards (Uber-style)
        Text(
            text = "Today's Overview",
            style = MaterialTheme.typography.titleLarge,
            color = Color.White,
            fontWeight = FontWeight.Bold
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            StatCard(
                title = "Orders",
                value = "12",
                icon = Icons.Filled.List,
                color = Color(0xFF2196F3),
                modifier = Modifier.weight(1f)
            )
            
            StatCard(
                title = "Delivered",
                value = "8",
                icon = Icons.Filled.CheckCircle,
                color = Color(0xFF4CAF50),
                modifier = Modifier.weight(1f)
            )
            
            StatCard(
                title = "Pending",
                value = "4",
                icon = Icons.Filled.Star,
                color = Color(0xFFFF9800),
                modifier = Modifier.weight(1f)
            )
        }

        // Recent Activity (Uber-style)
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = Color(0xFF1E1E1E)
            ),
            shape = RoundedCornerShape(16.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
        ) {
            Column(
                modifier = Modifier.padding(20.dp)
            ) {
                Text(
                    text = "Recent Activity",
                    style = MaterialTheme.typography.titleMedium,
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
                
                Spacer(modifier = Modifier.height(16.dp))
                
                ActivityItem(
                    icon = Icons.Filled.CheckCircle,
                    title = "Order #1234 delivered",
                    subtitle = "2 hours ago",
                    iconColor = Color(0xFF4CAF50)
                )
                
                ActivityItem(
                    icon = Icons.Filled.Star,
                    title = "Order #1235 in progress",
                    subtitle = "4 hours ago",
                    iconColor = Color(0xFFFF9800)
                )
                
                ActivityItem(
                    icon = Icons.Filled.List,
                    title = "Order #1236 assigned",
                    subtitle = "6 hours ago",
                    iconColor = Color(0xFF2196F3)
                )
            }
        }

        Spacer(modifier = Modifier.weight(1f))
    }
}

@Composable
fun StatCard(
    title: String,
    value: String,
    icon: ImageVector,
    color: Color,
    modifier: Modifier = Modifier
) {
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = Color(0xFF1E1E1E)
        ),
        shape = RoundedCornerShape(16.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier.padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = color,
                modifier = Modifier.size(32.dp)
            )
            
            Spacer(modifier = Modifier.height(12.dp))
            
            Text(
                text = value,
                style = MaterialTheme.typography.headlineMedium,
                color = Color.White,
                fontWeight = FontWeight.Bold
            )
            
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                color = Color.Gray
            )
        }
    }
}

@Composable
fun ActivityItem(
    icon: ImageVector,
    title: String,
    subtitle: String,
    iconColor: Color
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            imageVector = icon,
            contentDescription = title,
            tint = iconColor,
            modifier = Modifier.size(24.dp)
        )
        
        Spacer(modifier = Modifier.width(16.dp))
        
        Column(
            modifier = Modifier.weight(1f)
        ) {
            Text(
                text = title,
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White,
                fontWeight = FontWeight.Medium
            )
            
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = Color.Gray
            )
        }
    }
}

private fun getGreeting(): String {
    val hour = java.util.Calendar.getInstance().get(java.util.Calendar.HOUR_OF_DAY)
    return when (hour) {
        in 5..11 -> "Morning"
        in 12..17 -> "Afternoon"
        in 18..21 -> "Evening"
        else -> "Night"
    }
}

private fun getRoleText(role: Int): String {
    return when (role) {
        0 -> "Admin"
        1 -> "Manager"
        2 -> "Driver"
        else -> "Unknown"
    }
}