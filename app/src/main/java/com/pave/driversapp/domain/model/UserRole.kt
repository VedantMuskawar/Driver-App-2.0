package com.pave.driversapp.domain.model

import kotlinx.serialization.Serializable
import kotlinx.serialization.Contextual

/**
 * User roles in the system
 */
enum class UserRole(val value: Int, val displayName: String) {
    ADMIN(0, "Admin"),
    MANAGER(1, "Manager"), 
    DRIVER(2, "Driver");
    
    companion object {
        fun fromValue(value: Int): UserRole? {
            return values().find { it.value == value }
        }
    }
}

/**
 * User membership data from MEMBERSHIP collection
 */
@Serializable
data class Membership(
    val userId: String,
    val orgId: String,
    val role: Int, // 0=Admin, 1=Manager, 2=Driver
    val vehicleId: String? = null, // Only for drivers
    val vehicleNumber: String? = null, // Only for drivers
    val isActive: Boolean = true,
    @Contextual val createdAt: com.google.firebase.Timestamp? = null,
    @Contextual val updatedAt: com.google.firebase.Timestamp? = null
) {
    val userRole: UserRole
        get() = UserRole.fromValue(role) ?: UserRole.DRIVER
    
    val isAdmin: Boolean
        get() = role == 0
        
    val isManager: Boolean
        get() = role == 1
        
    val isDriver: Boolean
        get() = role == 2
}

/**
 * Access control permissions
 */
data class AccessPermissions(
    val canAccessDepotSettings: Boolean,
    val canAccessAllOrders: Boolean,
    val canAccessSettings: Boolean,
    val canManageUsers: Boolean,
    val canViewReports: Boolean,
    val canDispatchTrips: Boolean,
    val canCancelTrips: Boolean,
    val vehicleId: String? = null // For drivers - their assigned vehicle
) {
    companion object {
        fun forRole(role: UserRole, vehicleId: String? = null): AccessPermissions {
            return when (role) {
                UserRole.ADMIN -> AccessPermissions(
                    canAccessDepotSettings = true,
                    canAccessAllOrders = true,
                    canAccessSettings = true,
                    canManageUsers = true,
                    canViewReports = true,
                    canDispatchTrips = true,
                    canCancelTrips = true
                )
                UserRole.MANAGER -> AccessPermissions(
                    canAccessDepotSettings = false, // Managers cannot access depot settings
                    canAccessAllOrders = true,
                    canAccessSettings = true,
                    canManageUsers = false,
                    canViewReports = true,
                    canDispatchTrips = true,
                    canCancelTrips = true
                )
                UserRole.DRIVER -> AccessPermissions(
                    canAccessDepotSettings = false,
                    canAccessAllOrders = false, // Drivers only see their vehicle's orders
                    canAccessSettings = false,
                    canManageUsers = false,
                    canViewReports = false,
                    canDispatchTrips = true,
                    canCancelTrips = true,
                    vehicleId = vehicleId
                )
            }
        }
    }
}
