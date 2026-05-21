package com.baranov.cookbook.network.dto

import kotlinx.serialization.Serializable

@Serializable
data class RecipeDto(
    val id: Int,
    val authorId: Int,
    val title: String,
    val description: String?,
    val cookingInstructions: String,
    val photo: String?
)

@Serializable
data class RecipeWithDetailsDto(
    val recipe: RecipeDto,
    val products: List<RecipeProductDto>,
    val accessibleToUsers: List<Int>
)

@Serializable
data class RecipeProductDto(
    val productId: Int,
    val quantity: Double
)

@Serializable
data class ProductDto(
    val id: Int,
    val name: String,
    val measurementUnit: String
)

@Serializable
data class UserDto(
    val id: Int,
    val name: String,
    val email: String,
    val passwordHash: String,
    val photo: String?
)

@Serializable
data class CreateRecipeRequest(
    val authorId: Int,
    val title: String,
    val description: String?,
    val cookingInstructions: String,
    val photo: String?,
    val products: List<ProductInRecipeRequest>,
    val accessibleUserIds: List<Int>
)

@Serializable
data class ProductInRecipeRequest(
    val productId: Int,
    val quantity: Double
)

@Serializable
data class UpdateRecipeRequest(
    val title: String? = null,
    val description: String? = null,
    val cookingInstructions: String? = null,
    val photo: String? = null
)

@Serializable
data class AddProductToRecipeRequest(
    val productId: Int,
    val quantity: Double
)

@Serializable
data class UpdateProductQuantityRequest(
    val quantity: Double
)