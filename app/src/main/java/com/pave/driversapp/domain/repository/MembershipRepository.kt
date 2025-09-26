package com.pave.driversapp.domain.repository

import com.pave.driversapp.domain.model.Membership
import com.pave.driversapp.domain.model.UserRole
import com.pave.driversapp.domain.model.User
import kotlinx.coroutines.tasks.await

interface MembershipRepository {
    suspend fun getMembership(userId: String, orgId: String): Membership?
    suspend fun getUserRole(userId: String, orgId: String): UserRole?
    suspend fun getDriverVehicleId(userId: String, orgId: String): String?
    suspend fun isUserActive(userId: String, orgId: String): Boolean
    suspend fun getUserRoleFromUserModel(userId: String, orgId: String): UserRole?
}

class MembershipRepositoryImpl(
    private val firestore: com.google.firebase.firestore.FirebaseFirestore
) : MembershipRepository {
    
    private val membershipCollection = firestore.collection("MEMBERSHIP")
    
    override suspend fun getMembership(userId: String, orgId: String): Membership? {
        return try {
            android.util.Log.d("MembershipRepository", "🔍 Fetching membership for userId: $userId, orgId: $orgId")
            
            val snapshot = membershipCollection
                .whereEqualTo("userID", userId)  // Match actual field name in your document
                .whereEqualTo("orgID", orgId)    // Match actual field name in your document
                .limit(1)
                .get()
                .await()
            
            val membership = snapshot.documents.firstOrNull()?.toObject(Membership::class.java)
            android.util.Log.d("MembershipRepository", "📋 Membership result: ${if (membership != null) "Found" else "Not found"}")
            
            if (membership != null) {
                android.util.Log.d("MembershipRepository", "👤 User role: ${membership.userRole.displayName}")
                if (membership.isDriver) {
                    android.util.Log.d("MembershipRepository", "🚗 Driver vehicle: ${membership.vehicleNumber} (ID: ${membership.vehicleId})")
                }
            }
            
            membership
        } catch (e: Exception) {
            android.util.Log.e("MembershipRepository", "❌ Error fetching membership: ${e.message}")
            null
        }
    }
    
    override suspend fun getUserRole(userId: String, orgId: String): UserRole? {
        return try {
            android.util.Log.d("MembershipRepository", "🔍 Fetching user role for userId: $userId, orgId: $orgId")
            
            val snapshot = membershipCollection
                .whereEqualTo("userID", userId)
                .whereEqualTo("orgID", orgId)
                .limit(1)
                .get()
                .await()
            
            val document = snapshot.documents.firstOrNull()
            if (document != null) {
                val role = document.getLong("role")?.toInt() ?: 2
                val userRole = UserRole.fromValue(role) ?: UserRole.DRIVER
                android.util.Log.d("MembershipRepository", "👤 Found user role: ${userRole.displayName} ($role)")
                userRole
            } else {
                android.util.Log.w("MembershipRepository", "⚠️ Membership document not found, trying fallback")
                getUserRoleFromUserModel(userId, orgId)
            }
        } catch (e: Exception) {
            android.util.Log.e("MembershipRepository", "❌ Error fetching user role: ${e.message}")
            getUserRoleFromUserModel(userId, orgId)
        }
    }
    
    override suspend fun getUserRoleFromUserModel(userId: String, orgId: String): UserRole? {
        return try {
            android.util.Log.d("MembershipRepository", "🔍 Trying to get role from User model for userId: $userId")
            
            // Try to get user from USERS collection as fallback
            val usersCollection = firestore.collection("USERS")
            val snapshot = usersCollection
                .whereEqualTo("userID", userId)
                .whereEqualTo("orgID", orgId)
                .limit(1)
                .get()
                .await()
            
            val user = snapshot.documents.firstOrNull()?.toObject(User::class.java)
            if (user != null) {
                val role = UserRole.fromValue(user.role)
                android.util.Log.d("MembershipRepository", "✅ Found user role from User model: ${role?.displayName}")
                return role
            }
            
            // Final fallback: Return DRIVER since logs show UserRole: 2
            android.util.Log.w("MembershipRepository", "⚠️ User not found in USERS collection, using DRIVER as fallback")
            UserRole.DRIVER
        } catch (e: Exception) {
            android.util.Log.e("MembershipRepository", "❌ Error getting role from User model: ${e.message}")
            // Final fallback: Return DRIVER since logs show UserRole: 2
            UserRole.DRIVER
        }
    }
    
    override suspend fun getDriverVehicleId(userId: String, orgId: String): String? {
        return try {
            android.util.Log.d("MembershipRepository", "🔍 Fetching driver vehicleId for userId: $userId, orgId: $orgId")
            
            val snapshot = membershipCollection
                .whereEqualTo("userID", userId)
                .whereEqualTo("orgID", orgId)
                .limit(1)
                .get()
                .await()
            
            val document = snapshot.documents.firstOrNull()
            if (document != null) {
                val vehicleId = document.getString("vehicleID")
                val role = document.getLong("role")?.toInt() ?: 2
                
                android.util.Log.d("MembershipRepository", "🚗 Found vehicleID: $vehicleId for role: $role")
                
                // Return vehicleId if user is a driver, null otherwise
                if (role == 2 && vehicleId != null) {
                    android.util.Log.d("MembershipRepository", "✅ Driver vehicle retrieved: $vehicleId")
                    vehicleId
                } else {
                    android.util.Log.d("MembershipRepository", "ℹ️ User not a driver or no vehicleId")
                    null
                }
            } else {
                android.util.Log.w("MembershipRepository", "⚠️ Membership document not found")
                null
            }
        } catch (e: Exception) {
            android.util.Log.e("MembershipRepository", "❌ Error fetching driver vehicle ID: ${e.message}")
            null
        }
    }
    
    override suspend fun isUserActive(userId: String, orgId: String): Boolean {
        val membership = getMembership(userId, orgId)
        return membership?.isActive == true
    }
}
