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

// Tennis — grass green with a yellow ball.
val TennisGrass = Color(0.13f, 0.47f, 0.27f)
val TennisGrassDeep = Color(0.06f, 0.30f, 0.16f)
val TennisBall = Color(0.86f, 0.93f, 0.20f)

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

private val DefaultTheme = SportTheme(
    primary = TournmatePurple, primaryDeep = DarkNavy,
    accent = LimeAccent, onAccent = DarkNavy,
    tint = TournmatePurple.copy(alpha = 0.10f),
    tagline = "Organize. Play. Compete.",
    gearPhrase = "get the game started",
    gearNoun = "gear",
)

/** Theme for catalog sports without a bespoke palette: one brand colour, darkened for the gradient. */
private fun catalogTheme(hex: Long, tagline: String, gearPhrase: String, gearNoun: String): SportTheme {
    val primary = Color(hex)
    // Gradient end: the spec colour at 0.68× brightness (iOS parity).
    val deep = Color(primary.red * 0.68f, primary.green * 0.68f, primary.blue * 0.68f)
    return SportTheme(
        primary = primary, primaryDeep = deep,
        accent = Color.White, onAccent = deep,
        tint = primary.copy(alpha = 0.10f),
        tagline = tagline, gearPhrase = gearPhrase, gearNoun = gearNoun,
    )
}

// Catalog sports (spec colours). Only used if one goes live and is selected.
private val TableTennisTheme = catalogTheme(0xFFC62828, "Spin. Loop. Win.", "get the paddles spinning", "paddle and balls")
private val PadelTheme = catalogTheme(0xFF0E7C7B, "Lob. Bandeja. Win.", "get the padel rackets out", "racket and balls")
private val SquashTheme = catalogTheme(0xFF37474F, "Boast. Drop. Win.", "get the ball warmed up", "racket and balls")
private val VolleyballTheme = catalogTheme(0xFFF2A900, "Bump. Set. Spike.", "get the ball in the air", "a ball")
private val BeachVolleyballTheme = catalogTheme(0xFFE07A1F, "Dig. Set. Spike.", "get on the sand", "a ball")
private val BasketballTheme = catalogTheme(0xFFE65100, "Dribble. Pass. Score.", "get the hoops going", "a ball")
private val SoccerTheme = catalogTheme(0xFF2E7D32, "Pass. Shoot. Score.", "get the ball rolling", "boots and a ball")
private val CricketTheme = catalogTheme(0xFFB71C1C, "Bowl. Bat. Win.", "get the stumps up", "bat and ball")
private val GolfTheme = catalogTheme(0xFF1B5E20, "Drive. Chip. Putt.", "get out on the course", "clubs and balls")
private val BowlingTheme = catalogTheme(0xFF3949AB, "Roll. Strike. Repeat.", "get the pins falling", "bowling shoes")
private val DartsTheme = catalogTheme(0xFF263238, "Aim. Throw. Checkout.", "get the darts flying", "darts")
private val DiscGolfTheme = catalogTheme(0xFFE8890C, "Drive. Upshot. Chains.", "get the discs flying", "discs")
private val RoundnetTheme = catalogTheme(0xFFC99A06, "Serve. Set. Spike.", "get the net set up", "a net and balls")

val SportType.theme: SportTheme
    get() = when (this) {
        SportType.PICKLEBALL -> PickleballTheme
        SportType.BADMINTON -> BadmintonTheme
        SportType.TENNIS -> TennisTheme
        SportType.TABLE_TENNIS -> TableTennisTheme
        SportType.PADEL -> PadelTheme
        SportType.SQUASH -> SquashTheme
        SportType.VOLLEYBALL -> VolleyballTheme
        SportType.BEACH_VOLLEYBALL -> BeachVolleyballTheme
        SportType.BASKETBALL -> BasketballTheme
        SportType.SOCCER -> SoccerTheme
        SportType.CRICKET -> CricketTheme
        SportType.GOLF -> GolfTheme
        SportType.BOWLING -> BowlingTheme
        SportType.DARTS -> DartsTheme
        SportType.DISC_GOLF -> DiscGolfTheme
        SportType.ROUNDNET -> RoundnetTheme
        SportType.FOOTBALL, SportType.GENERIC -> DefaultTheme
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
