package com.willykez.codeorganizer.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Typography
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val PrimaryBlue = Color(0xFF1E3A8A)
val AccentGreen = Color(0xFF10B981)
val ErrorRed = Color(0xFFEF4444)
val LightBg = Color(0xFFF5F5F5)
val DarkBg = Color(0xFF121212)

private val LightColors = lightColorScheme(
    primary = PrimaryBlue,
    secondary = AccentGreen,
    background = LightBg,
    surface = Color.White,
    error = ErrorRed
)

private val DarkColors = darkColorScheme(
    primary = Color(0xFF60A5FA),
    secondary = AccentGreen,
    background = DarkBg,
    surface = Color(0xFF1E1E1E),
    error = ErrorRed
)

@Composable
fun CodeOrganizerTheme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colors = if (darkTheme) DarkColors else LightColors
    MaterialTheme(colorScheme = colors, typography = Typography(), content = content)
}
