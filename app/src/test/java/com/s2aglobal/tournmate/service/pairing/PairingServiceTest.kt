package com.s2aglobal.tournmate.service.pairing

import com.s2aglobal.tournmate.domain.model.Player
import com.s2aglobal.tournmate.domain.model.Registration
import com.s2aglobal.tournmate.domain.model.SportType
import com.s2aglobal.tournmate.domain.model.Tournament
import com.s2aglobal.tournmate.domain.model.TournamentFormat
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs

class PairingServiceTest {

    private val tournament = Tournament(sportType = SportType.BADMINTON)

    /** Entries with strictly decreasing Elo, so seeding order = list order. */
    private fun teams(n: Int): List<Registration> = (0 until n).map { i ->
        val player = Player(name = "P$i", eloRatings = mapOf("badminton" to 2000.0 - i))
        Registration(tournament = tournament, player = player, playerId = player.id.toString())
    }

    private fun key(r: Registration) = r.id.toString()

    // ── Round robin (circle method) ──

    @Test fun roundRobinEveryPairOnceAndRoundsValid() {
        for (n in 2..32) {
            val t = teams(n)
            val schedule = PairingService.generateRoundRobinSchedule(t)
            assertEquals("matches for $n", n * (n - 1) / 2, schedule.size)

            val pairs = schedule.map { setOf(key(it.first), key(it.second)) }
            assertEquals("unique pairs for $n", pairs.size, pairs.toSet().size)
            assertTrue("no self matches for $n", pairs.all { it.size == 2 })

            val rounds = schedule.groupBy { it.third }
            val expectedRounds = if (n % 2 == 0) n - 1 else n
            assertEquals("rounds for $n", expectedRounds, rounds.size)
            assertEquals((1..expectedRounds).toSet(), rounds.keys)
            rounds.forEach { (round, matches) ->
                assertEquals("matches in round $round for $n", n / 2, matches.size)
                val ids = matches.flatMap { listOf(key(it.first), key(it.second)) }
                assertEquals("team twice in round $round for $n", ids.size, ids.toSet().size)
            }
            if (n % 2 == 1) {
                // Each team rests exactly once.
                t.forEach { team ->
                    val played = rounds.count { (_, ms) -> ms.any { key(it.first) == key(team) || key(it.second) == key(team) } }
                    assertEquals("rests for $n", expectedRounds - 1, played)
                }
            }
        }
    }

    @Test fun roundRobinHomeAwayBalanced() {
        for (n in 2..64) {
            val schedule = PairingService.generateRoundRobinSchedule(teams(n))
            val firstCounts = schedule.groupingBy { key(it.first) }.eachCount()
            val ids = schedule.flatMap { listOf(key(it.first), key(it.second)) }.toSet()
            ids.forEach { id ->
                val count = firstCounts[id] ?: 0
                assertTrue("n=$n team listed first $count times", abs(count - (n - 1) / 2.0) <= 1.0)
            }
        }
    }

    @Test fun roundRobinTooFewTeams() {
        assertTrue(PairingService.generateRoundRobinSchedule(emptyList()).isEmpty())
        assertTrue(PairingService.generateRoundRobinSchedule(teams(1)).isEmpty())
    }

    // ── Bracket ──

    @Test fun bracketFirstRoundByesGoToTopSeeds() {
        for (n in 2..64) {
            val t = teams(n)
            var size = 1
            while (size < n) size *= 2
            val result = PairingService.generateBracketFirstRound(t)
            val byes = size - n
            assertEquals("byes for $n", byes, result.byeTeams.size)
            assertEquals("top seeds get byes for $n", t.take(byes).map(::key), result.byeTeams.map(::key))
            assertEquals("first-round matches for $n", (n - byes) / 2, result.matches.size)
            val playing = result.matches.flatMap { listOf(key(it.first), key(it.second)) }
            assertEquals((t.map(::key) - result.byeTeams.map(::key).toSet()).toSet(), playing.toSet())
            assertEquals(playing.size, playing.toSet().size)
        }
    }

