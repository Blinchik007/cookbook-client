package com.baranov.cookbook

import androidx.compose.runtime.mutableStateOf

/**
 * Режим темы приложения.
 * SYSTEM — следовать системной настройке телефона.
 * LIGHT — всегда светлая.
 * DARK — всегда тёмная.
 */
enum class ThemeMode {
    SYSTEM, LIGHT, DARK
}

/**
 * Глобальный держатель текущего режима темы.
 * Аналогичен CurrentUserHolder — состояние читается корневым setContent,
 * пишется из SettingsScreen. mutableStateOf обеспечивает реактивную перерисовку.
 */
object ThemeHolder {
    var mode = mutableStateOf(ThemeMode.SYSTEM)
}