package com.baranov.cookbook.data

import android.content.Context
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import com.baranov.cookbook.CurrentUser
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

private val Context.dataStore by preferencesDataStore(name = "user_prefs")

class UserPreferences(private val context: Context) {

    private object Keys {
        val USER_ID = intPreferencesKey("user_id")
        val USER_NAME = stringPreferencesKey("user_name")
        val USER_EMAIL = stringPreferencesKey("user_email")
        val USER_PHOTO = stringPreferencesKey("user_photo")
    }

    suspend fun saveUser(user: CurrentUser) {
        context.dataStore.edit { prefs ->
            prefs[Keys.USER_ID] = user.id
            prefs[Keys.USER_NAME] = user.name
            prefs[Keys.USER_EMAIL] = user.email
            if (user.photo != null) {
                prefs[Keys.USER_PHOTO] = user.photo
            } else {
                prefs.remove(Keys.USER_PHOTO)
            }
        }
    }

    suspend fun loadUser(): CurrentUser? {
        val prefs = context.dataStore.data.first()
        val id = prefs[Keys.USER_ID] ?: return null
        return CurrentUser(
            id = id,
            name = prefs[Keys.USER_NAME] ?: "",
            email = prefs[Keys.USER_EMAIL] ?: "",
            photo = prefs[Keys.USER_PHOTO]
        )
    }

    suspend fun clearUser() {
        context.dataStore.edit { it.clear() }
    }
}