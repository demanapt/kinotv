package com.personal.kinotv.ui.theme

import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.graphics.Color
import androidx.tv.material3.LocalContentColor
import androidx.tv.material3.MaterialTheme
import androidx.tv.material3.darkColorScheme

/** Цвета в духе Netflix: почти чёрный фон, красный акцент, белый текст. */
object KinoColors {
    val Background = Color(0xFF141414)
    val Surface = Color(0xFF1F1F1F)
    val SurfaceVariant = Color(0xFF2A2A2A)
    val Accent = Color(0xFFE53935)
    val TextPrimary = Color(0xFFFFFFFF)
    val TextSecondary = Color(0xFFB3B3B3)
    val Rating = Color(0xFFFFC107)
    val FocusBorder = Color(0xFFFFFFFF)
}

@Composable
fun KinoTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = darkColorScheme(
            primary = KinoColors.Accent,
            onPrimary = Color.White,
            background = KinoColors.Background,
            onBackground = KinoColors.TextPrimary,
            surface = KinoColors.Surface,
            onSurface = KinoColors.TextPrimary,
            surfaceVariant = KinoColors.SurfaceVariant,
            onSurfaceVariant = KinoColors.TextSecondary,
            border = KinoColors.FocusBorder,
        ),
    ) {
        // Текст по умолчанию белый: экраны рисуются прямо на тёмном фоне, без Surface
        CompositionLocalProvider(LocalContentColor provides KinoColors.TextPrimary, content = content)
    }
}
