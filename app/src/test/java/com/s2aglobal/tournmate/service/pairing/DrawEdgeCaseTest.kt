package com.s2aglobal.tournmate.service.pairing

import com.s2aglobal.tournmate.domain.model.CricketResult
import com.s2aglobal.tournmate.domain.model.CricketRules
import com.s2aglobal.tournmate.domain.model.Match
import com.s2aglobal.tournmate.domain.model.MatchFormat
import com.s2aglobal.tournmate.domain.model.MatchStatus
import com.s2aglobal.tournmate.domain.model.Player
import com.s2aglobal.tournmate.domain.model.Registration
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs
import kotlin.math.log2

/**
 * Draw edge cases, mirroring iOS `Tools/DrawTests/main.swift` (the iOS harness
 * runs the same scenarios at larger sizes).
 */
class DrawEdgeCaseTest {

    private fun pairKey(a: Registration, b: Registration): String {
        val x = idOf(a); val y = idOf(b)
        return if (x < y) x + y else y + x
    }

    // ── 1. Tiny fields ──

    @Test fun tinyFieldsEveryFormat() {
        for (n in 0..3) {
            val teams = makeTeams(n, makeTournament(MatchFormat.ROUND_ROBIN))
            assertEquals("RR $n", maxOf(0, n * (n - 1) / 2), PairingService.generateRoundRobinSchedule(teams).size)
            assertEquals("DRR $n", maxOf(0, n * (n - 1)), PairingService.generateRoundRobinSchedule(teams, true).size)
            val ko = PairingService.generateBracketFirstRound(teams)
            val expected = listOf(Triple(0, 0, 0), Triple(0, 0, 0), Triple(1, 0, 1), Triple(1, 1, 2))[n]
            assertEquals("KO $n", expected, Triple(ko.matches.size, ko.byeTeams.size, ko.totalRounds))
            assertTrue("GK $n", PairingService.generateGroupStageSchedule(teams, 2, 2).isEmpty())
            assertEquals("Swiss $n", n / 2, PairingService.generateSwissPairings(teams, emptyList(), 1).size)
            assertEquals("seeding from groups $n", minOf(n, 2), PairingService.knockoutSeedingFromGroups(listOf(teams), 2).size)
            assertEquals("nextRoundPairs $n", n / 2, PairingService.nextRoundPairs(teams).size)
        }
        assertTrue(PairingService.knockoutSeedingFromGroups(emptyList(), 2).isEmpty())
        val four = makeTeams(4, makeTournament(MatchFormat.GROUP_KNOCKOUT))
        assertTrue("GK minimum of 4 generates", PairingService.generateGroupStageSchedule(four, 2, 2).isNotEmpty())
        val t2 = makeTournament(MatchFormat.SWISS)
        val two = makeTeams(2, t2)
        val m = Match(tournament = t2, teamA = two[0], teamB = two[1], teamAId = idOf(two[0]), teamBId = idOf(two[1]), round = 1,
            winnerRegistrationId = idOf(two[0]), statusRaw = MatchStatus.FINISHED.rawValue)
        assertTrue("Swiss 2 teams round 2", PairingService.generateSwissPairings(two, listOf(m), 2).isEmpty())
    }

    // ── 2. Round robin ──

    @Test fun doubleRoundRobin() {
        for (n in (2..40) + listOf(64, 128, 200)) {
            val teams = makeTeams(n, makeTournament(MatchFormat.ROUND_ROBIN))
            val start = System.nanoTime()
            val leg = PairingService.generateRoundRobinSchedule(teams)
            val s = PairingService.generateRoundRobinSchedule(teams, true)
            assertTrue("fast $n", (System.nanoTime() - start) / 1e9 < 2)
            val legRounds = if (n % 2 == 0) n - 1 else n
            assertEquals("count $n", n * (n - 1), s.size)
            assertEquals("rounds $n", (1..2 * legRounds).toSet(), s.map { it.third }.toSet())
            assertEquals("leg 1 unchanged $n", leg, s.take(leg.size))
            s.drop(leg.size).zip(leg).forEach { (b, a) ->
                assertEquals(a.first.id, b.second.id); assertEquals(a.second.id, b.first.id); assertEquals(a.third + legRounds, b.third)
            }
            val ordered = s.groupingBy { idOf(it.first) + ">" + idOf(it.second) }.eachCount()
            assertEquals("each ordered pair once $n", n * (n - 1), ordered.size)
            val home = s.groupingBy { idOf(it.first) }.eachCount()
            assertTrue("half at home $n", teams.all { home[idOf(it)] == n - 1 })
            if (n % 2 == 1) teams.forEach { t ->
                val rounds = s.filter { it.first.id == t.id || it.second.id == t.id }.map { it.third }
                assertEquals("one bye per leg $n", n - 1, rounds.count { it <= legRounds })
                assertEquals("one bye per leg $n", n - 1, rounds.count { it > legRounds })
            }
        }
    }

