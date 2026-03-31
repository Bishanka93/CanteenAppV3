package com.example.canteenappv2.ui.theme

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val DarkColorScheme = darkColorScheme(
    primary = DarkNeonGreen,
    onPrimary = Black,
    primaryContainer = DarkNeonGreenContainer,
    onPrimaryContainer = DarkNeonGreenOnContainer,
    secondary = DarkNeonGreen,
    onSecondary = Black,
    secondaryContainer = DarkNeonGreenContainer,
    onSecondaryContainer = DarkNeonGreenOnContainer,
    background = DarkBackground,
    surface = DarkSurface,
    onBackground = White,
    onSurface = White,
    surfaceVariant = DarkSurfaceVariant,
    onSurfaceVariant = White
)

private val LightColorScheme = lightColorScheme(
    primary = GreenPrimary,
    onPrimary = White,
    primaryContainer = GreenContainer,
    onPrimaryContainer = GreenOnContainer,
    secondary = GreenSecondary,
    onSecondary = White,
    secondaryContainer = GreenContainer,
    onSecondaryContainer = GreenOnContainer,
    background = White,
    surface = White,
    onBackground = Black,
    onSurface = Black,
    surfaceVariant = Color(0xFFF1F8E9),
    onSurfaceVariant = Black
)

@Composable
fun CanteenAppV2Theme(
    darkTheme: Boolean = isSystemInDarkTheme(),
    content: @Composable () -> Unit
) {
    val colorScheme = if (darkTheme) DarkColorScheme else LightColorScheme

    MaterialTheme(
        colorScheme = colorScheme,
        typography = Typography,
        content = content
    )
}
