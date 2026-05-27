package com.baranov.cookbook.ui.components

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable

/**
 * Универсальный диалог "Требуется вход в аккаунт".
 *
 * Показывается, когда гость пытается воспользоваться функцией,
 * требующей авторизации (скачать рецепт, опубликовать и т.п.).
 *
 * @param actionDescription короткое описание действия в форме инфинитива
 *   с маленькой буквы: "скачать рецепт", "опубликовать рецепт".
 *   Подставляется в шаблон "Чтобы {action}, нужно войти в аккаунт".
 * @param onDismiss закрыть диалог без действия (кнопка "Позже" или клик вне диалога).
 * @param onLogin перейти на экран авторизации.
 */
@Composable
fun LoginRequiredDialog(
    actionDescription: String,
    onDismiss: () -> Unit,
    onLogin: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text("Нужен вход в аккаунт") },
        text = { Text("Чтобы $actionDescription, нужно войти в аккаунт.") },
        confirmButton = {
            TextButton(onClick = onLogin) {
                Text("Войти")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Позже")
            }
        }
    )
}