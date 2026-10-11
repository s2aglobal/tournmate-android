package com.s2aglobal.tournmate.domain.model

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.longOrNull
import kotlinx.serialization.json.put
import java.util.Locale
import kotlin.math.abs

/*
 * Cricket ("innings" archetype): event types, overs notation, scorecard
 * validation, match outcome and net run rate.
 *
 * Port of the server reference `functions/src/services/cricket.ts` (and iOS
 * `CricketRules.swift`). Messages, margins and NRR must match them exactly; the
 * shared cases live in the server's `scripts/checkCricket.ts` and in
 * `CricketRulesTest`.
 *
 * Stored data
 * - tournaments.eventTypeId / rulesData (JSON string, [CricketRules])
 * - matches.resultType ("win" | "tie" | "noResult"), resultData (JSON string,
 *   [CricketResult]), summary ("Dallas Royals won by 6 wickets")
 */

// ── Rules ───────────────────────────────────────────────

enum class CricketEventType(val rawValue: String) {
    T20("t20"), ODI("odi"), T10("t10"), BOX8("box8"), BOX6("box6"), CUSTOM("custom");

    val displayName: String
        get() = when (this) {
            T20 -> "T20"
            ODI -> "One Day (50 overs)"
            T10 -> "T10"
            BOX8 -> "Box cricket (8-a-side)"
            BOX6 -> "Box cricket (6-a-side)"
            CUSTOM -> "Custom"
        }

    val shortName: String
        get() = when (this) {
            T20 -> "T20"
            ODI -> "ODI"
            T10 -> "T10"
            BOX8 -> "BOX 8"
            BOX6 -> "BOX 6"
            CUSTOM -> "CRICKET"
        }

    val defaultRules: CricketRules
        get() = when (this) {
            T20 -> CricketRules(T20, playersPerSide = 11, overs = 20)
            ODI -> CricketRules(ODI, playersPerSide = 11, overs = 50)
            T10 -> CricketRules(T10, playersPerSide = 11, overs = 10)
            BOX8 -> CricketRules(BOX8, playersPerSide = 8, overs = 8)
            BOX6 -> CricketRules(BOX6, playersPerSide = 6, overs = 6, lastManStands = true)
            CUSTOM -> CricketRules(CUSTOM, playersPerSide = 11, overs = 20)
        }

    companion object {
        /** Event types offered in the wizard (custom is reached by editing values). */
        val presets: List<CricketEventType> = listOf(T20, ODI, T10, BOX8, BOX6)

        fun fromRawValue(raw: String?): CricketEventType? = entries.firstOrNull { it.rawValue == raw }
    }
}

