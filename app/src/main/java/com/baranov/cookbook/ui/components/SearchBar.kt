package com.baranov.cookbook.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.platform.LocalSoftwareKeyboardController
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.unit.dp

/**
 * Переиспользуемая поисковая строка с опциональным dropdown'ом истории.
 *
 * UI-only: компонент не знает откуда данные и кто выполняет поиск.
 * Дебаунс/выполнение запроса — снаружи (родитель слушает onQueryChange).
 *
 * История показывается когда:
 *  - поле в фокусе,
 *  - поле пустое,
 *  - список historyItems не пуст.
 *
 * @param query текущий текст в поле.
 * @param onQueryChange вызывается при каждом изменении текста.
 * @param onSearch вызывается при тапе Enter/Done на клавиатуре.
 * @param historyItems элементы истории; если пуст — dropdown не рисуется.
 * @param onPickHistory вызывается при тапе на элемент истории. Родитель должен подставить
 *   значение в query и (обычно) автоматически инициировать поиск.
 * @param onClearHistory вызывается при тапе "Очистить историю". null — кнопка не показывается.
 */
@Composable
fun SearchBar(
    query: String,
    onQueryChange: (String) -> Unit,
    onSearch: (String) -> Unit = {},
    placeholder: String = "Поиск",
    historyItems: List<String> = emptyList(),
    onPickHistory: (String) -> Unit = {},
    onClearHistory: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val keyboardController = LocalSoftwareKeyboardController.current
    var isFocused by remember { mutableStateOf(false) }

    val showHistory = isFocused && query.isEmpty() && historyItems.isNotEmpty()

    Column(modifier = modifier) {
        OutlinedTextField(
            value = query,
            onValueChange = onQueryChange,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp)
                .onFocusChanged { isFocused = it.isFocused },
            placeholder = { Text(placeholder) },
            leadingIcon = {
                Icon(Icons.Default.Search, contentDescription = null)
            },
            trailingIcon = {
                // Кнопка "Очистить" видна только если есть текст — по требованию курсовой.
                if (query.isNotEmpty()) {
                    IconButton(onClick = {
                        onQueryChange("")
                        keyboardController?.hide()
                    }) {
                        Icon(Icons.Default.Clear, contentDescription = "Очистить")
                    }
                }
            },
            singleLine = true,
            keyboardOptions = KeyboardOptions(imeAction = ImeAction.Search),
            keyboardActions = KeyboardActions(onSearch = {
                onSearch(query)
                keyboardController?.hide()
            })
        )

        // Dropdown с историей. Показывается только при пустом фокусированном поле.
        if (showHistory) {
            Surface(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp),
                shape = RoundedCornerShape(8.dp),
                color = MaterialTheme.colorScheme.surfaceVariant,
                tonalElevation = 2.dp
            ) {
                Column {
                    historyItems.forEach { item ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable {
                                    onPickHistory(item)
                                    keyboardController?.hide()
                                }
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.History,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(Modifier.width(12.dp))
                            Text(
                                text = item,
                                style = MaterialTheme.typography.bodyMedium
                            )
                        }
                    }

                    // Кнопка "Очистить историю" — последним пунктом.
                    if (onClearHistory != null) {
                        HorizontalDivider()
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clickable { onClearHistory() }
                                .padding(horizontal = 16.dp, vertical = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Icon(
                                imageVector = Icons.Default.Clear,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.error,
                                modifier = Modifier.size(20.dp)
                            )
                            Spacer(Modifier.width(12.dp))
                            Text(
                                text = "Очистить историю",
                                style = MaterialTheme.typography.bodyMedium,
                                color = MaterialTheme.colorScheme.error
                            )
                        }
                    }
                }
            }
        }
    }
}