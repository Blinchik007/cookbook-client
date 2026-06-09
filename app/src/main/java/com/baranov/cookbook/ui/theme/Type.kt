package com.baranov.cookbook.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import com.baranov.cookbook.R

// Шрифт "мелком" с поддержкой кириллицы (только Regular).
// Файл: app/src/main/res/font/chalk_cyrillic_freehand.ttf
val ChalkFontFamily = FontFamily(
    Font(R.font.chalk_cyrillic_freehand, FontWeight.Normal),  // кириллица + цифры
    Font(R.font.cabin_sketch_regular, FontWeight.Normal)      // латиница
)

val Typography = Typography(
    // Крупные заголовки экранов ("Вход", "Регистрация") — мелком
    displayLarge = TextStyle(
        fontFamily = ChalkFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize   = 44.sp
    ),
    displayMedium = TextStyle(
        fontFamily = ChalkFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize   = 36.sp
    ),
    // Подзаголовки и текст на кнопках — мелком, помельче
    headlineMedium = TextStyle(
        fontFamily = ChalkFontFamily,
        fontWeight = FontWeight.Normal,
        fontSize   = 22.sp
    ),
    // Основной текст и подписи — системный (читаемый)
    bodyLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize   = 16.sp
    ),
    bodyMedium = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Normal,
        fontSize   = 14.sp
    ),
    labelLarge = TextStyle(
        fontFamily = FontFamily.Default,
        fontWeight = FontWeight.Medium,
        fontSize   = 16.sp
    )
)