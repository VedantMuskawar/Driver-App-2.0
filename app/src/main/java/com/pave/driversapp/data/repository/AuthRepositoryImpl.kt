package com.pave.driversapp.data.repository

import com.pave.driversapp.data.local.SecurePreferences
import com.pave.driversapp.data.remote.FirebaseAuthDataSource
import com.pave.driversapp.data.remote.FirestoreDataSource
import com.pave.driversapp.domain.model.AuthResult
import com.pave.driversapp.domain.model.Organization
import com.pave.driversapp.domain.model.User
import com.pave.driversapp.domain.repository.AuthRepository

class AuthRepositoryImpl(
    val firebaseAuthDataSource: FirebaseAuthDataSource,
    private val firestoreDataSource: FirestoreDataSource,
    private val securePreferences: SecurePreferences
) : AuthRepository {
    
    companion object {
        private const val KEY_IS_LOGGED_IN = "is_logged_in"
        private const val KEY_CURRENT_ORG_ID = "current_org_id"
        private const val KEY_VERIFICATION_ID = "verification_id"
    }
    
    override suspend fun sendOtp(phoneNumber: String, activity: android.app.Activity): Result<String> {
        return firebaseAuthDataSource.sendOtp(phoneNumber, activity)
    }
    
    override suspend fun verifyOtp(otp: String): Result<String> {
        return firebaseAuthDataSource.verifyOtp(otp)
    }
    
    override suspend fun getMembershipByPhone(phoneNumber: String): Result<List<User>> {
        return firestoreDataSource.getMembershipByPhone(phoneNumber)
    }
    
    override suspend fun saveUserSession(user: User, orgId: String) {
        securePreferences.saveBoolean(KEY_IS_LOGGED_IN, true)
        securePreferences.saveString(KEY_CURRENT_ORG_ID, orgId)
        securePreferences.saveString("user_id", user.userID)
        securePreferences.saveString("user_name", user.name)
        securePreferences.saveString("user_phone", user.phoneNumber)
        securePreferences.saveString("user_org_name", user.orgName)
        securePreferences.saveInt("user_role", user.role)
    }
    
    override suspend fun getCachedUser(): User? {
        return try {
            val userID = securePreferences.getString("user_id")
            val name = securePreferences.getString("user_name")
            val phoneNumber = securePreferences.getString("user_phone")
            val orgID = securePreferences.getString(KEY_CURRENT_ORG_ID)
            val orgName = securePreferences.getString("user_org_name")
            val role = securePreferences.getInt("user_role", 2)
            
            if (userID != null && name != null && phoneNumber != null && orgID != null && orgName != null) {
                User(
                    userID = userID,
                    name = name,
                    phoneNumber = phoneNumber,
                    orgID = orgID,
                    orgName = orgName,
                    role = role
                )
            } else {
                null
            }
        } catch (e: Exception) {
            null
        }
    }
    
    override suspend fun getCachedOrgId(): String? {
        return securePreferences.getString(KEY_CURRENT_ORG_ID)
    }
    
    override suspend fun clearSession() {
        securePreferences.clear()
        firebaseAuthDataSource.signOut()
    }
    
    override fun isUserLoggedIn(): Boolean {
        return securePreferences.getBoolean(KEY_IS_LOGGED_IN, false)
    }
}
