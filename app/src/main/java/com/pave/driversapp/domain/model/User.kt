package com.pave.driversapp.domain.model

data class User(
    val userID: String,
    val name: String,
    val phoneNumber: String,
    val orgID: String,
    val orgName: String,
    val role: Int // 0=admin, 1=manager, 2=driver
)

data class Organization(
    val orgID: String,
    val orgName: String
)

data class AuthResult(
    val isSuccess: Boolean,
    val user: User? = null,
    val organizations: List<Organization> = emptyList(),
    val errorMessage: String? = null
)

