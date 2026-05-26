package com.baranov.cookbook.localDB.dao

import androidx.room.*
import com.baranov.cookbook.localDB.entity.LocalRecipeProductEntity

@Dao
interface RecipeProductDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insert(recipeProduct: LocalRecipeProductEntity)

    @Query("SELECT * FROM local_recipe_products WHERE recipeLocalId = :recipeLocalId")
    suspend fun getProductsForRecipe(recipeLocalId: Long): List<LocalRecipeProductEntity>

    @Query("DELETE FROM local_recipe_products WHERE recipeLocalId = :recipeLocalId")
    suspend fun deleteByRecipeId(recipeLocalId: Long)

    @Query("DELETE FROM local_recipe_products WHERE recipeLocalId = :recipeLocalId AND productLocalId = :productLocalId")
    suspend fun deleteProductFromRecipe(recipeLocalId: Long, productLocalId: Long)
}