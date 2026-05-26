package com.baranov.cookbook.localDB.dao

import androidx.room.*
import com.baranov.cookbook.localDB.entity.LocalProductEntity

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
}