package com.baranov.cookbook.data

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.baranov.cookbook.ThemeMode
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

/**
 * Общее хранилище настроек приложения (не зависят от пользователя):
 *  - режим темы,
 *  - флаг авто-логина ("запоминать вход").
 *
 * Отдельный DataStore от профиля пользователя.
 */
private val Context.settingsDataStore by preferencesDataStore(name = "settings_prefs")

class SettingsPreferences(private val context: Context) {

    companion object {
        private val THEME_KEY = stringPreferencesKey("theme_mode")
        private val AUTO_LOGIN_KEY = booleanPreferencesKey("auto_login")
    }

    suspend fun loadThemeMode(): ThemeMode {
        val raw = context.settingsDataStore.data.map { it[THEME_KEY] }.first()
        return runCatching { ThemeMode.valueOf(raw ?: "") }.getOrDefault(ThemeMode.SYSTEM)
    }

    suspend fun saveThemeMode(mode: ThemeMode) {
        context.settingsDataStore.edit { it[THEME_KEY] = mode.name }
    }

    /**
     * Авто-логин. По умолчанию true — приложение запоминает вход (как было раньше).
     */
    suspend fun loadAutoLogin(): Boolean {
        return context.settingsDataStore.data.map { it[AUTO_LOGIN_KEY] }.first() ?: true
    }

    suspend fun saveAutoLogin(enabled: Boolean) {
        context.settingsDataStore.edit { it[AUTO_LOGIN_KEY] = enabled }
    }
}