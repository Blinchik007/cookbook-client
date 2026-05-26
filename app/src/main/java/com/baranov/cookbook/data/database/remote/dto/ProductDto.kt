package com.baranov.cookbook.data.database.remote.dto

import kotlinx.serialization.Serializable


@Serializable
data class CreateProductRequest(
    val name: String,
    val measurementUnit: String
)

@Serializable
data class UpdateProductRequest(
    val name: String? = null,
    val measurementUnit: String? = null
)