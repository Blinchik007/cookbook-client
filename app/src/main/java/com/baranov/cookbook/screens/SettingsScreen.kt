package com.baranov.cookbook.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.selection.selectable
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
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
    val scheme = MaterialTheme.colorScheme

    var autoLogin by remember { mutableStateOf(true) }
    LaunchedEffect(Unit) {
        autoLogin = AppContainer.settingsPreferences.loadAutoLogin()
    }

    val bgBrush = Brush.radialGradient(
        colors = listOf(scheme.surfaceVariant, scheme.background, scheme.background),
        radius = 1400f
    )

    Scaffold(
        containerColor = Color.Transparent,
        modifier = Modifier.background(bgBrush),
        topBar = {
            TopAppBar(
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent,
                    titleContentColor = scheme.primary,
                    navigationIconContentColor = scheme.primary
                ),
                title = {
                    Text(
                        "Настройки",
                        style = MaterialTheme.typography.headlineMedium
                    )
                },
                navigationIcon = {
                    Box(
                        modifier = Modifier
                            .padding(start = 8.dp)
                            .size(40.dp)
                            .clip(CircleShape)
                            .border(1.dp, scheme.primary.copy(alpha = 0.5f), CircleShape)
                    ) {
                        IconButton(onClick = onBack, modifier = Modifier.fillMaxSize()) {
                            Icon(
                                Icons.AutoMirrored.Filled.ArrowBack,
                                contentDescription = "Назад",
                                tint = scheme.primary
                            )
                        }
                    }
                }
            )
        }
    ) { innerPadding ->
        Column(
            modifier = Modifier
                .padding(innerPadding)
                .padding(horizontal = 20.dp, vertical = 16.dp)
        ) {
            // --- Тема ---
            SectionHeader("Тема оформления")
            Spacer(Modifier.height(8.dp))

            val options = listOf(
                ThemeMode.SYSTEM to "Системная",
                ThemeMode.LIGHT to "Светлая",
                ThemeMode.DARK to "Тёмная"
            )
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(scheme.surface)
                    .border(1.dp, scheme.primary.copy(alpha = 0.25f), RoundedCornerShape(16.dp))
                    .padding(vertical = 4.dp)
            ) {
                options.forEachIndexed { index, (mode, label) ->
                    val selected = currentMode == mode
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .selectable(
                                selected = selected,
                                onClick = {
                                    ThemeHolder.mode.value = mode
                                    scope.launch { AppContainer.settingsPreferences.saveThemeMode(mode) }
                                }
                            )
                            .padding(horizontal = 16.dp, vertical = 14.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier
                                .size(22.dp)
                                .clip(CircleShape)
                                .border(1.5.dp, scheme.primary, CircleShape),
                            contentAlignment = Alignment.Center
                        ) {
                            if (selected) {
                                Box(
                                    modifier = Modifier
                                        .size(12.dp)
                                        .clip(CircleShape)
                                        .background(scheme.primary)
                                )
                            }
                        }
                        Spacer(Modifier.width(14.dp))
                        Text(
                            label,
                            style = MaterialTheme.typography.bodyLarge,
                            color = if (selected) scheme.primary else scheme.onSurface
                        )
                    }
                    if (index < options.lastIndex) {
                        HorizontalDivider(
                            color = scheme.outline.copy(alpha = 0.3f),
                            modifier = Modifier.padding(horizontal = 16.dp)
                        )
                    }
                }
            }

            Spacer(Modifier.height(28.dp))

            // --- Авто-логин ---
            SectionHeader("Безопасность")
            Spacer(Modifier.height(8.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(16.dp))
                    .background(scheme.surface)
                    .border(1.dp, scheme.primary.copy(alpha = 0.25f), RoundedCornerShape(16.dp))
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        "Запоминать вход",
                        style = MaterialTheme.typography.bodyLarge,
                        color = scheme.onSurface
                    )
                    Text(
                        "Автоматический вход при следующем запуске",
                        style = MaterialTheme.typography.bodySmall,
                        color = scheme.onSurfaceVariant
                    )
                }
                Switch(
                    checked = autoLogin,
                    onCheckedChange = { enabled ->
                        autoLogin = enabled
                        scope.launch { AppContainer.settingsPreferences.saveAutoLogin(enabled) }
                    },
                    colors = SwitchDefaults.colors(
                        checkedThumbColor      = scheme.onPrimary,
                        checkedTrackColor      = scheme.primary,
                        checkedBorderColor     = scheme.primary,
                        uncheckedThumbColor    = scheme.onSurfaceVariant,
                        uncheckedTrackColor    = scheme.surfaceVariant,
                        uncheckedBorderColor   = scheme.outline
                    )
                )
            }
        }
    }
}

@Composable
private fun SectionHeader(text: String) {
    Text(
        text  = text,
        style = MaterialTheme.typography.headlineMedium,
        color = MaterialTheme.colorScheme.primary,
        modifier = Modifier.padding(start = 4.dp)
    )
}