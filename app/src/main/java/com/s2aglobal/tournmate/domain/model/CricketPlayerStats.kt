package com.s2aglobal.tournmate.domain.model

import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put

/*
 * Optional batting and bowling figures per player for a cricket match (stored
 * on the match as `playerStatsData`), the tournament leaderboards and a
 * player's career totals.
 *
 * Port of the server reference `functions/src/services/cricketPlayerStats.ts`
 * (and iOS `CricketPlayerStats.swift`): same checks, messages and integer
 * rounding. Shared cases: the server's `scripts/checkCricket.ts` ("Player
 * stats") and `CricketPlayerStatsTest`.
 *
 * Players are keyed like team roles ([TeamRoles.key]); `teamRegistrationId` is
 * the team the player plays for, so a bowler's figures belong to the team that
 * bowled.
 */

data class BattingFigures(
    val teamRegistrationId: String,
    val key: String,
    val name: String,
    val runs: Int,
    /** Balls faced. */
    val balls: Int,
    /** false = not out. */
    val out: Boolean,
)

data class BowlingFigures(
    val teamRegistrationId: String,
    val key: String,
    val name: String,
    /** Legal balls bowled. */
    val balls: Int,
    /** Runs conceded. */
    val runs: Int,
    val wickets: Int,
)

data class CricketPlayerStats(
    val batting: List<BattingFigures> = emptyList(),
    val bowling: List<BowlingFigures> = emptyList(),
) {
    val isEmpty: Boolean get() = batting.isEmpty() && bowling.isEmpty()

    data class Issue(val field: String, val message: String)

    // ── Stored JSON ─────────────────────────────────────────

    /** `playerStatsData` JSON with sorted keys, like iOS and the server seed. */
    fun encoded(): String = buildJsonObject {
        put("batting", buildJsonArray {
            batting.forEach { f ->
                add(buildJsonObject {
                    put("balls", f.balls)
                    put("key", f.key)
                    put("name", f.name)
                    put("out", f.out)
                    put("runs", f.runs)
                    put("teamRegistrationId", f.teamRegistrationId)
                })
            }
        })
        put("bowling", buildJsonArray {
            bowling.forEach { f ->
                add(buildJsonObject {
                    put("balls", f.balls)
                    put("key", f.key)
                    put("name", f.name)
                    put("runs", f.runs)
                    put("teamRegistrationId", f.teamRegistrationId)
                    put("wickets", f.wickets)
                })
            }
        })
    }.toString()

    // ── Validation ──────────────────────────────────────────

    /** Checks the figures against the match scorecard. Empty = can be saved. */
    fun validate(result: CricketResult, teamA: String, teamB: String, names: (String) -> String): List<Issue> {
        val issues = mutableListOf<Issue>()
        fun add(field: String, message: String) { issues.add(Issue(field, message)) }
        val teams = listOf(teamA, teamB)
        fun other(t: String) = if (t == teamA) teamB else teamA
        fun innings(t: String) = result.innings.firstOrNull { it.battingRegistrationId == t }

        data class Entry(val team: String, val key: String, val name: String, val numbers: List<Int>)
        fun checkEntries(kind: String, entries: List<Entry>) {
            val seen = mutableSetOf<String>()
            for (e in entries) {
                if (e.team !in teams) {
                    add(kind, "${e.name} isn't in either team.")
                    continue
                }
                val id = "${e.team}|${TeamRoles.key(e.key)}"
                if (id in seen) add(kind, "${e.name} is listed twice.")
                seen.add(id)
                if (!e.numbers.all { it >= 0 }) add(kind, "${e.name}'s figures must be whole numbers, 0 or more.")
            }
        }
        checkEntries("batting", batting.map { Entry(it.teamRegistrationId, it.key, it.name, listOf(it.runs, it.balls)) })
        checkEntries("bowling", bowling.map { Entry(it.teamRegistrationId, it.key, it.name, listOf(it.balls, it.runs, it.wickets)) })
        if (issues.isNotEmpty()) return issues

        for (team in teams) {
            val bat = batting.filter { it.teamRegistrationId == team }
            if (bat.isNotEmpty()) {
                val own = innings(team)
                if (own != null) {
                    val runs = bat.sumOf { it.runs }
                    val balls = bat.sumOf { it.balls }
                    val outs = bat.count { it.out }
                    if (runs > own.runs) add("batting", "${names(team)}'s batters scored $runs runs, more than the team's ${own.runs}.")
                    if (balls > own.balls) add("batting", "${names(team)}'s batters faced $balls balls, more than the innings' ${own.balls}.")
                    if (outs > own.wickets) add("batting", "$outs ${names(team)} batters are out, but the team lost ${own.wickets} wickets.")
                } else {
                    add("batting", "${names(team)} didn't bat.")
                }
            }
            val bowl = bowling.filter { it.teamRegistrationId == team }
            if (bowl.isNotEmpty()) {
                val faced = innings(other(team))
                if (faced != null) {
                    val balls = bowl.sumOf { it.balls }
                    val runs = bowl.sumOf { it.runs }
                    val wickets = bowl.sumOf { it.wickets }
                    if (balls > faced.balls) add("bowling", "${names(team)}'s bowlers bowled $balls balls, more than the innings' ${faced.balls}.")
                    if (runs > faced.runs) add("bowling", "${names(team)}'s bowlers conceded $runs runs, more than the ${faced.runs} scored.")
                    if (wickets > faced.wickets) add("bowling", "${names(team)}'s bowlers took $wickets wickets, more than the ${faced.wickets} that fell.")
                } else {
                    add("bowling", "${names(team)} didn't bowl.")
                }
            }
        }
        return issues
    }

    companion object {
        /** Decodes `playerStatsData`; null when missing or malformed (like the server's `decodePlayerStats`). */
        fun decode(json: String?): CricketPlayerStats? {
            val obj = parseObject(json) ?: return null
            val batting = (obj.listOrNull("batting") ?: return null).map { el ->
                val x = el as? JsonObject ?: return null
                BattingFigures(
                    teamRegistrationId = x.text("teamRegistrationId") ?: return null,
                    key = x.text("key") ?: return null,
                    name = x.text("name") ?: return null,
                    runs = x.count("runs") ?: return null,
                    balls = x.count("balls") ?: return null,
                    out = (x["out"] as? JsonPrimitive)?.takeIf { !it.isString }?.booleanOrNull ?: return null,
                )
            }
            val bowling = (obj.listOrNull("bowling") ?: return null).map { el ->
                val x = el as? JsonObject ?: return null
                BowlingFigures(
                    teamRegistrationId = x.text("teamRegistrationId") ?: return null,
                    key = x.text("key") ?: return null,
                    name = x.text("name") ?: return null,
                    balls = x.count("balls") ?: return null,
                    runs = x.count("runs") ?: return null,
                    wickets = x.count("wickets") ?: return null,
                )
            }
            return CricketPlayerStats(batting, bowling)
        }

        /** Missing or null → empty; an array → its elements; anything else → null (malformed, as on iOS). */
        private fun JsonObject.listOrNull(key: String): List<JsonElement>? = when (val v = this[key]) {
            null, is JsonNull -> emptyList()
            is JsonArray -> v
            else -> null
        }

        private fun JsonObject.text(key: String): String? =
            (this[key] as? JsonPrimitive)?.takeIf { it.isString }?.content?.takeIf { it.isNotEmpty() }

        private fun JsonObject.count(key: String): Int? = this[key]?.asStrictInt()?.takeIf { it >= 0 }
    }
}

