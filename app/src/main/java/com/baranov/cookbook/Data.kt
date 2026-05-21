package com.baranov.cookbook

import androidx.compose.runtime.mutableStateListOf

data class Recipe(
    val id: Int,
    val imageRes: Int,
    val title: String,
    val description: String,
    val longDescription: String
)

object RecipeRepository {
    private val _recipes = mutableStateListOf<Recipe>()
    val recipes: List<Recipe> = _recipes

    fun addRecipe(recipe: Recipe) {
        _recipes.add(recipe)
    }

    fun updateRecipe(recipe: Recipe) {
        val index = _recipes.indexOfFirst { it.id == recipe.id }
        if (index != -1) {
            _recipes[index] = recipe
        }
    }

    fun getRecipeById(id: Int): Recipe? = _recipes.find { it.id == id }

    fun initialize() {
        if (_recipes.isEmpty()) {
            _recipes.addAll(
                listOf(
                    Recipe(1, R.drawable.placeholder1, "Борщ", "Борщ – это первое блюдо", "Полное описание борща..."),
                    Recipe(2, R.drawable.placeholder2, "Оливье", "Салат оливье — король салатов", "Полное описание оливье..."),
                    Recipe(3, R.drawable.placeholder3, "Пельмени", "Много мяса, мало теста", "Полное описание пельменей..."),
                    Recipe(4, R.drawable.placeholder4, "Сыр", "Я люблю Сыр", "Полное описание сыра..."),
                    Recipe(5, R.drawable.placeholder5, "Кирпич", "Я люблю Кирпич", "Полное описание кирпича..."),
                    Recipe(6, R.drawable.placeholder6, "Дубовик", "Я люблю Дубовик", "Полное описание дубовика...")
                )
            )
        }
    }
}