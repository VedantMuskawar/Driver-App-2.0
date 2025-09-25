package com.pave.driversapp.presentation.navigation

import androidx.compose.runtime.*
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.pave.driversapp.presentation.ui.auth.LoginScreen
import com.pave.driversapp.presentation.ui.main.MainTabScreen
import com.pave.driversapp.presentation.ui.orgselect.OrgSelectScreen
import com.pave.driversapp.presentation.ui.depot.DepotSetupScreen
import com.pave.driversapp.presentation.ui.orders.OrderDetailsScreen
import com.pave.driversapp.presentation.ui.orders.OrderDetailsScreenWithData
import com.pave.driversapp.domain.model.Order
import com.pave.driversapp.presentation.viewmodel.AuthViewModel
import com.pave.driversapp.presentation.viewmodel.DepotViewModel
import com.pave.driversapp.data.repository.DepotRepositoryImpl
import com.google.firebase.firestore.FirebaseFirestore

@Composable
fun DriversAppNavigation(
    navController: NavHostController = rememberNavController(),
    authRepository: com.pave.driversapp.domain.repository.AuthRepository,
    authViewModel: AuthViewModel = viewModel { AuthViewModel(authRepository) }
) {
    val uiState by authViewModel.uiState.collectAsStateWithLifecycle()
    val context = androidx.compose.ui.platform.LocalContext.current
    
    // Create depot repository and view model
    val depotRepository = remember { DepotRepositoryImpl(FirebaseFirestore.getInstance(), context) }
    val depotViewModel: DepotViewModel = viewModel { DepotViewModel(depotRepository) }
    
    // Determine start destination based on authentication state
    val startDestination = if (uiState.authCompleted) "home" else "login"
    
    // Navigate to home if session is restored
    LaunchedEffect(uiState.authCompleted) {
        if (uiState.authCompleted && navController.currentDestination?.route != "home") {
            android.util.Log.d("Navigation", "🔄 Session restored, navigating to home")
            navController.navigate("home") {
                popUpTo(0) { inclusive = true }
            }
        }
    }
    
    NavHost(
        navController = navController,
        startDestination = startDestination
    ) {
        composable("login") {
            LoginScreen(
                onAuthSuccess = {
                    android.util.Log.d("Navigation", "🚀 onAuthSuccess called!")
                    val authResult = uiState.authResult
                    android.util.Log.d("Navigation", "📊 AuthResult: $authResult")
                    android.util.Log.d("Navigation", "🏢 Organizations count: ${authResult?.organizations?.size}")
                    android.util.Log.d("Navigation", "✅ AuthCompleted: ${uiState.authCompleted}")
                    
                    if (uiState.authCompleted) {
                        android.util.Log.d("Navigation", "✅ Already authenticated, navigating to home")
                        navController.navigate("home") {
                            popUpTo("login") { inclusive = true }
                        }
                    } else {
                        android.util.Log.d("Navigation", "📋 Multiple organizations, navigating to org_select")
                        navController.navigate("org_select")
                    }
                },
                authRepository = authRepository,
                viewModel = authViewModel
            )
        }
        
        composable("org_select") {
            OrgSelectScreen(
                onOrgSelected = {
                    navController.navigate("home") {
                        popUpTo("login") { inclusive = true }
                    }
                },
                authRepository = authRepository,
                viewModel = authViewModel
            )
        }
        
        composable("home") {
            MainTabScreen(
                authRepository = authRepository,
                authViewModel = authViewModel,
                onLogout = {
                    authViewModel.resetAuth()
                    navController.navigate("login") {
                        popUpTo("login") { inclusive = true }
                    }
                },
                onDepotSetup = {
                    navController.navigate("depot_setup")
                },
                onOrderClick = { order ->
                    navController.navigate("order_details/${order.orderId}")
                }
            )
        }
        
        composable("depot_setup") {
            val user = uiState.authResult?.user
            val orgId = uiState.selectedOrgId
            val userId = user?.userID ?: ""
            val orgName = user?.orgName ?: uiState.authResult?.organizations?.find { it.orgID == orgId }?.orgName ?: "Organization"
            
            DepotSetupScreen(
                orgId = orgId,
                orgName = orgName,
                userId = userId,
                depotViewModel = depotViewModel,
                onBack = {
                    navController.popBackStack()
                }
            )
        }
        
        composable(
            route = "order_details/{orderId}",
            arguments = listOf(
                androidx.navigation.navArgument("orderId") {
                    type = androidx.navigation.NavType.StringType
                }
            )
        ) { backStackEntry ->
            val orderId = backStackEntry.arguments?.getString("orderId") ?: ""
            val user = uiState.authResult?.user
            val orgId = uiState.selectedOrgId
            val driverId = user?.userID ?: ""
            
            // Fetch the actual order from SCH_ORDERS collection
            OrderDetailsScreenWithData(
                orderId = orderId,
                orgId = orgId,
                driverId = driverId,
                onBack = {
                    navController.popBackStack()
                }
            )
        }
    }
}