data class CricketRules(
    val eventType: CricketEventType,
    /** Players on the field per side. */
    val playersPerSide: Int,
    /** Overs per innings. */
    val overs: Int,
    val ballsPerOver: Int = 6,
    /** The last batter may bat alone, so all [playersPerSide] wickets can fall. */
    val lastManStands: Boolean = false,
    /** A tie is decided by a super over (the result records who won it). */
    val superOver: Boolean = true,
) {
    /** Wickets that end an innings (all out). */
    val maxWickets: Int get() = if (lastManStands) playersPerSide else playersPerSide - 1

    /** "20 overs · 11-a-side · Super over" */
    val summary: String
        get() {
            val parts = mutableListOf("$overs ${if (overs == 1) "over" else "overs"}", "$playersPerSide-a-side")
            if (ballsPerOver != 6) parts.add("$ballsPerOver-ball overs")
            if (lastManStands) parts.add("Last man stands")
            if (superOver) parts.add("Super over")
            return parts.joinToString(" · ")
        }

    /** `rulesData` JSON (same keys as iOS and the server). */
    fun encoded(): String = buildJsonObject {
        put("eventTypeId", eventType.rawValue)
        put("playersPerSide", playersPerSide)
        put("overs", overs)
        put("ballsPerOver", ballsPerOver)
        put("lastManStands", lastManStands)
        put("superOver", superOver)
    }.toString()

    companion object {
        val DEFAULT: CricketRules get() = CricketEventType.T20.defaultRules

        val playersRange = 2..11
        val oversRange = 1..50
        val ballsPerOverRange = 4..8

        /**
         * Decodes `rulesData`. Lenient like the server: missing keys fall back to the
         * event type's preset (T20 when the type is missing or unknown). `null` for bad
         * JSON, wrongly typed values, or values outside the allowed ranges.
         */
        fun decode(json: String?): CricketRules? {
            val obj = parseObject(json) ?: return null
            val rawType = (obj["eventTypeId"] as? JsonPrimitive)?.takeIf { it.isString }?.content
            val type = CricketEventType.fromRawValue(rawType)
            val preset = if (type == null || type == CricketEventType.CUSTOM) DEFAULT else type.defaultRules
            val eventType = if (type == CricketEventType.CUSTOM) CricketEventType.CUSTOM else preset.eventType

            val players = obj.intOr("playersPerSide", preset.playersPerSide) ?: return null
            val overs = obj.intOr("overs", preset.overs) ?: return null
            val bpo = obj.intOr("ballsPerOver", preset.ballsPerOver) ?: return null
            val lms = obj.boolOr("lastManStands", preset.lastManStands) ?: return null
            val so = obj.boolOr("superOver", preset.superOver) ?: return null
            if (players !in playersRange || overs !in oversRange || bpo !in ballsPerOverRange) return null
            return CricketRules(eventType, players, overs, bpo, lms, so)
        }
    }
}

// ── Overs notation ──────────────────────────────────────

object CricketOvers {
    private val pattern = Regex("""^\s*(\d{1,3})(?:\.(\d))?\s*$""")

    /** Legal balls → overs notation: 110 balls → "18.2". */
    fun format(balls: Int, ballsPerOver: Int = 6): String {
        val rest = balls % ballsPerOver
        return if (rest == 0) "${balls / ballsPerOver}" else "${balls / ballsPerOver}.$rest"
    }

    /**
     * Overs notation → legal balls: "18.2" → 110. `null` for text that isn't overs
     * notation or a ball digit ≥ balls per over ("18.6").
     */
    fun parse(text: String, ballsPerOver: Int = 6): Int? {
        val m = pattern.matchEntire(text) ?: return null
        val full = m.groupValues[1].toInt()
        val rest = m.groupValues[2].takeIf { it.isNotEmpty() }?.toInt() ?: 0
        if (rest >= ballsPerOver) return null
        return full * ballsPerOver + rest
    }

    /** Overs as a decimal for rates: 110 balls → 18.333… */
    fun decimal(balls: Int, ballsPerOver: Int = 6): Double = balls.toDouble() / ballsPerOver
}

// ── Result ──────────────────────────────────────────────

data class CricketInnings(
    val battingRegistrationId: String,
    val runs: Int,
    val wickets: Int,
    /** Legal balls faced. */
    val balls: Int,
)

data class CricketRevisedTarget(
    /** Runs the side batting second needs to win. */
    val runs: Int,
    /** Overs available to the side batting second. */
    val overs: Int,
)

