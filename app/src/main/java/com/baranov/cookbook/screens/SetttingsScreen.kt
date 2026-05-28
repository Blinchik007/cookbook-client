package com.baranov.cookbook.screens

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.selectable
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.baranov.cookbook.AppContainer
import com.baranov.cookbook.ThemeHolder
import com.baranov.cookbook.ThemeMode
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(onBack: () -> Unit) {
    val scope = rememberCoroutineScope()
    val currentMode by ThemeHolder.mode

    // Локальное состояние авто-логина. Загружаем один раз при открытии экрана.
    var autoLogin by remember { mutableStateOf(true) }
    LaunchedEffect(Unit) {
        autoLogin = AppContainer.settingsPreferences.loadAutoLogin()
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Настройки") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Назад")
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(modifier = Modifier.padding(innerPadding).padding(16.dp)) {
            // --- Тема ---
            Text("Тема оформления", style = MaterialTheme.typography.titleMedium)
            Spacer(Modifier.height(8.dp))

            val options = listOf(
                ThemeMode.SYSTEM to "Системная",
                ThemeMode.LIGHT to "Светлая",
                ThemeMode.DARK to "Тёмная"
            )
            options.forEach { (mode, label) ->
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .selectable(
                            selected = currentMode == mode,
                            onClick = {
                                ThemeHolder.mode.value = mode
                                scope.launch { AppContainer.settingsPreferences.saveThemeMode(mode) }
                            }
                        )
                        .padding(vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    RadioButton(selected = currentMode == mode, onClick = null)
                    Spacer(Modifier.width(12.dp))
                    Text(label, style = MaterialTheme.typography.bodyLarge)
                }
            }

            Spacer(Modifier.height(16.dp))
            HorizontalDivider()
            Spacer(Modifier.height(16.dp))

            // --- Авто-логин ---
            Row(
                modifier = Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text("Запоминать вход", style = MaterialTheme.typography.bodyLarge)
                    Text(
                        "Автоматический вход при следующем запуске",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
                Switch(
                    checked = autoLogin,
                    onCheckedChange = { enabled ->
                        autoLogin = enabled
                        scope.launch { AppContainer.settingsPreferences.saveAutoLogin(enabled) }
                    }
                )
            }
        }
    }
}