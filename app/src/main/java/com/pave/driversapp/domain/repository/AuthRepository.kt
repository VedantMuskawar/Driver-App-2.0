package com.pave.driversapp.domain.repository

import com.pave.driversapp.domain.model.AuthResult
import com.pave.driversapp.domain.model.User

interface AuthRepository {
    suspend fun sendOtp(phoneNumber: String, activity: android.app.Activity): Result<String>
    suspend fun verifyOtp(otp: String): Result<String>
    suspend fun getMembershipByPhone(phoneNumber: String): Result<List<User>>
    suspend fun saveUserSession(user: User, orgId: String)
    suspend fun getCachedUser(): User?
    suspend fun getCachedOrgId(): String?
    suspend fun clearSession()
    fun isUserLoggedIn(): Boolean
}
