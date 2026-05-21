package com.baranov.cookbook.network.dto

import kotlinx.serialization.Serializable

@Serializable
data class CreateUserRequest(
    val name: String,
    val email: String,
    val passwordHash: String,
    val photo: String? = null
)

@Serializable
data class UpdateUserRequest(
    val name: String? = null,
    val email: String? = null,
    val passwordHash: String? = null,
    val photo: String? = null
)