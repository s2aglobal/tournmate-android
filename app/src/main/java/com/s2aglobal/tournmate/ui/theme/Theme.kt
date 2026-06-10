package com.s2aglobal.tournmate.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color

private val LightColorScheme = lightColorScheme(
    primary = BrandPurple,
    onPrimary = Color.White,
    primaryContainer = BrandPurple.copy(alpha = 0.15f),
    onPrimaryContainer = DarkNavy,
    secondary = BrandPurpleDark,
    onSecondary = Color.White,
    background = SurfaceLight,
    onBackground = OnSurfaceLight,
    surface = Color.White,
    onSurface = OnSurfaceLight,
    surfaceVariant = SurfaceDim,
    onSurfaceVariant = OnSurfaceVariantLight,
    error = ErrorRed,
    onError = Color.White,
)

@Composable
fun TournMateTheme(
    content: @Composable () -> Unit,
) {
    MaterialTheme(
        colorScheme = LightColorScheme,
        typography = TournMateTypography,
        content = content,
    )
}
