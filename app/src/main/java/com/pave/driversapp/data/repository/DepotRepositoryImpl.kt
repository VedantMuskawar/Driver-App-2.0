package com.pave.driversapp.data.repository

import android.content.Context
import android.content.SharedPreferences
import com.google.firebase.firestore.FirebaseFirestore
import com.google.firebase.Timestamp
import com.pave.driversapp.domain.model.DepotLocation
import com.pave.driversapp.domain.model.DepotSettings
import com.pave.driversapp.domain.repository.DepotRepository
import com.pave.driversapp.domain.util.LocationUtils
import kotlinx.coroutines.tasks.await
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class DepotRepositoryImpl(
    private val firestore: FirebaseFirestore,
    private val context: Context
) : DepotRepository {
    
    private val prefs: SharedPreferences = context.getSharedPreferences("depot_settings", Context.MODE_PRIVATE)
    private val json = Json { ignoreUnknownKeys = true }
    
    override suspend fun getDepot(orgId: String): Result<DepotSettings?> {
        return try {
            android.util.Log.d("DepotRepository", "🔍 Fetching depot settings for org: $orgId")
            
            val document = firestore.collection("DRIVERS_APP_SETTINGS")
                .document(orgId)
                .get()
                .await()
            
            if (document.exists()) {
                val depotLocation = document.get("depotLocation") as? Map<String, Any>
                val lat = depotLocation?.get("lat") as? Double ?: 0.0
                val lng = depotLocation?.get("lng") as? Double ?: 0.0
                
                val depotSettings = DepotSettings(
                    depotLocation = DepotLocation(lat, lng),
                    radius = document.getLong("radius")?.toInt() ?: 0,
                    createdBy = document.getString("createdBy") ?: "",
                    createdAt = document.getTimestamp("createdAt"),
                    updatedAt = document.getTimestamp("updatedAt")
                )
                
                android.util.Log.d("DepotRepository", "✅ Found depot settings: ${depotSettings.depotLocation.lat}, ${depotSettings.depotLocation.lng}, radius: ${depotSettings.radius}")
                
                // Cache the depot settings
                cacheDepot(orgId, depotSettings)
                
                Result.success(depotSettings)
            } else {
                android.util.Log.d("DepotRepository", "⚠️ No depot settings found for org: $orgId")
                Result.success(null)
            }
        } catch (e: Exception) {
            android.util.Log.e("DepotRepository", "❌ Error fetching depot settings: ${e.message}")
            Result.failure(e)
        }
    }
    
    override suspend fun setDepot(orgId: String, depotLocation: DepotLocation, radius: Int, createdBy: String): Result<Unit> {
        return try {
            android.util.Log.d("DepotRepository", "💾 Setting depot for org: $orgId at ${depotLocation.lat}, ${depotLocation.lng} with radius: $radius")
            
            val depotSettings = DepotSettings(
                depotLocation = depotLocation,
                radius = radius,
                createdBy = createdBy,
                createdAt = Timestamp.now(),
                updatedAt = Timestamp.now()
            )
            
            val data = mapOf(
                "depotLocation" to mapOf(
                    "lat" to depotLocation.lat,
                    "lng" to depotLocation.lng
                ),
                "radius" to radius,
                "createdBy" to createdBy,
                "createdAt" to Timestamp.now(),
                "updatedAt" to Timestamp.now()
            )
            
            firestore.collection("DRIVERS_APP_SETTINGS")
                .document(orgId)
                .set(data)
                .await()
            
            // Cache the depot settings
            cacheDepot(orgId, depotSettings)
            
            android.util.Log.d("DepotRepository", "✅ Depot settings saved successfully")
            Result.success(Unit)
        } catch (e: Exception) {
            android.util.Log.e("DepotRepository", "❌ Error saving depot settings: ${e.message}")
            Result.failure(e)
        }
    }
    
    override suspend fun deleteDepot(orgId: String): Result<Unit> {
        return try {
            android.util.Log.d("DepotRepository", "🗑️ Deleting depot for org: $orgId")
            
            // Delete from Firestore
            firestore.collection("DRIVERS_APP_SETTINGS")
                .document(orgId)
                .delete()
                .await()
            
            // Remove from cache
            prefs.edit().remove("depot_$orgId").apply()
            
            android.util.Log.d("DepotRepository", "✅ Depot settings deleted successfully")
            Result.success(Unit)
        } catch (e: Exception) {
            android.util.Log.e("DepotRepository", "❌ Error deleting depot settings: ${e.message}")
            Result.failure(e)
        }
    }
    
    override suspend fun isInsideDepot(currentLocation: DepotLocation, depotSettings: DepotSettings): Boolean {
        val isInside = LocationUtils.isInsideDepot(currentLocation, depotSettings.depotLocation, depotSettings.radius)
        android.util.Log.d("DepotRepository", "📍 Location check: ${currentLocation.lat}, ${currentLocation.lng} inside depot (${depotSettings.depotLocation.lat}, ${depotSettings.depotLocation.lng}, radius: ${depotSettings.radius}) = $isInside")
        return isInside
    }
    
    override suspend fun getCachedDepot(orgId: String): DepotSettings? {
        return try {
            val cachedJson = prefs.getString("depot_$orgId", null)
            if (cachedJson != null) {
                val depotSettings = json.decodeFromString<DepotSettings>(cachedJson)
                android.util.Log.d("DepotRepository", "📱 Retrieved cached depot settings for org: $orgId")
                depotSettings
            } else {
                android.util.Log.d("DepotRepository", "⚠️ No cached depot settings for org: $orgId")
                null
            }
        } catch (e: Exception) {
            android.util.Log.e("DepotRepository", "❌ Error reading cached depot settings: ${e.message}")
            null
        }
    }
    
    override suspend fun cacheDepot(orgId: String, depotSettings: DepotSettings) {
        try {
            val jsonString = json.encodeToString(depotSettings)
            prefs.edit().putString("depot_$orgId", jsonString).apply()
            android.util.Log.d("DepotRepository", "💾 Cached depot settings for org: $orgId")
        } catch (e: Exception) {
            android.util.Log.e("DepotRepository", "❌ Error caching depot settings: ${e.message}")
        }
    }
}
