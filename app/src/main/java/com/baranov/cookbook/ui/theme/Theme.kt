package com.baranov.cookbook.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColors = darkColorScheme(
    primary           = DarkAccent,
    onPrimary         = DarkBg,
    secondary         = DarkAccentSoft,
    onSecondary       = DarkBg,
    background        = DarkBg,
    onBackground      = DarkOnBg,
    surface           = DarkSurface,
    onSurface         = DarkOnBg,
    surfaceVariant    = DarkSurfaceHi,
    onSurfaceVariant  = DarkOnBgDim,
    outline           = DarkLine,
    error             = ErrorRed
)

private val LightColors = lightColorScheme(
    primary           = LightAccent,
    onPrimary         = Color.White,
    secondary         = LightAccentSoft,
    onSecondary       = LightOnBg,
    background        = LightBg,
    onBackground      = LightOnBg,
    surface           = LightSurface,
    onSurface         = LightOnBg,
    surfaceVariant    = LightSurfaceHi,
    onSurfaceVariant  = LightOnBgDim,
    outline           = LightLine,
    error             = ErrorRed
)

@Composable
fun CookbookTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = if (darkTheme) DarkColors else LightColors,
        typography  = Typography,
        content     = content
    )
}