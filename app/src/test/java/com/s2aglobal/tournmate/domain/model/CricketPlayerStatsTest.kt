package com.s2aglobal.tournmate.domain.model

import com.s2aglobal.tournmate.domain.model.CricketPlayerStatsFixtures.A
import com.s2aglobal.tournmate.domain.model.CricketPlayerStatsFixtures.B
import com.s2aglobal.tournmate.domain.model.CricketPlayerStatsFixtures.bat
import com.s2aglobal.tournmate.domain.model.CricketPlayerStatsFixtures.bowl
import com.s2aglobal.tournmate.domain.model.CricketPlayerStatsFixtures.card
import com.s2aglobal.tournmate.domain.model.CricketPlayerStatsFixtures.good
import com.s2aglobal.tournmate.domain.model.CricketPlayerStatsFixtures.inn
import com.s2aglobal.tournmate.domain.model.CricketPlayerStatsFixtures.m2
import com.s2aglobal.tournmate.domain.model.CricketPlayerStatsFixtures.names
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Port of the server's `check:cricket` "Player stats" cases (as mirrored in
 * the iOS DrawTests block "Cricket player stats"), plus the iOS round trip and
 * career checks. Messages and rounding must stay identical everywhere.
 */
class CricketPlayerStatsTest {

    private val R = CricketRates

    private fun msgs(s: CricketPlayerStats, c: CricketResult = card): List<String> =
        s.validate(c, A, B, names).map { it.message }

    private fun expect(s: CricketPlayerStats, message: String, c: CricketResult = card) {
        val m = msgs(s, c)
        assertTrue("expected \"$message\" in $m", m.contains(message))
    }

    private val nr = CricketResult(listOf(inn(A, 40, 1, "5")), noResult = true)

    // ── Validation ──

    @Test fun valid() = assertEquals(emptyList<String>(), msgs(good))
    @Test fun emptyValid() = assertEquals(emptyList<String>(), msgs(CricketPlayerStats()))
    @Test fun battingRunsOverTotal() = expect(CricketPlayerStats(batting = listOf(bat(A, "P1", 100, 60), bat(A, "P2", 59, 40))),
        "Strikers's batters scored 159 runs, more than the team's 158.")
    @Test fun battingBallsOverInnings() = expect(CricketPlayerStats(batting = listOf(bat(B, "Q1", 10, 111))),
        "Royals's batters faced 111 balls, more than the innings' 110.")
    @Test fun tooManyOuts() = expect(CricketPlayerStats(batting = (1..5).map { bat(B, "Q$it", 1, 1) }),
        "5 Royals batters are out, but the team lost 4 wickets.")
    @Test fun bowlingBallsOverInnings() = expect(CricketPlayerStats(bowling = listOf(bowl(A, "P3", 60, 50, 1), bowl(A, "P4", 51, 50, 1))),
        "Strikers's bowlers bowled 111 balls, more than the innings' 110.")
    @Test fun bowlingRunsOverTotal() = expect(CricketPlayerStats(bowling = listOf(bowl(B, "Q2", 24, 159, 1))),
        "Royals's bowlers conceded 159 runs, more than the 158 scored.")
    @Test fun bowlingWicketsOverFallen() = expect(CricketPlayerStats(bowling = listOf(bowl(A, "P3", 24, 20, 5))),
        "Strikers's bowlers took 5 wickets, more than the 4 that fell.")
    @Test fun unknownTeam() = expect(CricketPlayerStats(batting = listOf(bat("regX", "Z", 1, 1))), "Z isn't in either team.")
    @Test fun listedTwice() = expect(CricketPlayerStats(batting = listOf(bat(A, "p1", 1, 1), bat(A, "P1", 2, 2))), "P1 is listed twice.")
    @Test fun negative() = expect(CricketPlayerStats(batting = listOf(bat(A, "P1", -1, 1))), "P1's figures must be whole numbers, 0 or more.")
    @Test fun teamThatDidNotBat() = expect(CricketPlayerStats(batting = listOf(bat(B, "Q1", 1, 1))), "Royals didn't bat.", nr)
    @Test fun teamThatDidNotBowl() = expect(CricketPlayerStats(bowling = listOf(bowl(A, "P3", 6, 5, 0))), "Strikers didn't bowl.", nr)
    @Test fun partialNoResultOk() = assertEquals(
        emptyList<String>(),
        msgs(CricketPlayerStats(batting = listOf(bat(A, "P1", 30, 20, false)), bowling = listOf(bowl(B, "Q2", 18, 22, 1))), nr),
    )

    // ── Rates (integer rounding, half up) ──