    // ── 3. Knockout ──

    @Test fun knockoutTwoTo130() {
        for (n in 2..130) {
            val teams = makeTeams(n, makeTournament(MatchFormat.SINGLE_ELIMINATION))
            val seeded = teams.sortedByDescending { rating(label(it).drop(1).toInt(), false) }
            assertEquals(seeded.map { it.id }, PairingService.seededByRating(teams).map { it.id })
            val first = PairingService.generateBracketFirstRound(teams)
            var size = 1; while (size < n) size *= 2
            assertEquals("byes $n", seeded.take(size - n).map { it.id }, first.byeTeams.map { it.id })
            assertEquals("rounds $n", log2(size.toDouble()).toInt(), first.totalRounds)

            val seedOf = seeded.withIndex().associate { it.value.id to it.index + 1 }
            val fav = playKnockout(seeded) { a, b -> if (seedOf.getValue(a.id) < seedOf.getValue(b.id)) a else b }
            assertTrue("favourites $n ${fav.problems}", fav.problems.isEmpty())
            assertEquals(seeded[0].id, fav.champion?.id)
            fav.rounds.forEachIndexed { ri, round ->
                if (ri > 0) assertTrue("standard bracket $n r${ri + 1}",
                    round.all { seedOf.getValue(it.first.id) + seedOf.getValue(it.second.id) == round.size * 2 + 1 })
            }
            assertEquals("final 1 v 2 $n", setOf(1, 2), fav.rounds.last().first().let { setOf(seedOf[it.first.id], seedOf[it.second.id]) })

            // Ratings move after round 1 (Elo): byes must still be the drawn byes.
            val rng = Lcg(n * 7919)
            val run = playKnockout(seeded, seedForByes = {
                PairingService.seededByRating(teams.map { t -> t.copy(roster = t.roster.map { it.withElo(it.elo(com.s2aglobal.tournmate.domain.model.SportType.CRICKET) + (if (t.id in winnersRef) 900 else 0), com.s2aglobal.tournmate.domain.model.SportType.CRICKET) }) })
            }, beforeAdvancing = { w -> winnersRef = w.map { it.id }.toSet() }) { a, b -> if (rng.int(2) == 0) a else b }
            assertTrue("random $n ${run.problems}", run.problems.isEmpty())
            assertNotNull(run.champion)
            assertEquals("matches $n", n - 1, run.rounds.sumOf { it.size })
            assertEquals("every team plays $n", n, run.rounds.flatten().flatMap { listOf(it.first.id, it.second.id) }.toSet().size)
        }
    }

    private var winnersRef: Set<java.util.UUID> = emptySet()

    @Test fun legacyReseedReproAndFix() {
        val teams = makeTeams(6, makeTournament(MatchFormat.SINGLE_ELIMINATION))
        val first = PairingService.generateBracketFirstRound(teams)
        val w1 = first.matches.map { it.first }
        val bumped = teams.map { t -> if (t.id == w1[0].id) t.copy(roster = t.roster.map { it.withElo(5000.0, com.s2aglobal.tournmate.domain.model.SportType.CRICKET) }) else t }
        val legacy = PairingService.generateBracketFirstRound(bumped).byeTeams + w1
        assertTrue("legacy duplicates", legacy.map { it.id }.toSet().size < legacy.size)
        val r1 = first.matches.flatMap { listOf(idOf(it.first), idOf(it.second)) }.toSet()
        val fixed = PairingService.knockoutByeTeams(PairingService.seededByRating(bumped), r1)
        assertEquals(first.byeTeams.map { it.id }.toSet(), fixed.map { it.id }.toSet())
    }

    @Test fun tiedRatingsKeepRegistrationOrder() {
        for (n in listOf(2, 3, 5, 8, 13, 32)) {
            val teams = makeTeams(n, makeTournament(MatchFormat.SINGLE_ELIMINATION), tied = true)
            assertEquals(teams.map { it.id }, PairingService.seededByRating(teams).map { it.id })
            val r = PairingService.generateBracketFirstRound(teams)
            assertEquals(teams.take(r.byeTeams.size).map { it.id }, r.byeTeams.map { it.id })
        }
    }

    // ── 5. Group + knockout ──

