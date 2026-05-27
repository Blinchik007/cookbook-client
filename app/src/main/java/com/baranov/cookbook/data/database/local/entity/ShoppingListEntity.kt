package com.baranov.cookbook.data.database.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

/**
 * Одна строка в списке покупок.
 * Текст полностью свободный (например "Мука 100 г" или "Что-то очень нужное").
 * Принадлежит конкретному пользователю (или null = гость, как у рецептов).
 */
@Entity(
    tableName = "shopping_list_items",
    indices = [Index(value = ["ownerUserId"])]
)
data class ShoppingListItemEntity(
    @PrimaryKey(autoGenerate = true)
    val localId: Long = 0,
    val ownerUserId: Int? = null,
    val text: String,
    val checked: Boolean = false,
    val addedAt: Long
)