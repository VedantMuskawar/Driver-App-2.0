package com.pave.driversapp.presentation.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.pave.driversapp.domain.model.AuthResult
import com.pave.driversapp.domain.model.User
import com.pave.driversapp.domain.repository.AuthRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class AuthViewModel(
    private val authRepository: AuthRepository
) : ViewModel() {
    
    private val _uiState = MutableStateFlow(AuthUiState())
    val uiState: StateFlow<AuthUiState> = _uiState.asStateFlow()
    
    init {
        // Check for existing session on initialization
        restoreSession()
    }
    
    private fun restoreSession() {
        viewModelScope.launch {
            android.util.Log.d("AuthViewModel", "🔄 Checking for existing session...")
            
            if (authRepository.isUserLoggedIn()) {
                val cachedUser = authRepository.getCachedUser()
                val cachedOrgId = authRepository.getCachedOrgId()
                
                if (cachedUser != null && cachedOrgId != null) {
                    android.util.Log.d("AuthViewModel", "✅ Found cached session for user: ${cachedUser.name}")
                    
                    val organizations = listOf(
                        com.pave.driversapp.domain.model.Organization(
                            orgID = cachedOrgId,
                            orgName = cachedUser.orgName
                        )
                    )
                    
                    _uiState.value = _uiState.value.copy(
                        authResult = AuthResult(
                            isSuccess = true,
                            user = cachedUser,
                            organizations = organizations
                        ),
                        authCompleted = true,
                        selectedOrgId = cachedOrgId
                    )
                    
                    android.util.Log.d("AuthViewModel", "✅ Session restored successfully")
                } else {
                    android.util.Log.w("AuthViewModel", "⚠️ Cached session data incomplete, clearing session")
                    authRepository.clearSession()
                }
            } else {
                android.util.Log.d("AuthViewModel", "ℹ️ No existing session found")
            }
        }
    }
    
    fun sendOtp(phoneNumber: String, activity: android.app.Activity) {
        viewModelScope.launch {
            android.util.Log.d("AuthViewModel", "🚀 sendOtp called with: $phoneNumber")
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
            
            val result = authRepository.sendOtp(phoneNumber, activity)
            android.util.Log.d("AuthViewModel", "📤 sendOtp result: ${result.isSuccess}")
            
            if (result.isSuccess) {
                // Check if auto-verification happened (common with test numbers)
                val authRepo = authRepository as com.pave.driversapp.data.repository.AuthRepositoryImpl
                val firebaseAuth = authRepo.firebaseAuthDataSource
                
                val isAutoVerified = firebaseAuth.isAutoVerificationCompleted()
                android.util.Log.d("AuthViewModel", "🔍 Auto-verification check: $isAutoVerified")
                
                if (isAutoVerified) {
                    android.util.Log.d("AuthViewModel", "✅ Auto-verification completed, checking membership")
                    // Auto-verification completed, proceed to membership check
                    checkMembership(phoneNumber)
                } else {
                    android.util.Log.d("AuthViewModel", "📱 Manual OTP verification needed")
                    // Manual OTP verification needed
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        otpSent = true,
                        phoneNumber = phoneNumber
                    )
                }
            } else {
                android.util.Log.e("AuthViewModel", "❌ sendOtp failed: ${result.exceptionOrNull()?.message}")
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = result.exceptionOrNull()?.message ?: "Failed to send OTP"
                )
            }
        }
    }
    
    private suspend fun checkMembership(phoneNumber: String) {
        android.util.Log.d("AuthViewModel", "🔍 Checking membership for: $phoneNumber")
        val membershipResult = authRepository.getMembershipByPhone(phoneNumber)
        android.util.Log.d("AuthViewModel", "📊 Membership result: ${membershipResult.isSuccess}")
        
        if (membershipResult.isSuccess) {
            val users = membershipResult.getOrThrow()
            android.util.Log.d("AuthViewModel", "👥 Found ${users.size} users")
            
            if (users.isEmpty()) {
                android.util.Log.w("AuthViewModel", "⚠️ No users found for phone number")
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = "Phone number not registered. Please contact your administrator."
                )
            } else {
                val organizations = users.map { user ->
                    android.util.Log.d("AuthViewModel", "🏢 User: ${user.name} - Org: ${user.orgName}")
                    com.pave.driversapp.domain.model.Organization(
                        orgID = user.orgID,
                        orgName = user.orgName
                    )
                }.distinctBy { it.orgID }
                
                android.util.Log.d("AuthViewModel", "✅ Found ${organizations.size} organizations")
                
                // Auto-select organization if only one exists
                if (organizations.size == 1) {
                    android.util.Log.d("AuthViewModel", "🏢 Auto-selecting single organization: ${organizations.first().orgName}")
                    val user = users.first()
                    val orgId = organizations.first().orgID
                    authRepository.saveUserSession(user, orgId)
                    android.util.Log.d("AuthViewModel", "✅ User session saved, setting authCompleted = true")
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        authResult = AuthResult(
                            isSuccess = true,
                            user = user,
                            organizations = organizations
                        ),
                        authCompleted = true,
                        selectedOrgId = orgId
                    )
                } else {
                    _uiState.value = _uiState.value.copy(
                        isLoading = false,
                        authResult = AuthResult(
                            isSuccess = true,
                            user = users.first(),
                            organizations = organizations
                        )
                    )
                }
            }
        } else {
            android.util.Log.e("AuthViewModel", "❌ Membership check failed: ${membershipResult.exceptionOrNull()?.message}")
            _uiState.value = _uiState.value.copy(
                isLoading = false,
                errorMessage = membershipResult.exceptionOrNull()?.message ?: "Failed to verify membership"
            )
        }
    }
    
    fun verifyOtp(otp: String) {
        viewModelScope.launch {
            android.util.Log.d("AuthViewModel", "🔐 verifyOtp called with: $otp")
            _uiState.value = _uiState.value.copy(isLoading = true, errorMessage = null)
            
            // Check if auto-verification is available first
            val authRepo = authRepository as com.pave.driversapp.data.repository.AuthRepositoryImpl
            val firebaseAuth = authRepo.firebaseAuthDataSource
            
            if (firebaseAuth.isAutoVerificationCompleted()) {
                android.util.Log.d("AuthViewModel", "✅ Using auto-verification, checking membership")
                checkMembership(_uiState.value.phoneNumber)
                return@launch
            }
            
            // First verify the OTP
            val verificationResult = authRepository.verifyOtp(otp)
            android.util.Log.d("AuthViewModel", "🔐 OTP verification result: ${verificationResult.isSuccess}")
            
            if (verificationResult.isSuccess) {
                android.util.Log.d("AuthViewModel", "✅ OTP verified, checking membership")
                // OTP verified successfully, now check membership
                checkMembership(_uiState.value.phoneNumber)
            } else {
                android.util.Log.e("AuthViewModel", "❌ OTP verification failed: ${verificationResult.exceptionOrNull()?.message}")
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = verificationResult.exceptionOrNull()?.message ?: "Invalid OTP"
                )
            }
        }
    }
    
    fun selectOrganization(orgId: String) {
        viewModelScope.launch {
            android.util.Log.d("AuthViewModel", "🏢 selectOrganization called with: $orgId")
            val authResult = _uiState.value.authResult
            if (authResult != null && authResult.isSuccess) {
                val user = authResult.user!!
                authRepository.saveUserSession(user, orgId)
                android.util.Log.d("AuthViewModel", "✅ User session saved, setting authCompleted = true")
                _uiState.value = _uiState.value.copy(
                    authCompleted = true,
                    selectedOrgId = orgId
                )
            } else {
                android.util.Log.e("AuthViewModel", "❌ Cannot select organization - no valid auth result")
            }
        }
    }
    
    fun clearError() {
        _uiState.value = _uiState.value.copy(errorMessage = null)
    }
    
    fun resetAuth() {
        viewModelScope.launch {
            android.util.Log.d("AuthViewModel", "🔄 Resetting authentication state")
            authRepository.clearSession()
            _uiState.value = AuthUiState()
        }
    }
}

data class AuthUiState(
    val isLoading: Boolean = false,
    val otpSent: Boolean = false,
    val phoneNumber: String = "",
    val authResult: AuthResult? = null,
    val authCompleted: Boolean = false,
    val selectedOrgId: String = "",
    val errorMessage: String? = null
)