    @Test fun groupKnockoutFourTo64() {
        val rules = CricketRules.DEFAULT
        for (n in 4..64) for (adv in 1..4) {
            val teams = makeTeams(n, makeTournament(MatchFormat.GROUP_KNOCKOUT))
            val g = PairingService.optimalGroupCount(n, adv)
            val s = PairingService.generateGroupStageSchedule(teams, 2, adv)
            val members = linkedMapOf<String, MutableList<Registration>>()
            for (e in s) for (x in listOf(e.teamA, e.teamB)) {
                val l = members.getOrPut(e.group) { mutableListOf() }; if (l.none { it.id == x.id }) l.add(x)
            }
            assertEquals("groups $n/$adv", g, members.size)
            val sizes = members.values.map { it.size }
            assertEquals(n, sizes.sum())
            assertTrue(sizes.max() - sizes.min() <= 1)
            if (g >= 2) assertTrue(sizes.min() > adv) else assertTrue(n < 2 * (adv + 1))
            val rng = Lcg(n * 31 + adv)
            val results = s.map { e -> e.group to standingsResult(idOf(e.teamA), idOf(e.teamB),
                validScorecard(rng, rules, idOf(e.teamA), idOf(e.teamB), false), rules) }
            val byId = teams.associateBy { idOf(it) }
            val tables = members.keys.sorted().map { grp ->
                val rs = results.filter { it.first == grp }.map { it.second }
                val table = cricketTable(members.getValue(grp), rs, rules)
                assertEquals("deterministic $n/$adv", table.map { it.registrationId },
                    cricketTable(members.getValue(grp).reversed(), rs.reversed(), rules).map { it.registrationId })
                assertEquals("points $n/$adv", 2 * rs.size, table.sumOf { it.points })
                table.mapNotNull { byId[it.registrationId] }
            }
            val seeded = PairingService.knockoutSeedingFromGroups(tables, adv)
            assertEquals(tables.sumOf { minOf(adv, it.size) }, seeded.size)
            val groupOf = tables.withIndex().flatMap { (gi, tb) -> tb.map { it.id to gi } }.toMap()
            val first = PairingService.generateBracketFirstRoundSeeded(seeded)
            if (g >= 2) assertTrue("no same-group first round $n/$adv",
                first.matches.all { groupOf[it.first.id] != groupOf[it.second.id] })
            val ko = playKnockout(seeded) { a, b -> if (rng.int(2) == 0) a else b }
            assertTrue("ko $n/$adv ${ko.problems}", ko.problems.isEmpty())
            assertEquals(seeded.size - 1, ko.rounds.sumOf { it.size })
        }
    }

    @Test fun levelTableOrdersByNameThenId() {
        val rules = CricketRules.DEFAULT
        val teams = makeTeams(5, makeTournament(MatchFormat.GROUP_KNOCKOUT)).toMutableList()
        teams[3] = teams[3].copy(teamName = "Team 1")
        val s = PairingService.generateRoundRobinSchedule(teams)
        val results = s.map { standingsResult(idOf(it.first), idOf(it.second), CricketResult(emptyList(), noResult = true), rules) }
        val table = cricketTable(teams, results, rules)
        assertEquals(listOf(0, 1, 3, 2, 4).map { idOf(teams[it]) }, table.map { it.registrationId })
        assertEquals(2 * s.size, table.sumOf { it.points })
        assertEquals(table.map { it.registrationId }, cricketTable(teams.shuffled(java.util.Random(7)), results.reversed(), rules).map { it.registrationId })
    }

    // ── 6. Swiss ──

    private fun perfectPairingExists(ids: List<String>, played: Set<String>): Boolean {
        fun key(a: String, b: String) = if (a < b) a + b else b + a
        fun rec(rest: List<String>, byeLeft: Boolean): Boolean {
            val a = rest.firstOrNull() ?: return true
            val r = rest.drop(1)
            if (byeLeft && rec(r, false)) return true
            return r.withIndex().any { (j, b) -> key(a, b) !in played && rec(r.filterIndexed { k, _ -> k != j }, byeLeft) }
        }
        return rec(ids, ids.size % 2 == 1)
    }

