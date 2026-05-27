package com.baranov.cookbook.data.database.local.dao

import androidx.room.*
import com.baranov.cookbook.data.database.local.entity.ShoppingListItemEntity
import kotlinx.coroutines.flow.Flow

@Dao
interface ShoppingListDao {
    /**
     * Все строки списка для конкретного пользователя (или гостя если ownerUserId == null).
     * IS используется чтобы корректно сравнивать с null — то же что в RecipeDao.
     * Сортировка: по времени добавления (старые сверху, новые снизу).
     */
    @Query("SELECT * FROM shopping_list_items WHERE ownerUserId IS :ownerUserId ORDER BY addedAt ASC")
    fun getItemsForUser(ownerUserId: Int?): Flow<List<ShoppingListItemEntity>>

    @Insert
    suspend fun insertItem(item: ShoppingListItemEntity): Long

    @Update
    suspend fun updateItem(item: ShoppingListItemEntity)

    @Delete
    suspend fun deleteItem(item: ShoppingListItemEntity)

    /**
     * Удаление всех отмеченных строк для конкретного пользователя.
     * Вызывается из TopAppBar "удалить отмеченные".
     */
    @Query("DELETE FROM shopping_list_items WHERE ownerUserId IS :ownerUserId AND checked = 1")
    suspend fun deleteCheckedForUser(ownerUserId: Int?)

    /**
     * Массовая вставка — используется при экспорте ингредиентов из рецепта.
     */
    @Insert
    suspend fun insertAll(items: List<ShoppingListItemEntity>)
}