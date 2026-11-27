package com.inter.calc.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.staticCompositionLocalOf

// 色值唯一真源：suite/design/tokens.json -> 生成的 DsTokens.kt。
// 这里只做 Compose 主题映射，不再手写任何色值。

val LightPalette = DsLightPalette
val DarkPalette = DsDarkPalette

val LocalInterPalette = staticCompositionLocalOf { LightPalette }

@Composable
fun InterTheme(dark: Boolean, content: @Composable () -> Unit) {
    val palette = if (dark) DarkPalette else LightPalette
    val scheme = if (dark) {
        darkColorScheme(
            primary = palette.accent,
            onPrimary = palette.onAccent,
            secondary = palette.accent,
            secondaryContainer = palette.accentSoft,
            onSecondaryContainer = palette.accent,
            background = palette.background,
            onBackground = palette.textPrimary,
            surface = palette.card,
            onSurface = palette.textPrimary,
            surfaceVariant = palette.card,
            onSurfaceVariant = palette.textSecondary,
            outline = palette.border,
            error = palette.danger,
        )
    } else {
        lightColorScheme(
            primary = palette.accent,
            onPrimary = palette.onAccent,
            secondary = palette.accent,
            secondaryContainer = palette.accentSoft,
            onSecondaryContainer = palette.accent,
            background = palette.background,
            onBackground = palette.textPrimary,
            surface = palette.card,
            onSurface = palette.textPrimary,
            surfaceVariant = palette.card,
            onSurfaceVariant = palette.textSecondary,
            outline = palette.border,
            error = palette.danger,
        )
    }
    CompositionLocalProvider(LocalInterPalette provides palette) {
        MaterialTheme(colorScheme = scheme, content = content)
    }
}
