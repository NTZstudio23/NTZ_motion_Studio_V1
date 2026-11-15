package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.darkColorScheme
import androidx.compose.runtime.Composable

private val NtzDarkColorScheme =
  darkColorScheme(
    primary = NtzNeonViolet,
    onPrimary = NtzTextPrimary,
    secondary = NtzBrightCyan,
    onSecondary = NtzDeepInk,
    tertiary = NtzNeonMagenta,
    onTertiary = NtzTextPrimary,
    background = NtzDeepInk,
    onBackground = NtzTextPrimary,
    surface = NtzSurface,
    onSurface = NtzTextPrimary,
    surfaceVariant = NtzCard,
    onSurfaceVariant = NtzTextSecondary,
    outline = NtzBorderStrong,
  )

@Composable
fun NtzMotionStudioTheme(content: @Composable () -> Unit) {
  MaterialTheme(
    colorScheme = NtzDarkColorScheme,
    typography = Typography,
    content = content,
  )
}