    @Test fun swissNoAvoidableShortRoundsOrRepeatByes() {
        for (n in 2..14) for (trial in 0 until 3) {
            val t = makeTournament(MatchFormat.SWISS)
            val teams = makeTeams(n, t)
            val played = mutableListOf<Match>()
            val seen = mutableSetOf<String>()
            val byes = mutableMapOf<java.util.UUID, Int>()
            val rng = Lcg(n * 1000 + trial)
            for (r in 1..n + 1) {
                val pairs = PairingService.generateSwissPairings(teams, played, r)
                val playedSet = played.map { if (it.teamAId < it.teamBId) it.teamAId + it.teamBId else it.teamBId + it.teamAId }.toSet()
                pairs.forEach { assertTrue("rematch n=$n r=$r", seen.add(pairKey(it.first, it.second))) }
                if (pairs.size < n / 2) {
                    assertTrue("avoidable short round n=$n r=$r", !perfectPairingExists(teams.map { idOf(it) }, playedSet))
                } else if (n % 2 == 1) {
                    val inRound = pairs.flatMap { listOf(it.first.id, it.second.id) }.toSet()
                    val bye = teams.single { it.id !in inRound }
                    if ((byes[bye.id] ?: 0) >= 1) {
                        teams.filter { (byes[it.id] ?: 0) == 0 }.forEach { fresh ->
                            assertTrue("avoidable second bye n=$n r=$r",
                                !perfectPairingExists(teams.filter { it.id != fresh.id }.map { idOf(it) }, playedSet))
                        }
                    }
                    byes[bye.id] = (byes[bye.id] ?: 0) + 1
                }
                for ((a, b) in pairs) {
                    val w = when (rng.int(3)) { 0 -> idOf(a); 1 -> idOf(b); else -> null }
                    played.add(Match(tournament = t, teamA = a, teamB = b, teamAId = idOf(a), teamBId = idOf(b), round = r,
                        winnerRegistrationId = w, statusRaw = MatchStatus.FINISHED.rawValue))
                }
            }
        }
    }

    @Test fun swissTieIsNotAWin() {
        val t = makeTournament(MatchFormat.SWISS)
        val teams = makeTeams(4, t)
        val m1 = Match(tournament = t, teamA = teams[0], teamB = teams[1], teamAId = idOf(teams[0]), teamBId = idOf(teams[1]),
            round = 1, winnerRegistrationId = idOf(teams[0]), statusRaw = MatchStatus.FINISHED.rawValue)
        val m2 = Match(tournament = t, teamA = teams[2], teamB = teams[3], teamAId = idOf(teams[2]), teamBId = idOf(teams[3]),
            round = 1, statusRaw = MatchStatus.FINISHED.rawValue)
        val r2 = PairingService.generateSwissPairings(teams, listOf(m1, m2), 2)
        assertEquals(teams[0].id, r2.first().first.id)
        assertEquals(teams[3].id, r2.first().second.id)
    }

    // ── 8. Team entries ──

    @Test fun teamEntrySeeding() {
        val t = makeTournament(MatchFormat.SINGLE_ELIMINATION)
        val cricket = com.s2aglobal.tournmate.domain.model.SportType.CRICKET
        fun entry(i: Int, roster: List<Double>, captain: Double, guests: Int = 0, duplicate: Boolean = false): Registration {
            val cap = Player(id = uuid(5000 + i * 20), name = "C$i", eloRatings = mapOf("cricket" to captain))
            var players = roster.mapIndexed { j, e -> Player(id = uuid(5001 + i * 20 + j), eloRatings = mapOf("cricket" to e)) }
            if (duplicate) players = players + players.first()
            return Registration(id = uuid(900 + i), tournament = t, player = cap, teamName = "E$i", roster = players,
                rosterIds = players.map { it.id.toString().uppercase() }, guestNames = (0 until guests).map { "G$it" })
        }
        val guestsOnly = entry(0, emptyList(), 1500.0, guests = 5)
        val captainOff = entry(1, listOf(1100.0, 1300.0), 2000.0)
        val dup = entry(2, listOf(1400.0, 1000.0), 1000.0, duplicate = true)
        val two = entry(3, listOf(1250.0, 1250.0), 1250.0)
        val eleven = entry(4, List(11) { 1210.0 }, 1210.0)
        assertTrue(guestsOnly.isTeamFormed)
        assertEquals(2, dup.rosterCount)
        assertEquals(1200.0, PairingService.avgElo(dup), 1e-9)
        assertEquals(listOf("E0", "E3", "E4", "E2", "E1"),
            PairingService.seededByRating(listOf(dup, eleven, two, captainOff, guestsOnly)).map { it.teamName })
        val lone = entry(5, listOf(1300.0), 1300.0).let { it.copy(rosterIds = listOf(it.rosterIds[0], it.rosterIds[0].lowercase())) }
        assertTrue(!lone.isTeamFormed)
        assertTrue(abs(PairingService.avgElo(eleven) - 1210.0) < 1e-9)
        assertEquals(cricket, t.sportType)
    }
}
