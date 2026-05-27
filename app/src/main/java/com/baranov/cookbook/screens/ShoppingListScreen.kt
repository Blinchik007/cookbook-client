package com.baranov.cookbook.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.baranov.cookbook.AppContainer
import com.baranov.cookbook.CurrentUserHolder
import com.baranov.cookbook.data.database.local.entity.ShoppingListItemEntity
import kotlinx.coroutines.launch

/**
 * Экран списка покупок.
 *
 * Поведение:
 * - Чекбокс слева → toggle "куплено". Купленные остаются на месте, перечёркнуты, приглушённого цвета.
 * - Тап по тексту → inline-редактирование (TextField на месте Text). Tap вне или Enter → сохранить.
 * - Корзина справа → удалить строку без подтверждения.
 * - FAB "+" снизу справа → диалог добавления новой строки.
 *
 * Удаление всех отмеченных делается через action в TopAppBar родительского HomeScreen
 * (см. [shoppingListDeleteCheckedAction]).
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ShoppingListScreen() {
    val repository = AppContainer.shoppingListRepository
    val currentUserId = CurrentUserHolder.currentUser?.id
    val items by repository.getItemsForUser(currentUserId)
        .collectAsStateWithLifecycle(initialValue = emptyList())
    var showAddDialog by remember { mutableStateOf(false) }
    var editingItemId by remember { mutableStateOf<Long?>(null) }
    val scope = rememberCoroutineScope()

    Scaffold(
        floatingActionButton = {
            FloatingActionButton(onClick = { showAddDialog = true }) {
                Icon(Icons.Default.Add, contentDescription = "Добавить")
            }
        }
    ) { innerPadding ->
        if (items.isEmpty()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = "Список покупок пуст",
                    color = MaterialTheme.colorScheme.outline
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentPadding = PaddingValues(vertical = 8.dp)
            ) {
                items(items, key = { it.localId }) { item ->
                    ShoppingListRow(
                        item = item,
                        isEditing = editingItemId == item.localId,
                        onToggleChecked = {
                            scope.launch { repository.toggleChecked(item) }
                        },
                        onStartEditing = { editingItemId = item.localId },
                        onFinishEditing = { newText ->
                            editingItemId = null
                            val trimmed = newText.trim()
                            if (trimmed.isNotEmpty() && trimmed != item.text) {
                                scope.launch { repository.updateItem(item.copy(text = trimmed)) }
                            }
                        },
                        onDelete = {
                            scope.launch { repository.deleteItem(item) }
                        }
                    )
                }
            }
        }
    }

    if (showAddDialog) {
        AddItemDialog(
            onDismiss = { showAddDialog = false },
            onAdd = { text ->
                showAddDialog = false
                scope.launch { repository.addItem(currentUserId, text) }
            }
        )
    }
}

/**
 * Action-кнопка для TopAppBar — удалить все отмеченные.
 * Вызывается из HomeScreen, когда активна страница шоппинг-листа.
 *
 * Спрятана за отдельной функцией, потому что:
 *  - TopAppBar живёт в HomeScreen и общий для всех страниц,
 *  - но действие специфично для шоппинг-листа.
 */
@Composable
fun ShoppingListDeleteCheckedAction() {
    val repository = AppContainer.shoppingListRepository
    val currentUserId = CurrentUserHolder.currentUser?.id
    val items by repository.getItemsForUser(currentUserId)
        .collectAsStateWithLifecycle(initialValue = emptyList())
    val hasChecked = items.any { it.checked }
    val scope = rememberCoroutineScope()

    IconButton(
        onClick = {
            scope.launch { repository.deleteCheckedForUser(currentUserId) }
        },
        enabled = hasChecked
    ) {
        Icon(
            imageVector = Icons.Default.Delete,
            contentDescription = "Удалить отмеченные"
        )
    }
}

@Composable
private fun ShoppingListRow(
    item: ShoppingListItemEntity,
    isEditing: Boolean,
    onToggleChecked: () -> Unit,
    onStartEditing: () -> Unit,
    onFinishEditing: (String) -> Unit,
    onDelete: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 8.dp, vertical = 2.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Checkbox(
            checked = item.checked,
            onCheckedChange = { onToggleChecked() }
        )

        if (isEditing) {
            // Inline-редактор: TextField на месте Text.
            // Локальное состояние ввода, отдаётся наружу через onFinishEditing.
            var draft by remember(item.localId) { mutableStateOf(item.text) }
            val focusRequester = remember { FocusRequester() }

            OutlinedTextField(
                value = draft,
                onValueChange = { draft = it },
                modifier = Modifier
                    .weight(1f)
                    .focusRequester(focusRequester)
                    .onFocusChanged { focusState ->
                        // Если поле потеряло фокус — сохраняем.
                        if (!focusState.isFocused && isEditing) {
                            onFinishEditing(draft)
                        }
                    },
                singleLine = true,
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = { onFinishEditing(draft) })
            )

            // Автофокус при появлении в режиме редактирования.
            LaunchedEffect(item.localId) {
                focusRequester.requestFocus()
            }
        } else {
            Text(
                text = item.text,
                style = MaterialTheme.typography.bodyLarge.copy(
                    textDecoration = if (item.checked) TextDecoration.LineThrough else TextDecoration.None
                ),
                color = if (item.checked) MaterialTheme.colorScheme.outline
                else MaterialTheme.colorScheme.onSurface,
                modifier = Modifier
                    .weight(1f)
                    .clickableNoIndication { onStartEditing() }
                    .padding(vertical = 12.dp)
            )
        }

        IconButton(onClick = onDelete) {
            Icon(
                imageVector = Icons.Default.Delete,
                contentDescription = "Удалить",
                tint = MaterialTheme.colorScheme.outline
            )
        }
    }
}

@Composable
private fun AddItemDialog(
    onDismiss: () -> Unit,
    onAdd: (String) -> Unit
) {
    var text by remember { mutableStateOf("") }
    val focusRequester = remember { FocusRequester() }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Новый пункт") },
        text = {
            OutlinedTextField(
                value = text,
                onValueChange = { text = it },
                placeholder = { Text("Например: Хлеб") },
                singleLine = true,
                modifier = Modifier.focusRequester(focusRequester),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = {
                    if (text.isNotBlank()) onAdd(text)
                })
            )
        },
        confirmButton = {
            TextButton(
                onClick = { if (text.isNotBlank()) onAdd(text) },
                enabled = text.isNotBlank()
            ) {
                Text("Добавить")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Отмена")
            }
        }
    )

    // Автофокус на поле ввода при открытии диалога.
    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }
}

/**
 * Тап без ripple-эффекта — для текста, который превращается в TextField по тапу.
 */
@Composable
private fun Modifier.clickableNoIndication(onClick: () -> Unit): Modifier {
    val interactionSource = remember { androidx.compose.foundation.interaction.MutableInteractionSource() }
    return this.clickable(
        interactionSource = interactionSource,
        indication = null,
        onClick = onClick
    )
}