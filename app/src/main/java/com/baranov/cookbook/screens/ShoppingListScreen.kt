package com.baranov.cookbook.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardActions
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.input.ImeAction
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.baranov.cookbook.AppContainer
import com.baranov.cookbook.CurrentUserHolder
import com.baranov.cookbook.data.database.local.entity.ShoppingListItemEntity
import kotlinx.coroutines.launch

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
    val scheme = MaterialTheme.colorScheme

    Scaffold(
        containerColor = Color.Transparent,
        floatingActionButton = {
            FloatingActionButton(
                onClick = { showAddDialog = true },
                containerColor = scheme.primary,
                contentColor = scheme.onPrimary
            ) {
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
                    style = MaterialTheme.typography.headlineMedium,
                    color = scheme.onSurfaceVariant
                )
            }
        } else {
            LazyColumn(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
                contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp)
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

@Composable
fun ShoppingListDeleteCheckedAction() {
    val repository = AppContainer.shoppingListRepository
    val currentUserId = CurrentUserHolder.currentUser?.id
    val items by repository.getItemsForUser(currentUserId)
        .collectAsStateWithLifecycle(initialValue = emptyList())
    val hasChecked = items.any { it.checked }
    val scope = rememberCoroutineScope()
    val scheme = MaterialTheme.colorScheme

    IconButton(
        onClick = {
            scope.launch { repository.deleteCheckedForUser(currentUserId) }
        },
        enabled = hasChecked
    ) {
        Icon(
            imageVector = Icons.Default.Delete,
            contentDescription = "Удалить отмеченные",
            tint = if (hasChecked) scheme.primary
            else scheme.onSurfaceVariant.copy(alpha = 0.4f)
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
    val scheme = MaterialTheme.colorScheme

    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                modifier = Modifier
                    .size(24.dp)
                    .clip(RoundedCornerShape(4.dp))
                    .border(1.5.dp, scheme.primary, RoundedCornerShape(4.dp))
                    .background(
                        if (item.checked) scheme.primary.copy(alpha = 0.15f)
                        else Color.Transparent
                    )
                    .clickable { onToggleChecked() },
                contentAlignment = Alignment.Center
            ) {
                if (item.checked) {
                    Icon(
                        imageVector = Icons.Default.Check,
                        contentDescription = "Отмечено",
                        tint = scheme.primary,
                        modifier = Modifier.size(18.dp)
                    )
                }
            }

            Spacer(Modifier.width(12.dp))

            if (isEditing) {
                var draft by remember(item.localId) { mutableStateOf(item.text) }
                val focusRequester = remember { FocusRequester() }

                TextField(
                    value = draft,
                    onValueChange = { draft = it },
                    modifier = Modifier
                        .weight(1f)
                        .focusRequester(focusRequester)
                        .onFocusChanged { focusState ->
                            if (!focusState.isFocused && isEditing) {
                                onFinishEditing(draft)
                            }
                        },
                    singleLine = true,
                    keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                    keyboardActions = KeyboardActions(onDone = { onFinishEditing(draft) }),
                    colors = TextFieldDefaults.colors(
                        focusedContainerColor = Color.Transparent,
                        unfocusedContainerColor = Color.Transparent,
                        focusedIndicatorColor = scheme.primary,
                        unfocusedIndicatorColor = scheme.outline,
                        cursorColor = scheme.primary
                    )
                )

                LaunchedEffect(item.localId) {
                    focusRequester.requestFocus()
                }
            } else {
                Text(
                    text = item.text,
                    style = MaterialTheme.typography.bodyLarge.copy(
                        textDecoration = if (item.checked) TextDecoration.LineThrough
                        else TextDecoration.None
                    ),
                    color = if (item.checked) scheme.onSurfaceVariant
                    else scheme.onBackground,
                    modifier = Modifier
                        .weight(1f)
                        .clickableNoIndication { onStartEditing() }
                        .padding(vertical = 12.dp)
                )
            }

            IconButton(
                onClick = onDelete,
                modifier = Modifier.size(36.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Close,
                    contentDescription = "Удалить",
                    tint = scheme.onSurfaceVariant,
                    modifier = Modifier.size(20.dp)
                )
            }
        }

        // Подчёркивающая линия под строкой
        HorizontalDivider(
            color = scheme.outline.copy(alpha = 0.4f),
            thickness = 1.dp
        )
    }
}

@Composable
private fun AddItemDialog(
    onDismiss: () -> Unit,
    onAdd: (String) -> Unit
) {
    var text by remember { mutableStateOf("") }
    val focusRequester = remember { FocusRequester() }
    val scheme = MaterialTheme.colorScheme

    AlertDialog(
        onDismissRequest = onDismiss,
        containerColor = scheme.surface,
        titleContentColor = scheme.primary,
        textContentColor = scheme.onSurface,
        title = {
            Text(
                "Новый пункт",
                style = MaterialTheme.typography.headlineMedium
            )
        },
        text = {
            TextField(
                value = text,
                onValueChange = { text = it },
                placeholder = {
                    Text("Например: Хлеб", color = scheme.onSurfaceVariant.copy(alpha = 0.5f))
                },
                singleLine = true,
                modifier = Modifier.focusRequester(focusRequester),
                keyboardOptions = KeyboardOptions(imeAction = ImeAction.Done),
                keyboardActions = KeyboardActions(onDone = {
                    if (text.isNotBlank()) onAdd(text)
                }),
                colors = TextFieldDefaults.colors(
                    focusedContainerColor = Color.Transparent,
                    unfocusedContainerColor = Color.Transparent,
                    focusedIndicatorColor = scheme.primary,
                    unfocusedIndicatorColor = scheme.outline,
                    cursorColor = scheme.primary
                )
            )
        },
        confirmButton = {
            TextButton(
                onClick = { if (text.isNotBlank()) onAdd(text) },
                enabled = text.isNotBlank()
            ) {
                Text("Добавить", color = scheme.primary)
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Отмена", color = scheme.onSurfaceVariant)
            }
        }
    )

    LaunchedEffect(Unit) {
        focusRequester.requestFocus()
    }
}

@Composable
private fun Modifier.clickableNoIndication(onClick: () -> Unit): Modifier {
    val interactionSource = remember {
        androidx.compose.foundation.interaction.MutableInteractionSource()
    }
    return this.clickable(
        interactionSource = interactionSource,
        indication = null,
        onClick = onClick
    )
}