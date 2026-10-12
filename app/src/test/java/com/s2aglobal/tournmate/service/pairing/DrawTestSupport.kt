package com.s2aglobal.tournmate.service.pairing

import com.s2aglobal.tournmate.domain.model.CricketInnings
import com.s2aglobal.tournmate.domain.model.CricketResult
import com.s2aglobal.tournmate.domain.model.CricketRevisedTarget
import com.s2aglobal.tournmate.domain.model.CricketRules
import com.s2aglobal.tournmate.domain.model.CricketScoring
import com.s2aglobal.tournmate.domain.model.CricketResultType
import com.s2aglobal.tournmate.domain.model.MatchFormat
import com.s2aglobal.tournmate.domain.model.Player
import com.s2aglobal.tournmate.domain.model.Registration
import com.s2aglobal.tournmate.domain.model.SportType
import com.s2aglobal.tournmate.domain.model.TieBreaker
import com.s2aglobal.tournmate.domain.model.Tournament
import com.s2aglobal.tournmate.domain.model.TournamentFormat
import com.s2aglobal.tournmate.ui.screen.tournament.StandingsEntry
import com.s2aglobal.tournmate.ui.screen.tournament.StandingsPoints
import com.s2aglobal.tournmate.ui.screen.tournament.StandingsResult
import java.util.UUID

/**
 * Shared fixtures for the draw tests. Mirrors iOS `Tools/DrawTests/main.swift`
 * exactly (same ids, ratings, RNG and scorecard generator), so the parity dump
 * is byte-for-byte comparable.
 */
internal class Lcg(seed: Int) {
    private var s: Long = seed.toLong()
    fun next(): Long {
        s = s * 6364136223846793005L + 1442695040888963407L
        return s ushr 33
    }
    fun int(bound: Int): Int = (next() % bound).toInt()
}

internal fun uuid(k: Int): UUID = UUID.fromString(String.format("00000000-0000-0000-0000-%012d", k))

internal fun idOf(r: Registration): String = r.id.toString().uppercase()

internal fun makeTournament(format: MatchFormat, rules: CricketRules = CricketRules.DEFAULT): Tournament =
    Tournament(sportType = SportType.CRICKET, formatRaw = TournamentFormat.TEAM.rawValue, matchFormatRaw = format.rawValue)
        .withCricketRules(rules)

internal fun rating(i: Int, tied: Boolean): Double = if (tied) 1000.0 else 1000.0 + ((i * 37) % 400)

internal fun makeTeams(n: Int, t: Tournament, tied: Boolean = false, rosterSize: Int = 2): List<Registration> =
    (0 until n).map { i ->
        val roster = (0 until rosterSize).map { j ->
            Player(id = uuid(1000 + i * 20 + j), name = "P$i-$j", eloRatings = mapOf("cricket" to rating(i, tied)))
        }
        Registration(
            id = uuid(i + 1), tournament = t, player = roster[0], playerId = roster[0].id.toString().uppercase(),
            teamId = "team$i", teamName = "Team $i",
            rosterIds = roster.map { it.id.toString().uppercase() }, roster = roster,
        )
    }

internal fun label(r: Registration): String = "T${r.id.toString().takeLast(12).toInt() - 1}"
internal fun labels(rs: List<Registration>): String = "[" + rs.joinToString(" ") { label(it) } + "]"

