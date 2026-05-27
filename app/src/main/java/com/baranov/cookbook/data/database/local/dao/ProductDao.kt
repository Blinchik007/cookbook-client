package com.baranov.cookbook.data.database.local.dao

import androidx.room.*
import com.baranov.cookbook.data.database.local.entity.LocalProductEntity

@Dao
interface ProductDao {
    @Query("SELECT * FROM local_products WHERE serverId = :serverId")
    suspend fun getProductByServerId(serverId: Int): LocalProductEntity?

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertProduct(product: LocalProductEntity): Long

    @Update
    suspend fun updateProduct(product: LocalProductEntity)

    @Query("SELECT * FROM local_products WHERE localId = :localId")
    suspend fun getProductByLocalId(localId: Long): LocalProductEntity?

    @Query("SELECT * FROM local_products ORDER BY name ASC")
    suspend fun getAllProducts(): List<LocalProductEntity>

    @Query("SELECT * FROM local_products WHERE name LIKE '%' || :query || '%' ORDER BY name ASC")
    suspend fun searchProducts(query: String): List<LocalProductEntity>



}