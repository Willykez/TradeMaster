package com.trademaster.pro.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

// Dark-only by design -- this is a trading desk, not a theme-switcher.
private val TradeMasterColorScheme = darkColorScheme(
    primary = Gold,
    onPrimary = Color.Black,
    secondary = Blue,
    tertiary = Purple,
    background = Bg,
    surface = CardBg,
    onBackground = TextPrimary,
    onSurface = TextPrimary,
    error = Red,
    outline = Border,
)

@Composable
fun TradeMasterProTheme(content: @Composable () -> Unit) {
    val view = LocalView.current
    if (!view.isInEditMode) {
        val window = (view.context as? android.app.Activity)?.window
        window?.let {
            WindowCompat.getInsetsController(it, view).isAppearanceLightStatusBars = false
            it.statusBarColor = Bg2.toArgb()
            it.navigationBarColor = Bg2.toArgb()
        }
    }
    MaterialTheme(
        colorScheme = TradeMasterColorScheme,
        typography = TradeMasterTypography,
        content = content
    )
}
