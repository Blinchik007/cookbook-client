package com.baranov.cookbook.data.database.remote.dto

import kotlinx.serialization.Serializable

@Serializable
data class UserProfileResponse(
    val id: Int,
    val name: String,
    val email: String,
    val photo: String?
)

@Serializable
data class CreateUserRequest(
    val name: String,
    val email: String,
    val password: String,
    val photo: String? = null
)

@Serializable
data class UpdateUserRequest(
    val name: String? = null,
    val email: String? = null,
    val passwordHash: String? = null,
    val photo: String? = null
)

@Serializable
data class LoginRequest(
    val email: String,
    val password: String
)