data class CricketResult(
    /** Two innings in batting order. Empty or one for an abandoned match. */
    val innings: List<CricketInnings>,
    /** Rain-affected chase: target and overs announced by the umpires. */
    val revisedTarget: CricketRevisedTarget? = null,
    /** Registration that won the super over, for a tie. */
    val superOverWinnerRegistrationId: String? = null,
    /** Abandoned without a result (weather, light, ground). */
    val noResult: Boolean? = null,
) {
    val isNoResult: Boolean get() = noResult == true

    fun innings(forRegistrationId: String): CricketInnings? =
        innings.firstOrNull { it.battingRegistrationId == forRegistrationId }

    /**
     * Both innings entered and the scores level: the first innings' total, or
     * one short of a revised target. Only then can a super over be played. Mirrors iOS.
     */
    val scoresLevel: Boolean
        get() {
            if (isNoResult || innings.size != 2) return false
            val target = revisedTarget?.runs ?: (innings[0].runs + 1)
            return innings[1].runs == target - 1
        }

    /** `resultData` JSON with sorted keys, like iOS. */
    fun encoded(): String = buildJsonObject {
        put("innings", buildJsonArray {
            innings.forEach { inn ->
                add(buildJsonObject {
                    put("balls", inn.balls)
                    put("battingRegistrationId", inn.battingRegistrationId)
                    put("runs", inn.runs)
                    put("wickets", inn.wickets)
                })
            }
        })
        noResult?.let { put("noResult", it) }
        revisedTarget?.let { rt ->
            put("revisedTarget", buildJsonObject {
                put("overs", rt.overs)
                put("runs", rt.runs)
            })
        }
        superOverWinnerRegistrationId?.let { put("superOverWinnerRegistrationId", it) }
    }.toString()

    companion object {
        /** Decodes `matches.resultData`; `null` when it isn't a cricket result. */
        fun decode(json: String?): CricketResult? {
            val obj = parseObject(json) ?: return null
            val list = obj["innings"] as? JsonArray ?: return null
            val innings = list.map { el ->
                val x = el as? JsonObject ?: return null
                val side = (x["battingRegistrationId"] as? JsonPrimitive)?.takeIf { it.isString }?.content ?: return null
                CricketInnings(
                    battingRegistrationId = side,
                    runs = x.strictInt("runs") ?: return null,
                    wickets = x.strictInt("wickets") ?: return null,
                    balls = x.strictInt("balls") ?: return null,
                )
            }
            val rtEl = obj["revisedTarget"]
            val revised = if (rtEl == null || rtEl is JsonNull) null else {
                val rt = rtEl as? JsonObject ?: return null
                CricketRevisedTarget(rt.strictInt("runs") ?: return null, rt.strictInt("overs") ?: return null)
            }
            val so = (obj["superOverWinnerRegistrationId"] as? JsonPrimitive)?.takeIf { it.isString }?.content
            val nr = (obj["noResult"] as? JsonPrimitive)?.takeIf { !it.isString }?.booleanOrNull
            return CricketResult(innings, revised, so, if (nr == true) true else null)
        }
    }
}

enum class CricketResultType(val rawValue: String) {
    WIN("win"), TIE("tie"), NO_RESULT("noResult");

    companion object {
        fun fromRawValue(raw: String?): CricketResultType? = entries.firstOrNull { it.rawValue == raw }
    }
}

data class CricketOutcome(
    val resultType: CricketResultType,
    val winnerRegistrationId: String?,
    /** "won by 6 wickets (10 balls left)", "won by 23 runs", "Match tied", "No result". */
    val margin: String,
) {
    /** "Dallas Royals won by 6 wickets (10 balls left)" / "Match tied" / "No result". */
    fun summary(teamName: (String) -> String): String {
        val winner = winnerRegistrationId ?: return margin
        return "${teamName(winner)} $margin"
    }
}

data class CricketIssue(val field: String, val message: String)

object CricketScoring {
    private fun plural(n: Int, one: String, many: String): String = "$n ${if (n == 1) one else many}"

