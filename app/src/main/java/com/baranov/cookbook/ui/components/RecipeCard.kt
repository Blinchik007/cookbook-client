package com.baranov.cookbook.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ExpandLess
import androidx.compose.material.icons.filled.ExpandMore
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import coil.request.ImageRequest

/**
 * Карточка рецепта в списке.
 *
 * Поведение:
 * - Тап по всей карточке (кроме стрелочки) → [onClick]. Используется родителем, чтобы открыть экран просмотра.
 * - Тап по стрелочке слева внизу → разворачивает/сворачивает превью (description + ингредиенты).
 * - Долгий тап по карточке → [onLongClick] (меню действий).
 *
 * Состояние "развёрнуто/свёрнуто" контролируется снаружи через [expanded] + [onToggleExpand],
 * чтобы родитель мог следить за инвариантом "только одна развёрнута за раз".
 *
 * Ингредиенты загружаются лениво при первом разворачивании через [loadIngredients].
 * Результат кешируется внутри карточки — повторное разворачивание не делает новый запрос.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun RecipeCard(
    title: String,
    description: String?,
    photoBase64: String?,
    badge: String? = null,
    expanded: Boolean,
    onClick: () -> Unit,
    onToggleExpand: () -> Unit,
    onLongClick: (() -> Unit)? = null,
    loadIngredients: (suspend () -> List<IngredientDisplayItem>)? = null
) {
    val context = LocalContext.current
    val photoBytes = remember(photoBase64) {
        if (photoBase64.isNullOrBlank()) null
        else runCatching {
            android.util.Base64.decode(photoBase64, android.util.Base64.DEFAULT)
        }.getOrNull()
    }

    // Состояние ленивой загрузки ингредиентов. Сбрасывается, если карточка переиспользована под другой рецепт
    // (loadIngredients — новая лямбда, ключ remember поменяется).
    var ingredients by remember(loadIngredients) { mutableStateOf<List<IngredientDisplayItem>?>(null) }
    var ingredientsLoading by remember(loadIngredients) { mutableStateOf(false) }

    // Запускаем загрузку при первом разворачивании. Повторно не запускаем (есть кеш).
    LaunchedEffect(expanded, loadIngredients) {
        if (expanded && ingredients == null && loadIngredients != null && !ingredientsLoading) {
            ingredientsLoading = true
            ingredients = runCatching { loadIngredients() }.getOrDefault(emptyList())
            ingredientsLoading = false
        }
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .combinedClickable(
                onClick = onClick,
                onLongClick = onLongClick
            ),
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(4.dp)
    ) {
        Column {
            if (photoBytes != null) {
                AsyncImage(
                    model = ImageRequest.Builder(context)
                        .data(photoBytes)
                        .size(800, 360)   // защита от OOM: Coil сразу даунскейлит при декодировании
                        .crossfade(true)
                        .build(),
                    contentDescription = title,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(180.dp)
                )
            }
            Column(modifier = Modifier.padding(start = 16.dp, end = 16.dp, top = 16.dp, bottom = 4.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = title,
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.weight(1f)
                    )
                    if (badge != null) {
                        Text(
                            text = badge,
                            style = MaterialTheme.typography.labelSmall,
                            color = MaterialTheme.colorScheme.outline
                        )
                    }
                }

                AnimatedVisibility(visible = expanded) {
                    Column(modifier = Modifier.padding(top = 8.dp)) {
                        if (!description.isNullOrBlank()) {
                            Text(
                                text = description,
                                style = MaterialTheme.typography.bodyMedium
                            )
                            Spacer(modifier = Modifier.height(8.dp))
                        }
                        when {
                            ingredientsLoading -> {
                                Text(
                                    text = "Загрузка ингредиентов…",
                                    style = MaterialTheme.typography.bodySmall,
                                    color = MaterialTheme.colorScheme.outline
                                )
                            }
                            ingredients != null -> {
                                IngredientsCompactRow(items = ingredients!!)
                            }
                            // ingredients == null && !loading → ничего не показываем
                            // (loadIngredients не передан или ещё не успело запуститься)
                        }
                    }
                }

                // Кнопка-стрелочка слева внизу.
                // Свёрнуто → ExpandMore (⌄), развёрнуто → ExpandLess (⌃).
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = onToggleExpand,
                        modifier = Modifier.size(36.dp)
                    ) {
                        Icon(
                            imageVector = if (expanded) Icons.Default.ExpandLess else Icons.Default.ExpandMore,
                            contentDescription = if (expanded) "Свернуть" else "Развернуть",
                            tint = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}