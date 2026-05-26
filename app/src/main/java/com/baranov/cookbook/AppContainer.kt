package com.baranov.cookbook

import android.content.Context
import com.baranov.cookbook.localDB.AppDatabase
import com.baranov.cookbook.localDB.LocalRecipesRepository

object AppContainer {
    lateinit var repository: LocalRecipesRepository
        private set

    fun init(context: Context) {
        val db = AppDatabase.getInstance(context)
        repository = LocalRecipesRepository(
            recipeDao = db.recipeDao(),
            productDao = db.productDao(),
            recipeProductDao = db.recipeProductDao()
        )
    }
}