// ── Rates (integer rounding, half up, same as the server) ──

object CricketRates {
    /** Strike rate in tenths (runs × 100 / balls). null without balls. */
    fun strikeRateTenths(runs: Int, balls: Int): Int? =
        if (balls > 0) ((2L * runs * 1000 + balls) / (2L * balls)).toInt() else null

    /** Economy in hundredths (runs per 6 balls). null without balls. */
    fun economyHundredths(runs: Int, balls: Int): Int? =
        if (balls > 0) ((2L * runs * 600 + balls) / (2L * balls)).toInt() else null

    /** Batting average in hundredths (runs / times out). null if never out. */
    fun averageHundredths(runs: Int, outs: Int): Int? =
        if (outs > 0) ((2L * runs * 100 + outs) / (2L * outs)).toInt() else null

    /** 1425 → "142.5"; null → "–". */
    fun formatTenths(v: Int?): String = if (v == null) "–" else "${v / 10}.${v % 10}"

    /** 625 → "6.25"; null → "–". */
    fun formatHundredths(v: Int?): String = if (v == null) "–" else "${v / 100}.${(v % 100).toString().padStart(2, '0')}"
}

// ── Leaderboards ────────────────────────────────────────

data class BattingLeader(
    val teamRegistrationId: String,
    val key: String,
    val name: String,
    val innings: Int = 0,
    val runs: Int = 0,
    val balls: Int = 0,
    val notOuts: Int = 0,
    val highest: Int = 0,
    val highestNotOut: Boolean = false,
) {
    val id: String get() = "$teamRegistrationId|$key"
    val outs: Int get() = innings - notOuts
    /** "54*" / "54". */
    val highestText: String get() = "$highest${if (highestNotOut) "*" else ""}"
    val strikeRate: String get() = CricketRates.formatTenths(CricketRates.strikeRateTenths(runs, balls))
    val average: String get() = CricketRates.formatHundredths(CricketRates.averageHundredths(runs, outs))
}

