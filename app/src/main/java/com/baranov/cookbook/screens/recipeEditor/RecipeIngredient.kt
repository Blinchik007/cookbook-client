package com.baranov.cookbook.screens.recipeEditor

/**
 * Подтверждённый ингредиент в списке рецепта.
 * Имя и единицу храним для отображения, чтобы не дёргать БД на каждую перерисовку.
 */
data class RecipeIngredient(
    val productLocalId: Long,
    val productServerId: Int,
    val productName: String,
    val measurementUnit: String,
    val quantity: Double
)

/**
 * Строка-редактор: либо добавление нового ингредиента, либо редактирование существующего.
 * editingIndex = -1 для нового, иначе индекс в списке.
 */
data class PendingIngredient(
    val editingIndex: Int = -1,
    val nameQuery: String = "",
    val selectedProductLocalId: Long? = null,
    val selectedProductServerId: Int? = null,
    val selectedProductName: String? = null,
    val selectedMeasurementUnit: String? = null,
    val quantityText: String = ""
) {
    val isReady: Boolean
        get() = selectedProductLocalId != null &&
                quantityText.toDoubleOrNull()?.let { it > 0 } == true
}

/**
 * Фиксированный набор единиц измерения для создания новых продуктов.
 */
object MeasurementUnits {
    const val GRAMS = "г"
    const val MILLILITERS = "мл"
    const val PIECES = "шт"

    val all = listOf(GRAMS, MILLILITERS, PIECES)
}