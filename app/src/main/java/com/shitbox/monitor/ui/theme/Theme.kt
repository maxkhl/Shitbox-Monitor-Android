package com.shitbox.monitor.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

val Bg       = Color(0xFF0b0e13)
val Surface  = Color(0xFF131820)
val Border   = Color(0xFF1e2733)
val Accent   = Color(0xFF00d4aa)
val Accent2  = Color(0xFFf0a500)
val Accent3  = Color(0xFF4ea3ff)
val Warn     = Color(0xFFff4d4d)
val TextMain = Color(0xFFd8e4f0)
val Muted    = Color(0xFF4a5e72)

private val ColorScheme = darkColorScheme(
    primary = Accent,
    onPrimary = Bg,
    secondary = Accent2,
    background = Bg,
    onBackground = TextMain,
    surface = Surface,
    onSurface = TextMain,
    surfaceVariant = Surface,
    onSurfaceVariant = Muted,
    error = Warn,
)

@Composable
fun ShitboxTheme(content: @Composable () -> Unit) {
    @Suppress("UNUSED_EXPRESSION") isSystemInDarkTheme()
    MaterialTheme(colorScheme = ColorScheme, content = content)
}
