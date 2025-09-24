package com.pave.driversapp.presentation.ui.main

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
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.foundation.background
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat
import androidx.activity.ComponentActivity
import androidx.compose.ui.platform.LocalContext
import com.pave.driversapp.presentation.ui.home.HomeScreen
import com.pave.driversapp.presentation.ui.schedule.ScheduleOrdersScreen
import com.pave.driversapp.presentation.ui.settings.SettingsScreen
import com.pave.driversapp.presentation.viewmodel.AuthViewModel
import com.pave.driversapp.domain.repository.AuthRepository

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainTabScreen(
    authRepository: AuthRepository,
    authViewModel: AuthViewModel,
    onLogout: () -> Unit,
    onDepotSetup: () -> Unit,
    onOrderClick: (com.pave.driversapp.domain.model.Order) -> Unit
) {
    var selectedTab by remember { mutableStateOf(0) }
    val context = LocalContext.current
    
    // Handle system UI to prevent notification bar collision
    LaunchedEffect(Unit) {
        val activity = context as? ComponentActivity
        activity?.let {
            WindowCompat.setDecorFitsSystemWindows(it.window, false)
        }
    }
    
    val tabs = listOf(
        TabItem("Home", Icons.Filled.Home),
        TabItem("Schedule Orders", Icons.Filled.List),
        TabItem("Settings", Icons.Filled.Settings)
    )
    
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .statusBarsPadding()
            .navigationBarsPadding()
    ) {
        // Content Area
        Box(
            modifier = Modifier
                .fillMaxSize()
                .weight(1f)
        ) {
            when (selectedTab) {
                0 -> HomeScreen(
                    onLogout = onLogout,
                    onSettings = onDepotSetup,
                    authRepository = authRepository,
                    viewModel = authViewModel
                )
                         1 -> ScheduleOrdersScreen(
                             authViewModel = authViewModel,
                             onOrderClick = onOrderClick
                         )
                2 -> SettingsScreen(
                    authViewModel = authViewModel,
                    onDepotSetup = onDepotSetup,
                    onLogout = onLogout
                )
            }
        }
        
        // Custom Tab Bar (Uber-style)
        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = Color(0xFF1E1E1E)
            ),
            shape = RoundedCornerShape(0.dp),
            elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                tabs.forEachIndexed { index, tab ->
                    TabButton(
                        tab = tab,
                        isSelected = selectedTab == index,
                        onClick = { selectedTab = index },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

@Composable
fun TabButton(
    tab: TabItem,
    isSelected: Boolean,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    IconButton(
        onClick = onClick,
        modifier = modifier
            .padding(horizontal = 4.dp, vertical = 8.dp)
            .size(48.dp)
    ) {
        Icon(
            imageVector = tab.icon,
            contentDescription = tab.title,
            modifier = Modifier.size(24.dp),
            tint = if (isSelected) Color.White else Color.Gray
        )
    }
}

data class TabItem(
    val title: String,
    val icon: ImageVector
)
