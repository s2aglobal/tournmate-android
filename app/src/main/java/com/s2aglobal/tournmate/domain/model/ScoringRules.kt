package com.s2aglobal.tournmate.domain.model

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable

@Serializable
enum class ScoringSystem(val rawValue: String) {
    @SerialName("rally") RALLY("rally"),
    @SerialName("sideOut") SIDE_OUT("sideOut");

    val displayName: String
        get() = when (this) {
            RALLY -> "Rally Scoring"
            SIDE_OUT -> "Side-Out Scoring"
        }

    val tagline: String
        get() = when (this) {
            RALLY -> "Every rally wins a point"
            SIDE_OUT -> "Only the serving team scores"
        }
}

/**
 * Scoring settings for a tournament's matches, stored as JSON in `scoringConfigData`.
 * Missing keys fall back to best-of-3, to 21, win by 2, rally (matches iOS).
 */
@Serializable
data class ScoringConfig(
    val gamesPerMatch: Int = 3,
    val pointsToWin: Int = 21,
    val winBy: Int = 2,
    val pointCap: Int? = null,
    val scoringSystem: ScoringSystem = ScoringSystem.RALLY,
) {
    val gamesToWin: Int
        get() = gamesPerMatch / 2 + 1

    val summary: String
        get() {
            val games = if (gamesPerMatch == 1) "1 game" else "Best of $gamesPerMatch"
            if (pointsToWin <= 0) return games
            val margin = if (winBy > 1) ", win by $winBy" else ""
            return "$games · to $pointsToWin$margin"
        }

    fun encode(): String = ConfigJson.encodeToString(serializer(), this)

    companion object {
        fun decodeOrNull(json: String?): ScoringConfig? {
            if (json.isNullOrBlank()) return null
            return try {
                ConfigJson.decodeFromString(serializer(), json)
            } catch (_: Exception) {
                null
            }
        }
    }
}

class SportScoringRules(
    val defaultConfig: ScoringConfig,
    val gamesPerMatchOptions: List<Int>,
    val pointsToWinOptions: List<Int>,
    val scoringSystemOptions: List<ScoringSystem>,
    val capForTarget: (Int) -> Int?,
) {
    fun config(gamesPerMatch: Int, pointsToWin: Int, scoringSystem: ScoringSystem): ScoringConfig =
        ScoringConfig(
            gamesPerMatch = gamesPerMatch,
            pointsToWin = pointsToWin,
            winBy = defaultConfig.winBy,
            pointCap = capForTarget(pointsToWin),
            scoringSystem = scoringSystem,
        )
}