    @Test fun sr45of30() = assertEquals("150.0", R.formatTenths(R.strikeRateTenths(45, 30)))
    @Test fun sr1of3() = assertEquals("33.3", R.formatTenths(R.strikeRateTenths(1, 3)))
    @Test fun sr2of3() = assertEquals("66.7", R.formatTenths(R.strikeRateTenths(2, 3)))
    @Test fun srHalfUp1of16() = assertEquals("6.3", R.formatTenths(R.strikeRateTenths(1, 16)))
    @Test fun srNoBalls() = assertEquals("–", R.formatTenths(R.strikeRateTenths(5, 0)))
    @Test fun econ41of24() = assertEquals("10.25", R.formatHundredths(R.economyHundredths(41, 24)))
    @Test fun econ35of20() = assertEquals("10.50", R.formatHundredths(R.economyHundredths(35, 20)))
    @Test fun econHalfUp1of8() = assertEquals("0.75", R.formatHundredths(R.economyHundredths(1, 8)))
    @Test fun econ7of23() = assertEquals("1.83", R.formatHundredths(R.economyHundredths(7, 23)))
    @Test fun avg105of2() = assertEquals("52.50", R.formatHundredths(R.averageHundredths(105, 2)))
    @Test fun avg10of3() = assertEquals("3.33", R.formatHundredths(R.averageHundredths(10, 3)))
    @Test fun avgNeverOut() = assertEquals("–", R.formatHundredths(R.averageHundredths(10, 0)))

    // ── Leaderboards ──

    private val bl get() = CricketLeaderboards.batting(listOf(good, m2))
    private val wl get() = CricketLeaderboards.bowling(listOf(good, m2))

    @Test fun battingOrder() =
        assertEquals("P2:150 guest:Ravi:145 P1:80 Q1:80", bl.joinToString(" ") { "${it.key}:${it.runs}" })

    @Test fun levelRunsHigherStrikeRateFirst() {
        val srTie = CricketLeaderboards.batting(listOf(CricketPlayerStats(batting = listOf(bat(A, "Slow", 30, 40), bat(B, "Fast", 30, 20)))))
        assertEquals("FAST", srTie.first().key)
    }

    @Test fun highestWithNotOut() {
        val p2 = bl.first { it.key == "P2" }
        assertTrue(p2.highestText == "105" && p2.notOuts == 1 && p2.innings == 2)
    }

    @Test fun guestHighestNotOut() {
        val ravi = bl.first { it.key == "guest:Ravi" }
        assertEquals("75", ravi.highestText)
        assertEquals("70*", ravi.copy(highest = 70, highestNotOut = true).highestText)
    }

    @Test fun mostWicketsThenEconomy() {
        val w = wl
        assertTrue(w[0].key == "Q2" && w[0].wickets == 6 && w[1].key == "P3" && w[1].wickets == 5)
    }

    @Test fun bestFigures() {
        assertEquals("3/18", wl.first { it.key == "P3" }.bestText)
        assertEquals("3/25", wl.first { it.key == "Q2" }.bestText)
    }

    @Test fun wicketlessBowlerLast() {
        assertEquals("Q9", wl.last().key)
        assertEquals("0/9", wl.last().bestText)
    }

    @Test fun keyCaseMerges() = assertEquals(
        1,
        CricketLeaderboards.batting(listOf(CricketPlayerStats(batting = listOf(bat(A, "p1", 5, 5), bat(A, "P1", 6, 6))))).size,
    )

    // ── Stored JSON ──

    @Test fun decodePlayerStats() = assertEquals(3, CricketPlayerStats.decode(good.encoded())?.bowling?.size)
    @Test fun decodeRejectsJunk() = assertNull(CricketPlayerStats.decode("""{"batting":[{"runs":"x"}]}"""))
    @Test fun decodeMissing() = assertNull(CricketPlayerStats.decode(null))
    @Test fun roundTrip() = assertEquals(good, CricketPlayerStats.decode(good.encoded()))

    @Test fun decodeRejectsNegative() = assertNull(CricketPlayerStats.decode(
        """{"bowling":[{"balls":6,"key":"P3","name":"P3","runs":-1,"teamRegistrationId":"regA","wickets":0}]}""",
    ))

    @Test fun encodesSortedKeys() = assertEquals(
        """{"batting":[{"balls":3,"key":"guest:Ravi","name":"guest:Ravi","out":false,"runs":7,"teamRegistrationId":"regB"}],""" +
            """"bowling":[{"balls":6,"key":"P3","name":"P3","runs":5,"teamRegistrationId":"regA","wickets":1}]}""",
        CricketPlayerStats(listOf(bat(B, "guest:Ravi", 7, 3, false)), listOf(bowl(A, "P3", 6, 5, 1))).encoded(),
    )

    // ── Career ──

    @Test fun careerMergesTeams() {
        val career = CricketLeaderboards.career("p1", listOf(good, m2, CricketPlayerStats(batting = listOf(bat(B, "P1", 7, 3, false)))))
        assertTrue(career.batting?.runs == 87 && career.batting?.innings == 3 && career.bowling == null)
    }
}
