package com.baranov.cookbook

import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.annotation.RequiresApi
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
            CurrentUserHolder.currentUser = AppContainer.userPreferences.loadUser()
        }

        setContent {
            CookbookTheme {
                CookbookApp()
            }
        }
    }
}