    /** Every problem with a scorecard; empty means it can be saved. */
    fun validate(result: CricketResult, rules: CricketRules, teamA: String, teamB: String): List<CricketIssue> {
        val issues = mutableListOf<CricketIssue>()
        fun add(field: String, message: String) { issues.add(CricketIssue(field, message)) }
        val r = rules
        val limit = r.overs * r.ballsPerOver
        val wMax = r.maxWickets

        if (result.isNoResult) {
            if (result.innings.size > 2) add("innings", "A match has at most two innings.")
        } else if (result.innings.size != 2) {
            add("innings", "Enter both innings, or mark the match as no result.")
            return issues
        }

        val sides = result.innings.map { it.battingRegistrationId }
        if (sides.any { it != teamA && it != teamB }) {
            add("innings", "Each innings must be batted by one of the two teams.")
        }
        if (sides.size == 2 && sides[0] == sides[1]) {
            add("innings", "Each team bats once.")
        }

        result.innings.forEachIndexed { idx, inn ->
            val f = "innings[$idx]"
            if (inn.runs < 0) add("$f.runs", "Runs must be 0 or more.")
            if (inn.wickets < 0 || inn.wickets > wMax) add("$f.wickets", "Wickets must be between 0 and $wMax.")
            val rt = result.revisedTarget
            val ballLimit = if (idx == 1 && rt != null) rt.overs * r.ballsPerOver else limit
            if (inn.balls < 0) {
                add("$f.balls", "Overs must be 0 or more.")
            } else if (inn.balls > ballLimit) {
                add("$f.balls", "An innings is at most ${plural(ballLimit / r.ballsPerOver, "over", "overs")}.")
            }
        }
        if (issues.isNotEmpty() || result.isNoResult) return issues

        val first = result.innings[0]
        val second = result.innings[1]

        result.revisedTarget?.let { rt ->
            if (rt.runs < 1) add("revisedTarget.runs", "Revised target must be at least 1 run.")
            if (rt.overs < 1 || rt.overs > r.overs) add("revisedTarget.overs", "Revised overs must be between 1 and ${r.overs}.")
            if (issues.isNotEmpty()) return issues
        }
        val target = result.revisedTarget?.runs ?: (first.runs + 1)
        val secondLimit = result.revisedTarget?.let { it.overs * r.ballsPerOver } ?: limit

        val firstDone = first.wickets == wMax || first.balls == limit || result.revisedTarget != null
        if (!firstDone) {
            add("innings[0]", "The first innings isn't finished. It ends when $wMax wickets fall or ${r.overs} overs are bowled.")
        }

        val chased = second.runs >= target
        val secondDone = chased || second.wickets == wMax || second.balls == secondLimit
        if (!secondDone) {
            add("innings[1]", "The chase isn't finished. It ends when the target of $target is reached, $wMax wickets fall, or the overs run out.")
        }
        if (chased && second.wickets == wMax) {
            add("innings[1].wickets", "The chasing team reached the target, so it can't also be all out.")
        }
        // The match ends on the ball the target is reached; one ball adds at
        // most a six plus a few extras or overthrows.
        if (chased && second.runs > target + 10) {
            add("innings[1].runs", "The chase passed the target of $target by ${second.runs - target}. The match ends as soon as the target is reached.")
        }

        result.superOverWinnerRegistrationId?.let { so ->
            // Level scores: the first innings' total, or one short of a revised target.
            val tied = second.runs == target - 1
            when {
                !tied -> add("superOver", "A super over is only played when the scores are level.")
                !r.superOver -> add("superOver", "This tournament doesn't use super overs.")
                so != teamA && so != teamB -> add("superOver", "The super over winner must be one of the two teams.")
            }
        }
        return issues
    }

    /** Outcome of a valid result (call [validate] first). */
    fun outcome(result: CricketResult, rules: CricketRules): CricketOutcome {
        if (result.isNoResult || result.innings.size < 2) {
            return CricketOutcome(CricketResultType.NO_RESULT, null, "No result")
        }
        val first = result.innings[0]
        val second = result.innings[1]
        val rt = result.revisedTarget
        val target = rt?.runs ?: (first.runs + 1)
        val secondLimit = (rt?.overs ?: rules.overs) * rules.ballsPerOver
        val dls = if (rt != null) " (revised target)" else ""

        if (second.runs >= target) {
            val wicketsLeft = rules.maxWickets - second.wickets
            val ballsLeft = secondLimit - second.balls
            val balls = if (ballsLeft > 0) " (${plural(ballsLeft, "ball", "balls")} left)" else ""
            return CricketOutcome(
                CricketResultType.WIN, second.battingRegistrationId,
                "won by ${plural(wicketsLeft, "wicket", "wickets")}$balls$dls",
            )
        }
        val tied = if (rt != null) second.runs == target - 1 else second.runs == first.runs
        if (tied) {
            result.superOverWinnerRegistrationId?.let {
                return CricketOutcome(CricketResultType.WIN, it, "won the super over")
            }
            return CricketOutcome(CricketResultType.TIE, null, "Match tied")
        }
        val runs = (if (rt != null) target - 1 else first.runs) - second.runs
        return CricketOutcome(CricketResultType.WIN, first.battingRegistrationId, "won by ${plural(runs, "run", "runs")}$dls")
    }
}

