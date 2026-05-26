package com.baranov.cookbook.localDB.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "local_recipes",
    indices = [
        Index(value = ["ownerUserId"]),
        Index(value = ["serverId"])
    ]
)
data class LocalRecipeEntity(
    @PrimaryKey(autoGenerate = true)
    val localId: Long = 0,
    val serverId: Int? = null,
    val ownerUserId: Int? = null,
    val authorId: Int? = null,
    val title: String,
    val description: String?,
    val cookingInstructions: String,
    val photo: String?,
    val createdAt: Long,
    val updatedAt: Long,
    val syncedAt: Long = 0
)