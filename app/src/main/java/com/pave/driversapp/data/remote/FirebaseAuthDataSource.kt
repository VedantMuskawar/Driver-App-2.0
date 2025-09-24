package com.pave.driversapp.data.remote

import android.app.Activity
import com.google.firebase.FirebaseException
import com.google.firebase.auth.FirebaseAuth
import com.google.firebase.auth.PhoneAuthCredential
import com.google.firebase.auth.PhoneAuthOptions
import com.google.firebase.auth.PhoneAuthProvider
import kotlinx.coroutines.tasks.await
import java.util.concurrent.TimeUnit
import javax.inject.Inject

class FirebaseAuthDataSource(
    private val firebaseAuth: FirebaseAuth
) {
    
    private var verificationId: String? = null
    private var resendToken: PhoneAuthProvider.ForceResendingToken? = null
    private var verificationCompleted = false
    private var autoCredential: PhoneAuthCredential? = null
    
    suspend fun sendOtp(phoneNumber: String, activity: Activity): Result<String> {
        return try {
            val options = PhoneAuthOptions.newBuilder(firebaseAuth)
                .setPhoneNumber(phoneNumber)
                .setTimeout(60L, TimeUnit.SECONDS)
                .setActivity(activity)
                .setCallbacks(object : PhoneAuthProvider.OnVerificationStateChangedCallbacks() {
                    override fun onVerificationCompleted(credential: PhoneAuthCredential) {
                        // Auto-verification completed (common with test numbers)
                        android.util.Log.d("FirebaseAuth", "✅ onVerificationCompleted called - Auto verification")
                        verificationCompleted = true
                        autoCredential = credential
                        verificationId = "auto_verification_completed"
                    }
                    
                    override fun onVerificationFailed(e: FirebaseException) {
                        android.util.Log.e("FirebaseAuth", "❌ onVerificationFailed: ${e.message}")
                        verificationCompleted = false
                        autoCredential = null
                    }
                    
                    override fun onCodeSent(verificationId: String, token: PhoneAuthProvider.ForceResendingToken) {
                        android.util.Log.d("FirebaseAuth", "📱 onCodeSent called - Manual OTP required")
                        this@FirebaseAuthDataSource.verificationId = verificationId
                        this@FirebaseAuthDataSource.resendToken = token
                        verificationCompleted = false
                    }
                })
                .build()
            
            PhoneAuthProvider.verifyPhoneNumber(options)
            
            // Wait a bit for callbacks to complete
            kotlinx.coroutines.delay(1000)
            
            Result.success("OTP sent successfully")
        } catch (e: Exception) {
            Result.failure(e)
        }
    }
    
            suspend fun verifyOtp(otp: String): Result<String> {
                return try {
                    android.util.Log.d("FirebaseAuth", "🔐 verifyOtp called with: $otp")
                    
                    // Check if auto-verification completed (common with test numbers)
                    if (verificationCompleted && autoCredential != null) {
                        android.util.Log.d("FirebaseAuth", "✅ Using auto-verification credential")
                        val result = firebaseAuth.signInWithCredential(autoCredential!!).await()
                        if (result.user != null) {
                            android.util.Log.d("FirebaseAuth", "✅ Auto-verification successful: ${result.user!!.uid}")
                            return Result.success(result.user!!.uid)
                        }
                    }
                    
                    // Manual verification with OTP
                    val verificationId = this.verificationId
                    android.util.Log.d("FirebaseAuth", "📱 Manual verification with ID: $verificationId")
                    
                    if (verificationId == null) {
                        android.util.Log.e("FirebaseAuth", "❌ No verification ID available")
                        return Result.failure(Exception("No verification ID available. Try sending OTP again."))
                    }
                    
                    val credential = PhoneAuthProvider.getCredential(verificationId, otp)
                    val result = firebaseAuth.signInWithCredential(credential).await()
                    
                    if (result.user != null) {
                        android.util.Log.d("FirebaseAuth", "✅ Manual verification successful: ${result.user!!.uid}")
                        Result.success(result.user!!.uid)
                    } else {
                        android.util.Log.e("FirebaseAuth", "❌ Manual verification failed")
                        Result.failure(Exception("Authentication failed"))
                    }
                } catch (e: Exception) {
                    android.util.Log.e("FirebaseAuth", "❌ verifyOtp exception: ${e.message}")
                    Result.failure(e)
                }
            }
    
    fun getCurrentUser() = firebaseAuth.currentUser
    
    fun isAutoVerificationCompleted(): Boolean {
        return verificationCompleted && autoCredential != null
    }
    
    suspend fun signOut() {
        firebaseAuth.signOut()
        verificationId = null
        verificationCompleted = false
        autoCredential = null
    }
}
