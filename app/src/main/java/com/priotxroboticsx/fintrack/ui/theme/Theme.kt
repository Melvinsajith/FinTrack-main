package com.priotxroboticsx.fintrack.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val DarkColorScheme = darkColorScheme(
    primary = AccentBlue,
    background = DarkBlue,
    surface = LightBlue,
    onPrimary = DarkBlue,
    onBackground = TextPrimary,
    onSurface = TextPrimary,
    error = Red,
    outline = TextSecondary,
    inversePrimary = Green
)

@Composable
fun FinTrackTheme(content: @Composable () -> Unit) {
    MaterialTheme(
        colorScheme = DarkColorScheme,
        typography = Typography,
        content = content
    )
}
