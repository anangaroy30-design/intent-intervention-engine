package com.intent.app

data class AuthRequest(
    val email: String,
    val password: String
)

data class AuthUser(
    val id: Long,
    val email: String,
    val createdAt: String
)

data class AuthResponse(
    val user: AuthUser,
    val token: String
)

data class MeResponse(
    val user: AuthUser
)

data class ApiError(
    val error: String?
)
