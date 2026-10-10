package com.s2aglobal.tournmate.service.pairing

import com.s2aglobal.tournmate.domain.model.CricketEventType
import com.s2aglobal.tournmate.domain.model.CricketRules
import com.s2aglobal.tournmate.domain.model.CricketRunRate
import com.s2aglobal.tournmate.domain.model.CricketScoring
import com.s2aglobal.tournmate.domain.model.Match
import com.s2aglobal.tournmate.domain.model.MatchFormat
import com.s2aglobal.tournmate.domain.model.MatchStatus
import com.s2aglobal.tournmate.domain.model.Registration
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Cross-platform parity: builds the same fixed-seed dump as iOS
 * `Tools/DrawTests/main.swift` (round robin, knockout brackets, groups and
 * tables, Swiss rounds, 400 random cricket scorecards) and compares it line by
 * line with `draw-parity-fixture.txt`, a copy of iOS
 * `Tools/DrawTests/parity-fixture.txt`. Any difference means the two apps
 * would draw or score differently.
 */
class DrawParityTest {

    private fun dump(): List<String> {
        val out = mutableListOf<String>()
        for (n in 0..12) {
            val teams = makeTeams(n, makeTournament(MatchFormat.ROUND_ROBIN))
            for (d in listOf(false, true)) {
                val s = PairingService.generateRoundRobinSchedule(teams, d)
                out.add("RR n=$n double=$d: " + s.joinToString(" ") { "${it.third}:${label(it.first)}-${label(it.second)}" })
            }
        }
        for (n in 0..12) {
            for (tied in listOf(false, true)) {
                val teams = makeTeams(n, makeTournament(MatchFormat.SINGLE_ELIMINATION), tied)
                val seeded = PairingService.seededByRating(teams)
                val first = PairingService.generateBracketFirstRoundSeeded(seeded)
                out.add("KO n=$n tied=$tied: rounds=${first.totalRounds} byes=${labels(first.byeTeams)} r1=" +
                    first.matches.joinToString(" ") { "${it.third}:${label(it.first)}-${label(it.second)}" })
                if (n < 2) continue
                val rng = Lcg(100_000 + n * 10 + (if (tied) 1 else 0))
                val ko = playKnockout(seeded) { a, b -> if (rng.int(2) == 0) a else b }
                ko.rounds.forEachIndexed { ri, round ->
                    if (ri > 0) out.add("  round ${ri + 1}: " + round.joinToString(" ") { "${label(it.first)}-${label(it.second)}" })
                }
                out.add("  champion=${ko.champion?.let { label(it) } ?: "-"}")
            }
        }
        val rules = CricketRules.DEFAULT
        for (n in 4..16) {
            for (adv in 1..4) {
                val teams = makeTeams(n, makeTournament(MatchFormat.GROUP_KNOCKOUT))
                val s = PairingService.generateGroupStageSchedule(teams, groupCount = 2, advancingPerGroup = adv)
                val members = linkedMapOf<String, MutableList<Registration>>()
                for (e in s) {
                    for (x in listOf(e.teamA, e.teamB)) {
                        val list = members.getOrPut(e.group) { mutableListOf() }
                        if (list.none { it.id == x.id }) list.add(x)
                    }
                }
                val groupLabels = members.keys.sorted()
                out.add("GK n=$n adv=$adv: " + groupLabels.joinToString(" ") { "$it=${labels(members.getValue(it))}" })
                out.add("  matches: " + s.joinToString(" ") { "${it.group}${it.round}:${label(it.teamA)}-${label(it.teamB)}" })
                val rng = Lcg(200_000 + n * 10 + adv)
                val results = s.map { e ->
                    val res = validScorecard(rng, rules, idOf(e.teamA), idOf(e.teamB), needWinner = false)
                    e.group to standingsResult(idOf(e.teamA), idOf(e.teamB), res, rules)
                }
                val byId = teams.associateBy { idOf(it) }
                val tables = groupLabels.map { grp ->
                    val table = cricketTable(members.getValue(grp), results.filter { it.first == grp }.map { it.second }, rules)
                    out.add("  table $grp: " + table.joinToString(", ") {
                        "${label(byId.getValue(it.registrationId))} ${it.points} ${CricketRunRate.format(it.netRunRate)}"
                    })
                    table.mapNotNull { byId[it.registrationId] }
                }
                val seeded = PairingService.knockoutSeedingFromGroups(tables, adv)
                val first = PairingService.generateBracketFirstRoundSeeded(seeded)
                out.add("  seeds=${labels(seeded)} byes=${labels(first.byeTeams)} r1=" +
                    first.matches.joinToString(" ") { "${label(it.first)}-${label(it.second)}" })
            }
        }
        for (n in 2..12) {
            val t = makeTournament(MatchFormat.SWISS)
            val teams = makeTeams(n, t)
            val played = mutableListOf<Match>()
            val rng = Lcg(300_000 + n)
            for (r in 1..n) {
                val pairs = PairingService.generateSwissPairings(teams, played, r)
                out.add("SW n=$n r=$r: " + pairs.joinToString(" ") { "${label(it.first)}-${label(it.second)}" })
                for ((a, b) in pairs) {
                    val winner = when (rng.int(3)) { 0 -> idOf(a); 1 -> idOf(b); else -> null }
                    played.add(Match(tournament = t, teamA = a, teamB = b, teamAId = idOf(a), teamBId = idOf(b), round = r,
                        winnerRegistrationId = winner, statusRaw = MatchStatus.FINISHED.rawValue))
                }
            }
        }
        val ruleSets = CricketEventType.presets.map { it.defaultRules } + listOf(
            CricketRules(CricketEventType.CUSTOM, playersPerSide = 8, overs = 5, ballsPerOver = 8),
            CricketRules(CricketEventType.CUSTOM, playersPerSide = 6, overs = 10, ballsPerOver = 4, lastManStands = true),
        )
        val rng = Lcg(400_000)
        val a = uuid(1).toString().uppercase()
        val b = uuid(2).toString().uppercase()
        fun side(id: String?) = if (id == null) "-" else if (id == a) "A" else "B"
        for (k in 0 until 400) {
            val r = ruleSets[k % ruleSets.size].copy(superOver = rng.int(2) == 0)
            val card = randomScorecard(rng, r, a, b)
            val issues = CricketScoring.validate(card, r, a, b)
            var line = "C$k ${r.eventType.rawValue}/${r.overs}x${r.ballsPerOver}/so=${r.superOver}: "
            val firstIssue = issues.firstOrNull()
            line += if (firstIssue != null) {
                "invalid ${firstIssue.field}: ${firstIssue.message}"
            } else {
                val o = CricketScoring.outcome(card, r)
                val nrr = CricketRunRate().apply { add(card, a, r) }
                "${o.resultType.rawValue} ${side(o.winnerRegistrationId)} ${o.margin} nrrA=${CricketRunRate.format(nrr.netRunRate(r.ballsPerOver))}"
            }
            out.add(line)
        }
        return out
    }

    @Test fun identicalToIosFixture() {
        val fixture = javaClass.getResource("/draw-parity-fixture.txt")?.readText()
            ?: error("draw-parity-fixture.txt missing from test resources")
        val expected = fixture.removeSuffix("\n").split("\n")
        val actual = dump()
        val firstDiff = expected.indices.firstOrNull { it >= actual.size || expected[it] != actual[it] }
        assertTrue(
            "first difference at line ${firstDiff?.plus(1)}:\n  iOS:     ${firstDiff?.let { expected[it] }}\n  Android: ${firstDiff?.let { actual.getOrNull(it) }}",
            firstDiff == null,
        )
        assertEquals("line count", expected.size, actual.size)
    }
}
