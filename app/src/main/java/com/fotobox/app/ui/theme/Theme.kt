package com.fotobox.app.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val FotoboxColorScheme = darkColorScheme(
    primary = Color(0xFFE91E8C),
    onPrimary = Color.White,
    primaryContainer = Color(0xFF7B0045),
    onPrimaryContainer = Color(0xFFFFD8E9),
    secondary = Color(0xFFFF6B35),
    onSecondary = Color.White,
    secondaryContainer = Color(0xFF8C3A00),
    onSecondaryContainer = Color(0xFFFFDBCC),
    tertiary = Color(0xFFFFD700),
    onTertiary = Color.Black,
    background = Color(0xFF0D0D0D),
    onBackground = Color(0xFFEFEFEF),
    surface = Color(0xFF1A1A1A),
    onSurface = Color(0xFFEFEFEF),
    surfaceVariant = Color(0xFF2C2C2C),
    onSurfaceVariant = Color(0xFFCFCFCF),
    outline = Color(0xFF444444),
    error = Color(0xFFFF4444),
    onError = Color.White
)

@Composable
fun FotoboxTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = FotoboxColorScheme,
        typography = FotoboxTypography,
        content = content
    )
}