    /** Plays out full knockouts with advanceToNextRound's rule (higher seed always wins). */
    @Test fun fullKnockoutProducesOneChampion() {
        val sizes = (2..40) + listOf(48, 64, 100)
        for (n in sizes) {
            val t = teams(n)
            val seed = t.withIndex().associate { key(it.value) to it.index }
            fun winner(a: Registration, b: Registration) = if (seed.getValue(key(a)) < seed.getValue(key(b))) a else b

            val first = PairingService.generateBracketFirstRound(t)
            var matchCount = first.matches.size
            var advancing = first.byeTeams + first.matches.map { winner(it.first, it.second) }
            var rounds = 1
            while (advancing.size > 1) {
                assertEquals("odd field in round ${rounds + 1} for $n", 0, advancing.size % 2)
                val pairs = PairingService.nextRoundPairs(advancing)
                matchCount += pairs.size
                advancing = pairs.map { winner(it.first, it.second) }
                rounds++
            }
            assertEquals("champion for $n", 1, advancing.size)
            assertEquals("top seed wins for $n", key(t[0]), key(advancing[0]))
            assertEquals("matches for $n", n - 1, matchCount)
            assertEquals("rounds for $n", first.totalRounds, rounds)
        }
    }

    // ── Groups ──

    @Test fun groupStageBalancedAndComplete() {
        for (advancing in 1..2) {
            for (n in 4..40) {
                val t = teams(n)
                val schedule = PairingService.generateGroupStageSchedule(t, groupCount = 2, advancingPerGroup = advancing)
                val groups = schedule.groupBy { it.group }
                val members = groups.mapValues { (_, ms) -> ms.flatMap { listOf(key(it.teamA), key(it.teamB)) }.toSet() }
                assertEquals("group count for n=$n adv=$advancing", PairingService.optimalGroupCount(n, advancing), groups.size)

                val sizes = members.values.map { it.size }
                assertTrue("balanced n=$n adv=$advancing $sizes", sizes.max() - sizes.min() <= 1)
                assertTrue("group larger than advancing n=$n adv=$advancing $sizes", sizes.all { it > advancing })
                val all = members.values.flatten()
                assertEquals("every team placed once n=$n adv=$advancing", n, all.size)
                assertEquals(t.map(::key).toSet(), all.toSet())

                groups.forEach { (label, ms) ->
                    val size = members.getValue(label).size
                    assertEquals("complete RR in $label n=$n", size * (size - 1) / 2, ms.size)
                    assertEquals(ms.size, ms.map { setOf(key(it.teamA), key(it.teamB)) }.toSet().size)
                }
            }
        }
    }

    @Test fun optimalGroupCountSmallFields() {
        assertEquals(1, PairingService.optimalGroupCount(4, 2))
        assertEquals(1, PairingService.optimalGroupCount(5, 2))
        assertEquals(2, PairingService.optimalGroupCount(6, 2))
        assertEquals(2, PairingService.optimalGroupCount(4, 1))
        assertEquals(2, PairingService.optimalGroupCount(3, 2))
    }

    // ── Seeding ──

    @Test fun teamEntrySeedsOnRosterAverage() {
        val cricket = Tournament(sportType = SportType.CRICKET, formatRaw = TournamentFormat.TEAM.rawValue)
        val captain = Player(name = "C", eloRatings = mapOf("cricket" to 1000.0))
        val roster = listOf(1300.0, 1500.0).map { Player(eloRatings = mapOf("cricket" to it)) }
        val reg = Registration(tournament = cricket, player = captain, teamName = "Kings", roster = roster)
        assertEquals(1400.0, PairingService.avgElo(reg), 1e-9)
        val noRoster = reg.copy(roster = emptyList())
        assertEquals(1000.0, PairingService.avgElo(noRoster), 1e-9)
    }
}
