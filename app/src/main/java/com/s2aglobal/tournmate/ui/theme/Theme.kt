package com.s2aglobal.tournmate.ui.theme

import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ProvideTextStyle
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.TextUnit

private fun lightScheme(theme: SportTheme) = lightColorScheme(
    primary = theme.primary,
    onPrimary = Color.White,
    primaryContainer = theme.primary.copy(alpha = 0.15f),
    onPrimaryContainer = DarkNavy,
    secondary = theme.primaryDeep,
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
    // Material components (switches, fields, progress, ripples) follow the sport accent.
    val theme = CurrentSport.theme
    val colorScheme = remember(theme) { lightScheme(theme) }
    MaterialTheme(
        colorScheme = colorScheme,
        typography = TournMateTypography,
    ) {
        // Material's default LocalTextStyle (bodyLarge) carries a fixed lineHeight that leaks into every
        // `Text(fontSize = …)` call. Use the font's natural line height instead, like SwiftUI.
        ProvideTextStyle(LocalTextStyle.current.copy(lineHeight = TextUnit.Unspecified), content)
    }
}
