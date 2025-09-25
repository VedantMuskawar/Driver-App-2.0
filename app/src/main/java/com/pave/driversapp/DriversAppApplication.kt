package com.pave.driversapp

import android.app.Application
import android.util.Log
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.pave.driversapp.data.local.SecurePreferences
import com.pave.driversapp.data.repository.AuthRepositoryImpl
import com.pave.driversapp.data.repository.DepotRepositoryImpl
import com.pave.driversapp.domain.repository.TripsRepositoryImpl
import com.pave.driversapp.data.remote.FirebaseAuthDataSource
import com.pave.driversapp.data.remote.FirestoreDataSource
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.firestore.FirebaseFirestore
import com.google.android.gms.location.FusedLocationProviderClient
import com.google.android.gms.location.LocationServices

class DriversAppApplication : Application() {
    
    override fun onCreate() {
        super.onCreate()
        
        // Set up crash handling for Compose hover events
        Thread.setDefaultUncaughtExceptionHandler { thread, exception ->
            if (exception.message?.contains("ACTION_HOVER_EXIT") == true) {
                Log.w("ComposeCrashHandler", "Caught ACTION_HOVER_EXIT crash, ignoring: ${exception.message}")
                // Don't crash the app for this known Compose issue
                return@setDefaultUncaughtExceptionHandler
            }
            // Let other crashes be handled normally
            Thread.getDefaultUncaughtExceptionHandler()?.uncaughtException(thread, exception)
        }
    }
    
    // Manual dependency injection
    val firebaseAuth by lazy { FirebaseAuth.getInstance() }
    val firebaseFirestore by lazy { FirebaseFirestore.getInstance() }
    
    val firebaseAuthDataSource by lazy { FirebaseAuthDataSource(firebaseAuth) }
    val firestoreDataSource by lazy { FirestoreDataSource(firebaseFirestore) }
    
    val securePreferences by lazy { SecurePreferences(this) }
    
    val authRepository by lazy { 
        AuthRepositoryImpl(
            firebaseAuthDataSource,
            firestoreDataSource,
            securePreferences
        )
    }
    
    val fusedLocationClient by lazy { LocationServices.getFusedLocationProviderClient(this) }
    
    val depotRepository by lazy { DepotRepositoryImpl(firebaseFirestore, this) }
    val tripsRepository by lazy { TripsRepositoryImpl(firebaseFirestore, this) }
}
