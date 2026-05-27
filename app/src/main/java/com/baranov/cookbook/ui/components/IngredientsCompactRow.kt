package com.baranov.cookbook.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

/**
 * Один элемент компактного списка ингредиентов.
 * Отображается как "Имя количество единица" (например: "Мука 200 г").
 */
data class IngredientDisplayItem(
    val name: String,
    val quantity: Double,
    val measurementUnit: String
)

/**
 * Компактный inline-список ингредиентов через middle-dot (·).
 * Используется в развёрнутой [RecipeCard].
 *
 * Пример отображения: "Мука 200 г · Молоко 500 мл · Яйцо 2 шт"
 *
 * Если список пустой — рендерит placeholder "Без ингредиентов".
 */
@Composable
fun IngredientsCompactRow(
    items: List<IngredientDisplayItem>,
    modifier: Modifier = Modifier
) {
    if (items.isEmpty()) {
        Text(
            text = "Без ингредиентов",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.outline,
            modifier = modifier
        )
        return
    }
    Text(
        text = items.joinToString(separator = " · ") { item ->
            "${item.name} ${formatCompactQuantity(item.quantity)} ${item.measurementUnit}".trim()
        },
        style = MaterialTheme.typography.bodySmall,
        color = MaterialTheme.colorScheme.onSurfaceVariant,
        modifier = modifier
    )
}

/**
 * Форматирует количество: целое без точки, дробное — до 2 знаков, без хвостовых нулей.
 * Логика та же, что в IngredientRow.kt — стоит вынести в общий util, но пока дублируем.
 */
private fun formatCompactQuantity(quantity: Double): String {
    return if (quantity % 1.0 == 0.0) {
        quantity.toInt().toString()
    } else {
        "%.2f".format(quantity).trimEnd('0').trimEnd(',').trimEnd('.')
    }
}