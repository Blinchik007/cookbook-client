package com.baranov.cookbook.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.unit.dp
import com.baranov.cookbook.data.database.local.entity.LocalProductEntity

/**
 * Inline-редактор строки ингредиента.
 *
 * Состояния:
 * - пользователь печатает → показываются совпадения + кнопка "Создать новый"
 * - продукт выбран → поле имени блокируется, активируется поле количества
 * - заполнено всё → кнопка ✓ активна
 *
 * @param query текст в поле имени
 * @param onQueryChange изменение текста
 * @param suggestions список найденных продуктов
 * @param selectedProductName если продукт выбран — его имя (для отображения единицы)
 * @param selectedMeasurementUnit единица выбранного продукта
 * @param quantityText текст в поле количества
 * @param onQuantityChange изменение количества
 * @param canConfirm можно ли подтвердить (продукт выбран + количество валидное)
 * @param onSelectSuggestion выбор продукта из подсказок
 * @param onCreateNew нажатие "Создать новый продукт"
 * @param onConfirm подтверждение
 * @param onCancel отмена
 */
@Composable
fun IngredientEditorRow(
    query: String,
    onQueryChange: (String) -> Unit,
    suggestions: List<LocalProductEntity>,
    selectedProductName: String?,
    selectedMeasurementUnit: String?,
    quantityText: String,
    onQuantityChange: (String) -> Unit,
    canConfirm: Boolean,
    onSelectSuggestion: (LocalProductEntity) -> Unit,
    onCreateNew: () -> Unit,
    onConfirm: () -> Unit,
    onCancel: () -> Unit,
    modifier: Modifier = Modifier
) {
    val isProductSelected = selectedProductName != null

    Surface(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(8.dp),
        color = MaterialTheme.colorScheme.surface,
        tonalElevation = 2.dp
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            // Верхний ряд: название/выбранный продукт + количество + кнопки
            Row(verticalAlignment = Alignment.CenterVertically) {
                OutlinedTextField(
                    value = if (isProductSelected) selectedProductName!! else query,
                    onValueChange = { if (!isProductSelected) onQueryChange(it) },
                    label = { Text("Продукт") },
                    singleLine = true,
                    enabled = !isProductSelected,
                    modifier = Modifier.weight(1f)
                )
                Spacer(modifier = Modifier.width(8.dp))
                OutlinedTextField(
                    value = quantityText,
                    onValueChange = onQuantityChange,
                    label = { Text(selectedMeasurementUnit ?: "Кол-во") },
                    singleLine = true,
                    enabled = isProductSelected,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Decimal),
                    modifier = Modifier.width(110.dp)
                )
            }

            Spacer(modifier = Modifier.height(8.dp))

            // Подсказки (только если продукт ещё не выбран и есть текст)
            if (!isProductSelected && query.isNotBlank()) {
                Column(
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(6.dp))
                        .background(MaterialTheme.colorScheme.surfaceVariant)
                        .padding(4.dp)
                ) {
                    suggestions.take(5).forEach { product ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onSelectSuggestion(product) }
                                .padding(horizontal = 8.dp, vertical = 6.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = product.name,
                                style = MaterialTheme.typography.bodyMedium,
                                modifier = Modifier.weight(1f)
                            )
                            Text(
                                text = product.measurementUnit,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable { onCreateNew() }
                            .padding(horizontal = 8.dp, vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            imageVector = Icons.Default.Add,
                            contentDescription = null,
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Создать \"$query\"",
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.primary,
                            fontWeight = FontWeight.Medium
                        )
                    }
                }
                Spacer(modifier = Modifier.height(8.dp))
            }

            // Кнопки действий
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.End
            ) {
                TextButton(onClick = onCancel) {
                    Icon(Icons.Default.Close, contentDescription = "Отмена")
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Отмена")
                }
                Spacer(modifier = Modifier.width(8.dp))
                Button(
                    onClick = onConfirm,
                    enabled = canConfirm
                ) {
                    Icon(Icons.Default.Check, contentDescription = "Готово")
                    Spacer(modifier = Modifier.width(4.dp))
                    Text("Готово")
                }
            }
        }
    }
}