data class BowlingLeader(
    val teamRegistrationId: String,
    val key: String,
    val name: String,
    val innings: Int = 0,
    val balls: Int = 0,
    val runs: Int = 0,
    val wickets: Int = 0,
    val bestWickets: Int = 0,
    val bestRuns: Int = 0,
) {
    val id: String get() = "$teamRegistrationId|$key"
    /** "3/21". */
    val bestText: String get() = "$bestWickets/$bestRuns"
    val economy: String get() = CricketRates.formatHundredths(CricketRates.economyHundredths(runs, balls))
    fun oversText(ballsPerOver: Int): String = CricketOvers.format(balls, ballsPerOver)
}

/** A player's career totals (null when they have no entries of that kind). */
data class CricketCareer(val batting: BattingLeader?, val bowling: BowlingLeader?)

object CricketLeaderboards {
    /**
     * Batting totals per player and team: most runs, then strike rate, then
     * name, team and key (UTF-16 code-unit order, as on the server).
     */
    fun batting(matches: List<CricketPlayerStats>): List<BattingLeader> {
        val map = LinkedHashMap<String, BattingLeader>()
        for (m in matches) {
            for (f in m.batting) {
                val k = TeamRoles.key(f.key)
                val id = "${f.teamRegistrationId}|$k"
                var e = map[id] ?: BattingLeader(f.teamRegistrationId, k, f.name)
                e = e.copy(
                    innings = e.innings + 1,
                    runs = e.runs + f.runs,
                    balls = e.balls + f.balls,
                    notOuts = e.notOuts + if (!f.out) 1 else 0,
                )
                if (f.runs > e.highest || (f.runs == e.highest && !f.out && !e.highestNotOut)) {
                    e = e.copy(highest = f.runs, highestNotOut = !f.out)
                }
                map[id] = e
            }
        }
        return map.values.sortedWith { a, b ->
            if (a.runs != b.runs) return@sortedWith b.runs.compareTo(a.runs)
            val sa = CricketRates.strikeRateTenths(a.runs, a.balls) ?: -1
            val sb = CricketRates.strikeRateTenths(b.runs, b.balls) ?: -1
            if (sa != sb) return@sortedWith sb.compareTo(sa)
            compareText(a.name, b.name, a.teamRegistrationId, b.teamRegistrationId, a.key, b.key)
        }
    }

    /** Bowling totals per player and team: most wickets, then economy, then name, team and key. */
    fun bowling(matches: List<CricketPlayerStats>): List<BowlingLeader> {
        val map = LinkedHashMap<String, BowlingLeader>()
        for (m in matches) {
            for (f in m.bowling) {
                val k = TeamRoles.key(f.key)
                val id = "${f.teamRegistrationId}|$k"
                var e = map[id] ?: BowlingLeader(f.teamRegistrationId, k, f.name)
                e = e.copy(
                    innings = e.innings + 1,
                    balls = e.balls + f.balls,
                    runs = e.runs + f.runs,
                    wickets = e.wickets + f.wickets,
                )
                if (e.innings == 1 || f.wickets > e.bestWickets || (f.wickets == e.bestWickets && f.runs < e.bestRuns)) {
                    e = e.copy(bestWickets = f.wickets, bestRuns = f.runs)
                }
                map[id] = e
            }
        }
        fun econ(e: BowlingLeader): Int = CricketRates.economyHundredths(e.runs, e.balls) ?: Int.MAX_VALUE
        return map.values.sortedWith { a, b ->
            if (a.wickets != b.wickets) return@sortedWith b.wickets.compareTo(a.wickets)
            if (econ(a) != econ(b)) return@sortedWith econ(a).compareTo(econ(b))
            compareText(a.name, b.name, a.teamRegistrationId, b.teamRegistrationId, a.key, b.key)
        }
    }

    /** Figures from finished matches only. */
    fun statsOf(matches: List<Match>): List<CricketPlayerStats> =
        matches.filter { it.status == MatchStatus.FINISHED }.mapNotNull { it.playerStats }

    /** One player's career across teams: their batting and bowling entries merged under one key. */
    fun career(key: String, stats: List<CricketPlayerStats>): CricketCareer {
        val k = TeamRoles.key(key)
        val mine = stats.map { m ->
            CricketPlayerStats(
                batting = m.batting.filter { TeamRoles.key(it.key) == k }.map { it.copy(teamRegistrationId = "career") },
                bowling = m.bowling.filter { TeamRoles.key(it.key) == k }.map { it.copy(teamRegistrationId = "career") },
            )
        }
        return CricketCareer(batting(mine).firstOrNull(), bowling(mine).firstOrNull())
    }

    /** Name, then team, then key; Kotlin's String.compareTo is UTF-16 code-unit order like JavaScript's `<`. */
    private fun compareText(an: String, bn: String, at: String, bt: String, ak: String, bk: String): Int {
        for ((x, y) in listOf(an to bn, at to bt, ak to bk)) {
            if (x != y) return x.compareTo(y)
        }
        return 0
    }
}
