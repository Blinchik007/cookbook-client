package com.baranov.cookbook.data.database.local.entity

import androidx.room.Entity
import androidx.room.Index
import androidx.room.PrimaryKey

@Entity(
    tableName = "local_products",
    indices = [Index(value = ["serverId"], unique = true)]
)
data class LocalProductEntity(
    @PrimaryKey(autoGenerate = true)
    val localId: Long = 0,
    val serverId: Int,
    val name: String,
    val measurementUnit: String,
    val updatedAt: Long,
    val syncedAt: Long = 0
)