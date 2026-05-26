package com.baranov.cookbook.data.database.local.entity

import androidx.room.Entity

@Entity(
    tableName = "local_recipe_products",
    primaryKeys = ["recipeLocalId", "productLocalId"]
)
data class LocalRecipeProductEntity(
    val recipeLocalId: Long,
    val productLocalId: Long,
    val quantity: Double
)