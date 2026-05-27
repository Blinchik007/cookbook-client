package com.baranov.cookbook.data.database.local

import com.baranov.cookbook.data.database.local.dao.ShoppingListDao
import com.baranov.cookbook.data.database.local.entity.ShoppingListItemEntity
import kotlinx.coroutines.flow.Flow

/**
 * Репозиторий для списка покупок (Shopping List).
 *
 * Список локальный, без сервера — синхронизации нет.
 * Каждая строка принадлежит конкретному пользователю (или гостю если ownerUserId == null).
 *
 * Текст строки — свободный (например "Мука 100 г" после экспорта из рецепта,
 * либо что пользователь ввёл вручную).
 */
class ShoppingListRepository(
    private val dao: ShoppingListDao
) {
    fun getItemsForUser(ownerUserId: Int?): Flow<List<ShoppingListItemEntity>> =
        dao.getItemsForUser(ownerUserId)

    suspend fun addItem(ownerUserId: Int?, text: String) {
        val trimmed = text.trim()
        if (trimmed.isEmpty()) return
        dao.insertItem(
            ShoppingListItemEntity(
                ownerUserId = ownerUserId,
                text = trimmed,
                checked = false,
                addedAt = System.currentTimeMillis()
            )
        )
    }

    /**
     * Массовое добавление — используется при экспорте ингредиентов из рецепта.
     * Все строки получают разный addedAt с микро-смещением, чтобы сохранить порядок.
     */
    suspend fun addItems(ownerUserId: Int?, texts: List<String>) {
        if (texts.isEmpty()) return
        val now = System.currentTimeMillis()
        val items = texts.mapIndexedNotNull { index, raw ->
            val trimmed = raw.trim()
            if (trimmed.isEmpty()) return@mapIndexedNotNull null
            ShoppingListItemEntity(
                ownerUserId = ownerUserId,
                text = trimmed,
                checked = false,
                // index-смещение, чтобы порядок в ORDER BY addedAt ASC соответствовал порядку в рецепте
                addedAt = now + index
            )
        }
        dao.insertAll(items)
    }

    suspend fun updateItem(item: ShoppingListItemEntity) = dao.updateItem(item)

    suspend fun toggleChecked(item: ShoppingListItemEntity) =
        dao.updateItem(item.copy(checked = !item.checked))

    suspend fun deleteItem(item: ShoppingListItemEntity) = dao.deleteItem(item)

    suspend fun deleteCheckedForUser(ownerUserId: Int?) = dao.deleteCheckedForUser(ownerUserId)
}