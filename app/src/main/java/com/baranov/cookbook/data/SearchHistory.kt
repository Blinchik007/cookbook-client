package com.baranov.cookbook.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.builtins.serializer
import kotlinx.serialization.json.Json

private val Context.searchHistoryDataStore by preferencesDataStore(name = "search_history_prefs")

class SearchHistory(private val context: Context) {

    companion object {
        const val MAX_SIZE = 10
        private val HISTORY_KEY = stringPreferencesKey("history_json")
        private val json = Json { ignoreUnknownKeys = true }
        private val listSerializer = ListSerializer(String.serializer())
    }

    val historyFlow: Flow<List<String>> = context.searchHistoryDataStore.data.map { prefs ->
        val raw = prefs[HISTORY_KEY] ?: return@map emptyList()
        runCatching { json.decodeFromString(listSerializer, raw) }.getOrDefault(emptyList())
    }

    suspend fun getHistory(): List<String> = historyFlow.first()

    suspend fun addQuery(query: String) {
        val trimmed = query.trim()
        if (trimmed.isEmpty()) return
        val current = getHistory()
        // Убираем дубль (если был) и кладём новый наверх.
        val updated = (listOf(trimmed) + current.filter { !it.equals(trimmed, ignoreCase = true) })
            .take(MAX_SIZE)
        save(updated)
    }

    suspend fun clear() {
        context.searchHistoryDataStore.edit { it.clear() }
    }

    private suspend fun save(list: List<String>) {
        context.searchHistoryDataStore.edit { prefs ->
            prefs[HISTORY_KEY] = json.encodeToString(listSerializer, list)
        }
    }
}