// ── Standings ───────────────────────────────────────────

object CricketPoints {
    const val WIN = 2
    const val TIE = 1
    const val NO_RESULT = 1
    const val LOSS = 0
}

data class CricketRunRate(
    var runsFor: Int = 0,
    var ballsFaced: Int = 0,
    var runsAgainst: Int = 0,
    var ballsBowled: Int = 0,
) {
    /**
     * Adds one match. A side bowled out counts its full quota of overs.
     * No-result matches don't count toward NRR.
     */
    fun add(result: CricketResult, registrationId: String, rules: CricketRules) {
        if (result.isNoResult || result.innings.size != 2) return
        result.innings.forEachIndexed { idx, inn ->
            val rt = result.revisedTarget
            val limit = if (idx == 1 && rt != null) rt.overs * rules.ballsPerOver else rules.overs * rules.ballsPerOver
            val balls = if (inn.wickets == rules.maxWickets) limit else inn.balls
            if (inn.battingRegistrationId == registrationId) {
                runsFor += inn.runs
                ballsFaced += balls
            } else {
                runsAgainst += inn.runs
                ballsBowled += balls
            }
        }
    }

    /** 0 until the team has batted and bowled. */
    fun netRunRate(ballsPerOver: Int = 6): Double {
        if (ballsFaced <= 0 || ballsBowled <= 0) return 0.0
        return runsFor / CricketOvers.decimal(ballsFaced, ballsPerOver) -
            runsAgainst / CricketOvers.decimal(ballsBowled, ballsPerOver)
    }

    companion object {
        /** "+0.820" / "−1.125" (U+2212 minus, like iOS and the server). */
        fun format(nrr: Double): String {
            val v = Math.round(nrr * 1000) / 1000.0
            return "${if (v >= 0) "+" else "−"}${String.format(Locale.US, "%.3f", abs(v))}"
        }
    }
}

// ── JSON helpers ────────────────────────────────────────

private val CricketJson = Json { ignoreUnknownKeys = true }

internal fun parseObject(json: String?): JsonObject? =
    runCatching { CricketJson.parseToJsonElement(json ?: return null) as? JsonObject }.getOrNull()

/** Integer value (JSON number without a fraction), or null for anything else. */
internal fun JsonElement.asStrictInt(): Int? {
    val p = this as? JsonPrimitive ?: return null
    if (p.isString) return null
    val content = p.content
    if (content.contains('.') || content.contains('e') || content.contains('E')) {
        val d = content.toDoubleOrNull() ?: return null
        return if (d % 1.0 == 0.0 && abs(d) < Int.MAX_VALUE) d.toInt() else null
    }
    return p.longOrNull?.takeIf { it in Int.MIN_VALUE..Int.MAX_VALUE }?.toInt()
}

private fun JsonObject.strictInt(key: String): Int? = this[key]?.asStrictInt()

/** Missing or null → [fallback]; present but not an integer → null (invalid). */
private fun JsonObject.intOr(key: String, fallback: Int): Int? {
    val v = this[key]
    if (v == null || v is JsonNull) return fallback
    return v.asStrictInt()
}

/** Missing or null → [fallback]; present but not a boolean → null (invalid). */
private fun JsonObject.boolOr(key: String, fallback: Boolean): Boolean? {
    val v = this[key]
    if (v == null || v is JsonNull) return fallback
    val p = v as? JsonPrimitive ?: return null
    if (p.isString) return null
    return p.booleanOrNull
}
