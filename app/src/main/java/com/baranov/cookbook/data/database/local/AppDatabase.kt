package com.baranov.cookbook.data.database.local

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import com.baranov.cookbook.data.database.local.dao.ProductDao
import com.baranov.cookbook.data.database.local.dao.RecipeDao
import com.baranov.cookbook.data.database.local.dao.RecipeProductDao
import com.baranov.cookbook.data.database.local.entity.LocalProductEntity
import com.baranov.cookbook.data.database.local.entity.LocalRecipeEntity
import com.baranov.cookbook.data.database.local.entity.LocalRecipeProductEntity

@Database(
    entities = [
        LocalRecipeEntity::class,
        LocalProductEntity::class,
        LocalRecipeProductEntity::class
    ],
    version = 2,
    exportSchema = false
)
abstract class AppDatabase : RoomDatabase() {
    abstract fun recipeDao(): RecipeDao
    abstract fun productDao(): ProductDao
    abstract fun recipeProductDao(): RecipeProductDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        fun getInstance(context: Context): AppDatabase {
            return INSTANCE ?: synchronized(this) {
                val instance = Room.databaseBuilder(
                    context.applicationContext,
                    AppDatabase::class.java,
                    "cookbook_local_db"
                )
                    .fallbackToDestructiveMigration()
                    .build()
                INSTANCE = instance
                instance
            }
        }
    }
}