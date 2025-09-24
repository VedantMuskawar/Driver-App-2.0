package com.pave.driversapp

import android.app.Application
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