/** A cricket scorecard, valid or not, drawn from [rng]. Mirrored exactly on iOS. */
internal fun randomScorecard(rng: Lcg, r: CricketRules, a: String, b: String): CricketResult {
    if (rng.int(25) == 0) return CricketResult(innings = emptyList(), noResult = true)
    val limit = r.overs * r.ballsPerOver
    val wMax = r.maxWickets
    val firstBat = if (rng.int(2) == 0) a else b
    val secondBat = if (firstBat == a) b else a
    val w1 = rng.int(wMax + 2)
    val r1 = rng.int(250)
    val b1 = if (w1 >= wMax) rng.int(limit + 1) else (if (rng.int(4) == 0) rng.int(limit + 1) else limit)
    var revised: CricketRevisedTarget? = null
    if (rng.int(8) == 0) {
        val overs = 1 + rng.int(r.overs + 1)
        revised = CricketRevisedTarget(runs = rng.int(r1 + 2), overs = overs)
    }
    val target = revised?.runs ?: (r1 + 1)
    val secondLimit = (revised?.overs ?: r.overs) * r.ballsPerOver
    val r2: Int
    val w2: Int
    val b2: Int
    when (rng.int(4)) {
        0 -> { r2 = target + rng.int(8); w2 = rng.int(wMax); b2 = rng.int(secondLimit + 1) }
        1 -> { r2 = maxOf(0, target - 1); w2 = rng.int(wMax + 1); b2 = secondLimit }
        2 -> {
            r2 = maxOf(0, target - 2 - rng.int(60)); w2 = rng.int(wMax + 1)
            b2 = if (w2 == wMax) rng.int(secondLimit + 1) else secondLimit
        }
        else -> { r2 = rng.int(300); w2 = rng.int(wMax + 2); b2 = rng.int(secondLimit + 3) }
    }
    var res = CricketResult(
        innings = listOf(CricketInnings(firstBat, r1, w1, b1), CricketInnings(secondBat, r2, w2, b2)),
        revisedTarget = revised,
    )
    if (rng.int(3) == 0) res = res.copy(superOverWinnerRegistrationId = if (rng.int(2) == 0) a else b)
    return res
}

/** A VALID scorecard for a match; retries the generator. */
internal fun validScorecard(rng: Lcg, rules: CricketRules, a: String, b: String, needWinner: Boolean): CricketResult {
    repeat(500) {
        val res = randomScorecard(rng, rules, a, b)
        if (CricketScoring.validate(res, rules, a, b).isNotEmpty()) return@repeat
        if (needWinner && CricketScoring.outcome(res, rules).winnerRegistrationId == null) return@repeat
        return res
    }
    error("no valid scorecard")
}

internal fun standingsResult(teamA: String, teamB: String, res: CricketResult, rules: CricketRules): StandingsResult {
    val o = CricketScoring.outcome(res, rules)
    return StandingsResult(teamA, teamB, o.winnerRegistrationId, 0, 0, 0, 0,
        cricket = res, noResult = o.resultType == CricketResultType.NO_RESULT)
}

internal val defaultPoints = StandingsPoints(2, 1, 0)

internal fun cricketTable(teams: List<Registration>, results: List<StandingsResult>, rules: CricketRules): List<StandingsEntry> {
    val entries = teams.associate { idOf(it) to StandingsEntry(idOf(it), it.teamName ?: "") }
    StandingsEntry.tally(results, entries, defaultPoints, rules)
    return StandingsEntry.ranked(entries.values, results, SportType.CRICKET, TieBreaker.HEAD_TO_HEAD, defaultPoints)
}

internal class KnockoutRun(val rounds: List<List<Pair<Registration, Registration>>>, val champion: Registration?, val problems: List<String>)

/**
 * Plays a knockout to a champion like TournamentDetailViewModel: round 1 from
 * [seeded], then knockoutByeTeams + nextRoundPairs.
 */
internal fun playKnockout(
    seeded: List<Registration>,
    seedForByes: (() -> List<Registration>)? = null,
    beforeAdvancing: (List<Registration>) -> Unit = {},
    winner: (Registration, Registration) -> Registration,
): KnockoutRun {
    val problems = mutableListOf<String>()
    val first = PairingService.generateBracketFirstRoundSeeded(seeded)
    val rounds = mutableListOf(first.matches.map { it.first to it.second })
    var champion: Registration? = null
    while (true) {
        val current = rounds.last()
        val ids = current.flatMap { listOf(it.first.id, it.second.id) }
        if (ids.toSet().size != ids.size) problems.add("team twice in round ${rounds.size}")
        if (current.any { it.first.id == it.second.id }) problems.add("self match")
        var winners = current.map { winner(it.first, it.second) }
        if (rounds.size == 1) {
            beforeAdvancing(winners)
            val r1 = current.flatMap { listOf(idOf(it.first), idOf(it.second)) }.toSet()
            winners = PairingService.knockoutByeTeams(seedForByes?.invoke() ?: seeded, r1) + winners
        }
        if (winners.size < 2) { champion = winners.firstOrNull(); break }
        if (winners.size % 2 != 0) problems.add("odd field ${winners.size}")
        rounds.add(PairingService.nextRoundPairs(winners))
        if (rounds.size > 12) { problems.add("too many rounds"); break }
    }
    return KnockoutRun(rounds, champion, problems)
}