val SportType.scoringRules: SportScoringRules
    get() = when (this) {
        SportType.BADMINTON -> SportScoringRules(
            defaultConfig = ScoringConfig(3, 21, 2, 30, ScoringSystem.RALLY),
            gamesPerMatchOptions = listOf(1, 3),
            pointsToWinOptions = listOf(11, 15, 21),
            scoringSystemOptions = listOf(ScoringSystem.RALLY),
            capForTarget = { target ->
                when (target) {
                    21 -> 30
                    15 -> 21
                    11 -> 15
                    else -> null
                }
            },
        )
        SportType.PICKLEBALL -> SportScoringRules(
            defaultConfig = ScoringConfig(3, 11, 2, null, ScoringSystem.SIDE_OUT),
            gamesPerMatchOptions = listOf(1, 3),
            pointsToWinOptions = listOf(11, 15, 21),
            scoringSystemOptions = listOf(ScoringSystem.SIDE_OUT, ScoringSystem.RALLY),
            capForTarget = { null },
        )
        SportType.TABLE_TENNIS -> SportScoringRules(
            defaultConfig = ScoringConfig(5, 11, 2, null, ScoringSystem.RALLY),
            gamesPerMatchOptions = listOf(3, 5, 7),
            pointsToWinOptions = listOf(11, 21),
            scoringSystemOptions = listOf(ScoringSystem.RALLY),
            capForTarget = { null },
        )
        SportType.TENNIS -> SportScoringRules(
            defaultConfig = ScoringConfig(3, 6, 2, 7, ScoringSystem.RALLY),
            gamesPerMatchOptions = listOf(1, 3, 5),
            pointsToWinOptions = listOf(6),
            scoringSystemOptions = listOf(ScoringSystem.RALLY),
            capForTarget = { target -> target + 1 },
        )
        SportType.VOLLEYBALL -> SportScoringRules(
            defaultConfig = ScoringConfig(5, 25, 2, null, ScoringSystem.RALLY),
            gamesPerMatchOptions = listOf(1, 3, 5),
            pointsToWinOptions = listOf(15, 21, 25),
            scoringSystemOptions = listOf(ScoringSystem.RALLY),
            capForTarget = { null },
        )
        SportType.PADEL -> SportScoringRules(
            defaultConfig = ScoringConfig(3, 6, 2, 7, ScoringSystem.RALLY),
            gamesPerMatchOptions = listOf(1, 3),
            pointsToWinOptions = listOf(6),
            scoringSystemOptions = listOf(ScoringSystem.RALLY),
            capForTarget = { target -> target + 1 },
        )
        SportType.SQUASH -> SportScoringRules(
            defaultConfig = ScoringConfig(5, 11, 2, null, ScoringSystem.RALLY),
            gamesPerMatchOptions = listOf(3, 5),
            pointsToWinOptions = listOf(11, 15),
            scoringSystemOptions = listOf(ScoringSystem.RALLY),
            capForTarget = { null },
        )
        SportType.BEACH_VOLLEYBALL -> SportScoringRules(
            defaultConfig = ScoringConfig(3, 21, 2, null, ScoringSystem.RALLY),
            gamesPerMatchOptions = listOf(1, 3),
            pointsToWinOptions = listOf(15, 21),
            scoringSystemOptions = listOf(ScoringSystem.RALLY),
            capForTarget = { null },
        )
        SportType.ROUNDNET -> SportScoringRules(
            defaultConfig = ScoringConfig(3, 21, 2, null, ScoringSystem.RALLY),
            gamesPerMatchOptions = listOf(1, 3),
            pointsToWinOptions = listOf(11, 15, 21),
            scoringSystemOptions = listOf(ScoringSystem.RALLY),
            capForTarget = { null },
        )
        // Generic fallback: no set scoring until these sports get real rules.
        SportType.BASKETBALL, SportType.FOOTBALL, SportType.SOCCER, SportType.CRICKET,
        SportType.GOLF, SportType.DISC_GOLF, SportType.BOWLING, SportType.DARTS, SportType.GENERIC ->
            SportScoringRules(
                defaultConfig = ScoringConfig(1, 0, 1, null, ScoringSystem.RALLY),
                gamesPerMatchOptions = listOf(1),
                pointsToWinOptions = emptyList(),
                scoringSystemOptions = listOf(ScoringSystem.RALLY),
                capForTarget = { null },
            )
    }

sealed class ScoreValidationError {
    data object Negative : ScoreValidationError()
    data object Tied : ScoreValidationError()
    data class BelowTarget(val target: Int) : ScoreValidationError()
    data class MarginTooSmall(val winBy: Int) : ScoreValidationError()
    data class GameShouldHaveEnded(val target: Int, val winBy: Int) : ScoreValidationError()
    data class ExceedsCap(val cap: Int) : ScoreValidationError()
    data class TooManyGames(val max: Int) : ScoreValidationError()
    data class MatchNotDecided(val gamesToWin: Int) : ScoreValidationError()
    data object GamesAfterMatchDecided : ScoreValidationError()

    val message: String get() = message(SportType.BADMINTON)

