package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val VeniceDarkColorScheme = darkColorScheme(
    primary = VeniceAmber,
    onPrimary = Color(0xFF1E1300),
    primaryContainer = Color(0xFF3B2500),
    onPrimaryContainer = VeniceAmberLight,
    secondary = VeniceCyan,
    onSecondary = Color(0xFF001F26),
    secondaryContainer = Color(0xFF003640),
    onSecondaryContainer = VeniceCyanLight,
    tertiary = VeniceViolet,
    onTertiary = Color.White,
    background = VeniceBackground,
    onBackground = VeniceTextPrimary,
    surface = VeniceSurface,
    onSurface = VeniceTextPrimary,
    surfaceVariant = VeniceSurfaceElevated,
    onSurfaceVariant = VeniceTextSecondary,
    outline = VeniceBorder,
    outlineVariant = VeniceBorderHighlight,
    error = VeniceRose,
    onError = Color.White
)

@Composable
fun VeniceTheme(
    content: @Composable () -> Unit
) {
    MaterialTheme(
        colorScheme = VeniceDarkColorScheme,
        typography = Typography,
        content = content
    )
}

@Composable
fun MyApplicationTheme(
    content: @Composable () -> Unit
) {
    VeniceTheme(content = content)
}
