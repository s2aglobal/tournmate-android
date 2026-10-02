package com.s2aglobal.tournmate.domain.model

import kotlinx.serialization.Serializable

enum class SeedingMode(val rawValue: String) {
    ELO_RANKED("eloRanked"),
    RANDOM("random"),
    MANUAL("manual");

    companion object {
        fun fromRawValue(raw: String?): SeedingMode =
            entries.firstOrNull { it.rawValue == raw } ?: ELO_RANKED
    }
}

enum class TieBreaker(val rawValue: String) {
    HEAD_TO_HEAD("headToHead"),
    POINT_DIFF("pointDiff"),
    GAMES_WON("gamesWon");

    companion object {
        fun fromRawValue(raw: String?): TieBreaker =
            entries.firstOrNull { it.rawValue == raw } ?: HEAD_TO_HEAD
    }
}

@Serializable
data class FormatConfig(
    val seedingMode: String = SeedingMode.ELO_RANKED.rawValue,
    val allowByes: Boolean = true,
    val bronzeMatch: Boolean = false,
    val consolationBracket: Boolean = false,
    val pointsPerWin: Int = 2,
    val pointsPerDraw: Int = 1,
    val pointsPerLoss: Int = 0,
    val tieBreaker: String = TieBreaker.HEAD_TO_HEAD.rawValue,
    val doubleRoundRobin: Boolean = false,
    val groupCount: Int = 2,
    val teamsPerGroup: Int = 4,
    val advancingPerGroup: Int = 2,
    val groupTieBreaker: String = TieBreaker.HEAD_TO_HEAD.rawValue,
    val swissRounds: Int = 5,
    val maxParticipants: Int? = null,
) {
    companion object {
        fun defaults(matchFormat: MatchFormat): FormatConfig = when (matchFormat) {
            MatchFormat.SINGLE_ELIMINATION -> FormatConfig(
                seedingMode = SeedingMode.ELO_RANKED.rawValue,
                allowByes = true,
                bronzeMatch = false,
            )
            MatchFormat.DOUBLE_ELIMINATION -> FormatConfig(
                seedingMode = SeedingMode.ELO_RANKED.rawValue,
                allowByes = true,
            )
            MatchFormat.ROUND_ROBIN -> FormatConfig(
                seedingMode = SeedingMode.RANDOM.rawValue,
                pointsPerWin = 2,
                pointsPerDraw = 1,
                pointsPerLoss = 0,
                tieBreaker = TieBreaker.HEAD_TO_HEAD.rawValue,
                doubleRoundRobin = false,
            )
            MatchFormat.GROUP_KNOCKOUT -> FormatConfig(
                groupCount = 2,
                teamsPerGroup = 4,
                advancingPerGroup = 2,
                groupTieBreaker = TieBreaker.HEAD_TO_HEAD.rawValue,
            )
            MatchFormat.SWISS -> FormatConfig(
                swissRounds = 5,
            )
            MatchFormat.MANUAL_DRAW -> FormatConfig(seedingMode = SeedingMode.MANUAL.rawValue)
        }

        fun decodeOrNull(json: String?): FormatConfig? {
            if (json.isNullOrBlank()) return null
            return try {
                ConfigJson.decodeFromString(serializer(), json)
            } catch (_: Exception) {
                null
            }
        }

        /** Decodes leniently, falling back to the match format's defaults. */
        fun decode(json: String?, matchFormat: MatchFormat): FormatConfig =
            decodeOrNull(json) ?: defaults(matchFormat)
    }

    fun encode(): String = ConfigJson.encodeToString(serializer(), this)
}
