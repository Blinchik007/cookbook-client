package com.baranov.cookbook

import android.annotation.SuppressLint
import android.content.Context
import com.baranov.cookbook.data.UserPreferences
import com.baranov.cookbook.data.database.local.AppDatabase
import com.baranov.cookbook.data.database.local.LocalRecipesRepository
import com.baranov.cookbook.data.database.local.ShoppingListRepository

object AppContainer {
    lateinit var repository: LocalRecipesRepository
        private set
    lateinit var shoppingListRepository: ShoppingListRepository
        private set
    @SuppressLint("StaticFieldLeak")
    lateinit var userPreferences: UserPreferences
        private set

    fun init(context: Context) {
        val db = AppDatabase.getInstance(context)
        repository = LocalRecipesRepository(
            recipeDao = db.recipeDao(),
            productDao = db.productDao(),
            recipeProductDao = db.recipeProductDao()
        )
        shoppingListRepository = ShoppingListRepository(
            dao = db.shoppingListDao()
        )
        userPreferences = UserPreferences(context.applicationContext)
    }
}