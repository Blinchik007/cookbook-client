package com.baranov.cookbook

import android.annotation.SuppressLint
import android.content.Context
import com.baranov.cookbook.data.SearchHistory
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
    @SuppressLint("StaticFieldLeak")
    lateinit var searchHistory: SearchHistory
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
        searchHistory = SearchHistory(context.applicationContext)
    }

    /**
     * Вызывать при любом изменении состояния авторизации:
     * — login (включая регистрацию),
     * — logout.
     * Чистит данные, привязанные к сеансу пользователя (на данный момент — историю поиска).
     */
    suspend fun onAuthChanged() {
        searchHistory.clear()
    }
}