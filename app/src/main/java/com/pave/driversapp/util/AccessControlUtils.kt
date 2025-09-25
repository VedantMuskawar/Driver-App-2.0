package com.pave.driversapp.util

import android.util.Log
import com.pave.driversapp.domain.model.UserRole
import com.pave.driversapp.domain.model.AccessPermissions
import com.pave.driversapp.domain.model.Membership
import com.pave.driversapp.domain.repository.MembershipRepository
import kotlinx.coroutines.flow.first

object AccessControlUtils {
    
    /**
     * Get user permissions based on their role
     */
    suspend fun getUserPermissions(
        userId: String,
        orgId: String,
        membershipRepository: MembershipRepository
    ): AccessPermissions? {
        return try {
            val membership = membershipRepository.getMembership(userId, orgId)
            if (membership != null) {
                val permissions = AccessPermissions.forRole(membership.userRole, membership.vehicleId)
                Log.d("AccessControl", "👤 User permissions for ${membership.userRole.displayName}: $permissions")
                permissions
            } else {
                Log.w("AccessControl", "⚠️ No membership found for user: $userId")
                null
            }
        } catch (e: Exception) {
            Log.e("AccessControl", "❌ Error getting user permissions: ${e.message}")
            null
        }
    }
    
    /**
     * Check if user can access depot settings
     */
    suspend fun canAccessDepotSettings(
        userId: String,
        orgId: String,
        membershipRepository: MembershipRepository
    ): Boolean {
        val permissions = getUserPermissions(userId, orgId, membershipRepository)
        return permissions?.canAccessDepotSettings == true
    }
    
    /**
     * Check if user can access all orders
     */
    suspend fun canAccessAllOrders(
        userId: String,
        orgId: String,
        membershipRepository: MembershipRepository
    ): Boolean {
        val permissions = getUserPermissions(userId, orgId, membershipRepository)
        return permissions?.canAccessAllOrders == true
    }
    
    /**
     * Get driver's assigned vehicle ID
     */
    suspend fun getDriverVehicleId(
        userId: String,
        orgId: String,
        membershipRepository: MembershipRepository
    ): String? {
        val permissions = getUserPermissions(userId, orgId, membershipRepository)
        return permissions?.vehicleId
    }
    
    /**
     * Check if user is admin
     */
    suspend fun isAdmin(
        userId: String,
        orgId: String,
        membershipRepository: MembershipRepository
    ): Boolean {
        val membership = membershipRepository.getMembership(userId, orgId)
        return membership?.isAdmin == true
    }
    
    /**
     * Check if user is manager
     */
    suspend fun isManager(
        userId: String,
        orgId: String,
        membershipRepository: MembershipRepository
    ): Boolean {
        val membership = membershipRepository.getMembership(userId, orgId)
        return membership?.isManager == true
    }
    
    /**
     * Check if user is driver
     */
    suspend fun isDriver(
        userId: String,
        orgId: String,
        membershipRepository: MembershipRepository
    ): Boolean {
        val membership = membershipRepository.getMembership(userId, orgId)
        return membership?.isDriver == true
    }
    
    /**
     * Get user role display name
     */
    suspend fun getUserRoleDisplayName(
        userId: String,
        orgId: String,
        membershipRepository: MembershipRepository
    ): String {
        val membership = membershipRepository.getMembership(userId, orgId)
        return membership?.userRole?.displayName ?: "Unknown"
    }
    
    /**
     * Validate if user has required role for action
     */
    suspend fun validateUserRole(
        userId: String,
        orgId: String,
        requiredRole: UserRole,
        membershipRepository: MembershipRepository
    ): Boolean {
        val membership = membershipRepository.getMembership(userId, orgId)
        return when (requiredRole) {
            UserRole.ADMIN -> membership?.isAdmin == true
            UserRole.MANAGER -> membership?.isManager == true || membership?.isAdmin == true
            UserRole.DRIVER -> membership?.isDriver == true || membership?.isManager == true || membership?.isAdmin == true
        }
    }
}
