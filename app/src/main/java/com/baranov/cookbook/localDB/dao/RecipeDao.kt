package com.baranov.cookbook.localDB.dao

import androidx.room.*
import com.baranov.cookbook.localDB.entity.LocalRecipeEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface RecipeDao {
    @Query("SELECT * FROM local_recipes WHERE ownerUserId IS :ownerUserId ORDER BY updatedAt DESC")
    fun getRecipesByOwner(ownerUserId: Int?): Flow<List<LocalRecipeEntity>>

    @Query("SELECT * FROM local_recipes WHERE localId = :localId")
    suspend fun getRecipeByLocalId(localId: Long): LocalRecipeEntity?

    @Query("SELECT * FROM local_recipes WHERE serverId = :serverId AND ownerUserId IS :ownerUserId")
    suspend fun getRecipeByServerId(serverId: Int, ownerUserId: Int?): LocalRecipeEntity?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertRecipe(recipe: LocalRecipeEntity): Long

    @Update
    suspend fun updateRecipe(recipe: LocalRecipeEntity)

    @Delete
    suspend fun deleteRecipe(recipe: LocalRecipeEntity)

    @Query("SELECT * FROM local_recipes WHERE (serverId IS NULL OR serverId = -1) AND ownerUserId IS :ownerUserId")
    suspend fun getUnsyncedRecipes(ownerUserId: Int?): List<LocalRecipeEntity>

    @Query("UPDATE local_recipes SET ownerUserId = :newOwner WHERE ownerUserId IS NULL")
    suspend fun reassignGuestRecipes(newOwner: Int)
}