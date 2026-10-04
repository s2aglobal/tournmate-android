package com.s2aglobal.tournmate.domain.model

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive

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
    /**
     * 0 (missing/null) = tournament created before tie-breakers were applied:
     * standings use the legacy order and ignore [tieBreaker]/[groupTieBreaker].
     * The wizard writes [CURRENT_TIE_BREAK_RULES_VERSION]; Edit keeps whatever is
     * stored. Same key and Int type as iOS.
     */
    val tieBreakRulesVersion: Int = 0,
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

        /**
         * Tie-breaker for a tournament whose stored config has no tie-breaker (or
         * no config at all). Standings ignored the setting until it was wired up,
         * ordering ties by the sport's score difference; POINT_DIFF reproduces
         * exactly that. New tournaments still default to HEAD_TO_HEAD, which every
         * client writes explicitly. Mirrors iOS `FormatConfig.legacyTieBreaker`.
         */
        val LEGACY_TIE_BREAKER = TieBreaker.POINT_DIFF

        /** Version the creation wizard writes ([tieBreakRulesVersion]). */
        const val CURRENT_TIE_BREAK_RULES_VERSION = 1

        fun decodeOrNull(json: String?): FormatConfig? {
            if (json.isNullOrBlank()) return null
            return try {
                val obj = parseConfigObject(json)
                ConfigJson.decodeFromJsonElement(serializer(), obj).copy(
                    tieBreaker = storedTieBreaker(obj, "tieBreaker"),
                    groupTieBreaker = storedTieBreaker(obj, "groupTieBreaker"),
                )
            } catch (_: Exception) {
                null
            }
        }

        /** The stored tie-breaker when present and known, else [LEGACY_TIE_BREAKER] (iOS decodes the same way). */
        private fun storedTieBreaker(obj: JsonObject, key: String): String {
            val raw = (obj[key] as? JsonPrimitive)?.takeIf { it.isString }?.content
            return TieBreaker.entries.firstOrNull { it.rawValue == raw }?.rawValue ?: LEGACY_TIE_BREAKER.rawValue
        }

        /** Config used when a tournament has no readable config. Mirrors iOS `FormatConfig.fallback`. */
        fun fallback(matchFormat: MatchFormat): FormatConfig =
            defaults(matchFormat).copy(tieBreaker = LEGACY_TIE_BREAKER.rawValue, groupTieBreaker = LEGACY_TIE_BREAKER.rawValue)

        /** Decodes leniently, falling back to the match format's defaults. */
        fun decode(json: String?, matchFormat: MatchFormat): FormatConfig =
            decodeOrNull(json) ?: fallback(matchFormat)
    }

    /** Tie-breaker standings actually apply to the round-robin table (iOS `effectiveTieBreaker`). */
    val effectiveTieBreaker: TieBreaker
        get() = if (tieBreakRulesVersion >= 1) TieBreaker.fromRawValue(tieBreaker) else LEGACY_TIE_BREAKER

    /** Same as [effectiveTieBreaker], for Group + Knockout group tables. */
    val effectiveGroupTieBreaker: TieBreaker
        get() = if (tieBreakRulesVersion >= 1) TieBreaker.fromRawValue(groupTieBreaker) else LEGACY_TIE_BREAKER

    fun encode(): String = ConfigJson.encodeToString(serializer(), this)
}
