package com.s2aglobal.tournmate.ui.theme

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import com.s2aglobal.tournmate.domain.model.SportType

// Per-sport palette — matches iOS SportTheme.swift exactly.

// Pickleball — blue hard court, green surround, optic-yellow ball.
val PickleCourtBlue = Color(0.09f, 0.36f, 0.70f)
val PickleCourtDeep = Color(0.05f, 0.20f, 0.45f)
val PickleSurround = Color(0.16f, 0.52f, 0.36f)
val PickleBall = Color(0.85f, 0.95f, 0.26f)
val PickleBallShade = Color(0.62f, 0.75f, 0.10f)

// Tennis — grass green with a yellow ball.
val TennisGrass = Color(0.13f, 0.47f, 0.27f)
val TennisGrassDeep = Color(0.06f, 0.30f, 0.16f)
val TennisBall = Color(0.86f, 0.93f, 0.20f)

// Table tennis — blue table, red paddle.
val TableTennisBlue = Color(0.10f, 0.25f, 0.55f)
val TableTennisRed = Color(0.86f, 0.18f, 0.20f)

data class SportTheme(
    /** Main surface colour (header gradient start, selected card fill). */
    val primary: Color,
    /** Darker end of the gradient. */
    val primaryDeep: Color,
    /** Pop colour for the ball, highlights and the selected checkmark. */
    val accent: Color,
    /** Text/icon colour drawn on top of [accent]. */
    val onAccent: Color,
    /** Very light tint for chips and badges on white backgrounds. */
    val tint: Color,
    /** Three-beat slogan shown in the Play header. */
    val tagline: String,
    /** Call to action about gear for empty states ("get the paddles popping"). */
    val gearPhrase: String,
    /** What players bring ("paddles and balls"). */
    val gearNoun: String,
) {
    /** Top-leading → bottom-trailing gradient. */
    val gradient: Brush get() = Brush.linearGradient(listOf(primary, primaryDeep))
}

private val PickleballTheme = SportTheme(
    primary = PickleCourtBlue, primaryDeep = PickleCourtDeep,
    accent = PickleBall, onAccent = PickleCourtDeep,
    tint = PickleCourtBlue.copy(alpha = 0.10f),
    tagline = "Dink. Drive. Dominate.",
    gearPhrase = "get the paddles popping",
    gearNoun = "paddles and balls",
)

private val BadmintonTheme = SportTheme(
    primary = TournmatePurple, primaryDeep = TournmatePurpleDark,
    accent = LimeAccent, onAccent = TournmatePurpleDark,
    tint = TournmatePurple.copy(alpha = 0.10f),
    tagline = "Smash. Rally. Win.",
    gearPhrase = "get the shuttlecocks flying",
    gearNoun = "shuttlecocks",
)

private val TennisTheme = SportTheme(
    primary = TennisGrass, primaryDeep = TennisGrassDeep,
    accent = TennisBall, onAccent = TennisGrassDeep,
    tint = TennisGrass.copy(alpha = 0.10f),
    tagline = "Serve. Volley. Ace.",
    gearPhrase = "get the rackets swinging",
    gearNoun = "racket and balls",
)

private val TableTennisTheme = SportTheme(
    primary = TableTennisBlue, primaryDeep = DarkNavy,
    accent = TableTennisRed, onAccent = Color.White,
    tint = TableTennisBlue.copy(alpha = 0.10f),
    tagline = "Spin. Loop. Win.",
    gearPhrase = "get the paddles spinning",
    gearNoun = "paddle and balls",
)

private val DefaultTheme = SportTheme(
    primary = TournmatePurple, primaryDeep = DarkNavy,
    accent = LimeAccent, onAccent = DarkNavy,
    tint = TournmatePurple.copy(alpha = 0.10f),
    tagline = "Organize. Play. Compete.",
    gearPhrase = "get the game started",
    gearNoun = "gear",
)

val SportType.theme: SportTheme
    get() = when (this) {
        SportType.PICKLEBALL -> PickleballTheme
        SportType.BADMINTON -> BadmintonTheme
        SportType.TENNIS -> TennisTheme
        SportType.TABLE_TENNIS -> TableTennisTheme
        else -> DefaultTheme
    }

/** What players bring, for placeholder copy. */
val SportType.gearNoun: String get() = theme.gearNoun

/**
 * The user's current sport as Compose snapshot state, mirrored from
 * `CurrentUserStore.preferredSportFlow` in MainActivity. Accent tokens read it,
 * so anything drawn with them re-themes when the sport changes.
 */
object CurrentSport {
    var sport: SportType by mutableStateOf(SportType.BADMINTON)
    val theme: SportTheme get() = sport.theme
}
