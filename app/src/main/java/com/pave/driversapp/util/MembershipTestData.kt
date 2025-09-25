package com.pave.driversapp.util

import android.util.Log
import com.google.firebase.firestore.FirebaseFirestore
import com.pave.driversapp.domain.model.Membership
import com.google.firebase.Timestamp
import kotlinx.coroutines.tasks.await
import java.util.Date

object MembershipTestData {
    
    /**
     * Create sample membership data for testing
     */
    suspend fun createSampleMembershipData() {
        try {
            val firestore = FirebaseFirestore.getInstance()
            val membershipCollection = firestore.collection("MEMBERSHIP")
            
            Log.d("MembershipTestData", "🔧 Creating sample membership data...")
            
            // Sample membership for the test driver
            val testDriverMembership = Membership(
                userId = "GQFF7e2LDsZGeex1jTF5LB7KIdB2", // From logs
                orgId = "K4Q6vPOuTcLPtlcEwdw0", // From logs
                role = 2, // Driver
                vehicleId = "xhEn4S62VCz6wm5b57La", // From SCH_ORDERS logs
                vehicleNumber = "MH34 AB8930", // From SCH_ORDERS logs
                isActive = true,
                createdAt = Timestamp(Date()),
                updatedAt = Timestamp(Date())
            )
            
            // Add the membership document
            membershipCollection.document("${testDriverMembership.userId}_${testDriverMembership.orgId}")
                .set(testDriverMembership)
                .await()
            
            Log.d("MembershipTestData", "✅ Created membership for driver: ${testDriverMembership.userId}")
            Log.d("MembershipTestData", "🚗 Vehicle: ${testDriverMembership.vehicleNumber} (${testDriverMembership.vehicleId})")
            
            // Create additional sample memberships for other roles
            val adminMembership = Membership(
                userId = "admin_user_id",
                orgId = "K4Q6vPOuTcLPtlcEwdw0",
                role = 0, // Admin
                vehicleId = null,
                vehicleNumber = null,
                isActive = true,
                createdAt = Timestamp(Date()),
                updatedAt = Timestamp(Date())
            )
            
            val managerMembership = Membership(
                userId = "manager_user_id",
                orgId = "K4Q6vPOuTcLPtlcEwdw0",
                role = 1, // Manager
                vehicleId = null,
                vehicleNumber = null,
                isActive = true,
                createdAt = Timestamp(Date()),
                updatedAt = Timestamp(Date())
            )
            
            membershipCollection.document("${adminMembership.userId}_${adminMembership.orgId}")
                .set(adminMembership)
                .await()
                
            membershipCollection.document("${managerMembership.userId}_${managerMembership.orgId}")
                .set(managerMembership)
                .await()
            
            Log.d("MembershipTestData", "✅ Created sample memberships for all roles")
            
        } catch (e: Exception) {
            Log.e("MembershipTestData", "❌ Error creating sample membership data: ${e.message}")
        }
    }
    
    /**
     * Check if membership data exists
     */
    suspend fun checkMembershipData(userId: String, orgId: String): Boolean {
        return try {
            val firestore = FirebaseFirestore.getInstance()
            val membershipCollection = firestore.collection("MEMBERSHIP")
            
            val snapshot = membershipCollection
                .whereEqualTo("userId", userId)
                .whereEqualTo("orgId", orgId)
                .limit(1)
                .get()
                .await()
            
            val exists = !snapshot.isEmpty
            Log.d("MembershipTestData", "🔍 Membership exists for $userId: $exists")
            exists
        } catch (e: Exception) {
            Log.e("MembershipTestData", "❌ Error checking membership data: ${e.message}")
            false
        }
    }
}