    /** User-facing text. Tennis sets are scored in games, so it says "games" / "Set". */
    fun message(sport: SportType): String {
        val scoreWord = if (sport == SportType.TENNIS) "games" else "points"
        val scoreWordSingular = if (sport == SportType.TENNIS) "game" else "point"
        val unit = sport.setNameSingular.replaceFirstChar { it.uppercase() }
        return when (this) {
            Negative -> "Scores can't be negative"
            Tied -> "A ${sport.setNameSingular} can't end in a tie"
            is BelowTarget -> "Winner needs at least $target $scoreWord"
            is MarginTooSmall -> "Must win by $winBy"
            is GameShouldHaveEnded -> "$unit ends at $target with a $winBy-$scoreWordSingular lead"
            is ExceedsCap -> "Max score is $cap"
            is TooManyGames -> "A match has at most $max games"
            is MatchNotDecided -> "One side must win $gamesToWin ${sport.setNameSingular}${if (gamesToWin == 1) "" else "s"}"
            GamesAfterMatchDecided -> "Match was already won before the last ${sport.setNameSingular}"
        }
    }
}

data class MatchValidationResult(
    val gameErrors: Map<Int, ScoreValidationError>,
    val matchError: ScoreValidationError?,
)

object ScoreValidator {

    /** Validates one finished game. Returns `null` when the score is legal. */
    fun validateGame(score: SetScore, config: ScoringConfig): ScoreValidationError? {
        val a = score.teamAPoints
        val b = score.teamBPoints
        if (a < 0 || b < 0) return ScoreValidationError.Negative
        if (a == b) return ScoreValidationError.Tied

        val target = config.pointsToWin
        if (target <= 0) return null

        val winner = maxOf(a, b)
        val loser = minOf(a, b)
        val winBy = maxOf(config.winBy, 1)
        val cap = config.pointCap

        if (cap != null && winner > cap) return ScoreValidationError.ExceedsCap(cap)
        if (winner < target) return ScoreValidationError.BelowTarget(target)

        if (winner == target) {
            return if (winner - loser >= winBy) null else ScoreValidationError.MarginTooSmall(winBy)
        }

        if (cap != null && winner == cap) {
            return if (loser >= cap - winBy) null else ScoreValidationError.GameShouldHaveEnded(target, winBy)
        }
        if (winBy == 1) return ScoreValidationError.GameShouldHaveEnded(target, winBy)
        if (winner - loser < winBy) return ScoreValidationError.MarginTooSmall(winBy)
        if (winner - loser > winBy) return ScoreValidationError.GameShouldHaveEnded(target, winBy)
        return null
    }

    /** Validates a full match: per-game errors keyed by game index, plus at most one match-level error. */
    fun validateMatch(scores: List<SetScore>, config: ScoringConfig): MatchValidationResult {
        val gameErrors = mutableMapOf<Int, ScoreValidationError>()
        scores.forEachIndexed { index, score ->
            validateGame(score, config)?.let { gameErrors[index] = it }
        }

        if (scores.size > config.gamesPerMatch) {
            return MatchValidationResult(gameErrors, ScoreValidationError.TooManyGames(config.gamesPerMatch))
        }

        var winsA = 0
        var winsB = 0
        for (score in scores) {
            if (winsA == config.gamesToWin || winsB == config.gamesToWin) {
                return MatchValidationResult(gameErrors, ScoreValidationError.GamesAfterMatchDecided)
            }
            if (score.teamAWon) winsA++ else if (score.teamBWon) winsB++
        }

        if (winsA != config.gamesToWin && winsB != config.gamesToWin) {
            return MatchValidationResult(gameErrors, ScoreValidationError.MatchNotDecided(config.gamesToWin))
        }
        return MatchValidationResult(gameErrors, null)
    }

    fun isValidMatch(scores: List<SetScore>, config: ScoringConfig): Boolean {
        val result = validateMatch(scores, config)
        return result.gameErrors.isEmpty() && result.matchError == null
    }
}
