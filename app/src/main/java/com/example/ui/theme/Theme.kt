package com.example.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = SportyPrimary,
    onPrimary = Color.Black,
    primaryContainer = SportySurfaceVariant,
    onPrimaryContainer = SportyTextPrimary,
    secondary = SportySecondary,
    onSecondary = Color.Black,
    tertiary = SportyAccent,
    background = SportyDarkBackground,
    onBackground = SportyTextPrimary,
    surface = SportySurface,
    onSurface = SportyTextPrimary,
    surfaceVariant = SportySurfaceVariant,
    onSurfaceVariant = SportyTextSecondary,
    error = SportyError,
    onError = Color.White,
    outline = SportyBorder
)

@Composable
fun SportyFlyTheme(
    darkTheme: Boolean = true, // Force dark theme for TV / sports streaming feel
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = DarkColorScheme,
        typography = Typography,
        content = content
    )
}
