package com.s2aglobal.tournmate.ui.theme

import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color

// Matches iOS AppTheme.swift exactly

// Fixed brand identity (logo, sign-in, update screen). Never sport-dependent.
val TournmatePurple = Color(0xFF630893)
val TournmatePurpleDark = Color(0xFF53057D)

// Accent tokens — follow the user's sport (see CurrentSport). Use these for every UI accent.
val AppAccent: Color get() = CurrentSport.theme.primary
val AppAccentDeep: Color get() = CurrentSport.theme.primaryDeep
val AppAccentTint: Color get() = CurrentSport.theme.tint

/** Accent gradient (leading → trailing), iOS `LinearGradient.brand`. */
val AccentGradient: Brush get() = Brush.horizontalGradient(listOf(AppAccent, AppAccentDeep))

val DarkNavy = Color(0xFF0F172A)
val DarkNavyLight = Color(0xFF1E293B)

val LimeAccent = Color(0xFFBA8CFF)

val CardSand = Color(0xFFF5EDAD)
val CardMint = Color(0xFFE0F2EE)
val CardLavender = Color(0xFFEBE6F8)
val CardPeach = Color(0xFFFAE8E0)
val CardSky = Color(0xFFE0EEFA)

val PrizeGold = Color(0xFF8C5A0D)
val PrizeGoldLight = Color(0xFFFFEDB8)
val SuccessGreen = Color(0xFF4CAF50)
val ErrorRed = Color(0xFFE53935)
val WarningOrange = Color(0xFFFF9800)

val SurfaceLight = Color(0xFFFAFAFA)
val SurfaceDim = Color(0xFFF0F0F0)
val OnSurfaceLight = Color(0xFF1C1B1F)
val OnSurfaceVariantLight = Color(0xFF49454F)
