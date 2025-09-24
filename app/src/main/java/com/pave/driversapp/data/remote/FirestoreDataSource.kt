package com.pave.driversapp.data.remote

import com.google.firebase.firestore.FirebaseFirestore
import com.pave.driversapp.domain.model.User
import kotlinx.coroutines.tasks.await
class FirestoreDataSource(
    private val firestore: FirebaseFirestore
) {
    
    suspend fun getMembershipByPhone(phoneNumber: String): Result<List<User>> {
        return try {
            android.util.Log.d("FirestoreDataSource", "🔍 Querying MEMBERSHIP collection for phone: $phoneNumber")
            
            // First, let's see what documents exist in the collection
            val allDocs = firestore.collection("MEMBERSHIP")
                .limit(5)
                .get()
                .await()
            
            android.util.Log.d("FirestoreDataSource", "📊 Found ${allDocs.documents.size} total documents in MEMBERSHIP collection")
            
            // Log sample documents to see the structure
            allDocs.documents.forEach { doc ->
                android.util.Log.d("FirestoreDataSource", "📄 Sample doc ${doc.id}: ${doc.data}")
            }
            
            // Try the original phone number format first
            val result = firestore.collection("MEMBERSHIP")
                .whereEqualTo("member.phoneNumber", phoneNumber)
                .get()
                .await()
            
            android.util.Log.d("FirestoreDataSource", "📊 Query for '$phoneNumber' returned ${result.documents.size} documents")
            
            val users = result.documents.mapNotNull { document ->
                try {
                    android.util.Log.d("FirestoreDataSource", "📄 Processing document: ${document.id}")
                    val memberData = document.get("member") as? Map<String, Any>
                    val user = User(
                        userID = document.getString("userID") ?: "",
                        name = memberData?.get("name") as? String ?: "",
                        phoneNumber = memberData?.get("phoneNumber") as? String ?: "",
                        orgID = document.getString("orgID") ?: "",
                        orgName = document.getString("orgName") ?: "",
                        role = document.getLong("role")?.toInt() ?: 2
                    )
                    android.util.Log.d("FirestoreDataSource", "👤 Created user: ${user.name} (${user.phoneNumber})")
                    user
                } catch (e: Exception) {
                    android.util.Log.e("FirestoreDataSource", "❌ Error processing document: ${e.message}")
                    null
                }
            }
            
            android.util.Log.d("FirestoreDataSource", "✅ Successfully processed ${users.size} users")
            Result.success(users)
        } catch (e: Exception) {
            android.util.Log.e("FirestoreDataSource", "❌ Query failed: ${e.message}")
            Result.failure(e)
        }
    }
}
