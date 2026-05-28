package com.baranov.cookbook

import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.annotation.RequiresApi
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.getValue
import androidx.lifecycle.lifecycleScope
import com.baranov.cookbook.ui.theme.CookbookTheme
import kotlinx.coroutines.launch

class MainActivity : ComponentActivity() {
    @RequiresApi(Build.VERSION_CODES.O)
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        AppContainer.init(this)

        lifecycleScope.launch {
            val autoLogin = AppContainer.settingsPreferences.loadAutoLogin()
            CurrentUserHolder.currentUser =
                if (autoLogin) AppContainer.userPreferences.loadUser() else null
            ThemeHolder.mode.value = AppContainer.settingsPreferences.loadThemeMode()
            AppContainer.repository.syncProductsFromServer()
        }

        setContent {
            val mode by ThemeHolder.mode
            val darkTheme = when (mode) {
                ThemeMode.SYSTEM -> isSystemInDarkTheme()
                ThemeMode.LIGHT -> false
                ThemeMode.DARK -> true
            }
            CookbookTheme(darkTheme = darkTheme) {
                CookbookApp()
            }
        }
    }
}