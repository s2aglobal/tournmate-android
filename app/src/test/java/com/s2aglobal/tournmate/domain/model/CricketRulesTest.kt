package com.s2aglobal.tournmate.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlin.math.abs

/**
 * Port of the server's `functions/src/scripts/checkCricket.ts` cases. Messages
 * and margins must stay identical to the server and iOS.
 */
class CricketRulesTest {

    private val a = "regA"
    private val b = "regB"
    private val t20 = CricketRules.DEFAULT
    private val odi = CricketEventType.ODI.defaultRules
    private val box6 = CricketEventType.BOX6.defaultRules

    private fun inn(side: String, runs: Int, wickets: Int, overs: String, bpo: Int = 6): CricketInnings {
        val balls = CricketOvers.parse(overs, bpo) ?: error("bad overs $overs")
        return CricketInnings(side, runs, wickets, balls)
    }

    private fun expectValid(result: CricketResult, rules: CricketRules, margin: String, winner: String?) {
        val issues = CricketScoring.validate(result, rules, a, b)
        assertTrue("unexpected issues: $issues", issues.isEmpty())
        val o = CricketScoring.outcome(result, rules)
        assertEquals(margin, o.margin)
        assertEquals(winner, o.winnerRegistrationId)
    }

    private fun expectInvalid(result: CricketResult, rules: CricketRules, message: String) {
        val issues = CricketScoring.validate(result, rules, a, b)
        assertTrue("expected \"$message\" in ${issues.map { it.message }}", issues.any { it.message == message })
    }

    // ── Overs notation ──

    @Test fun parse18point2() = assertEquals(110, CricketOvers.parse("18.2"))
    @Test fun parse20() = assertEquals(120, CricketOvers.parse("20"))
    @Test fun parse0point5() = assertEquals(5, CricketOvers.parse("0.5"))
    @Test fun reject18point6() = assertNull(CricketOvers.parse("18.6"))
    @Test fun rejectText() = assertNull(CricketOvers.parse("abc"))
    @Test fun eightBallOver() = assertEquals(31, CricketOvers.parse("3.7", 8))
    @Test fun format110() = assertEquals("18.2", CricketOvers.format(110))
    @Test fun format120() = assertEquals("20", CricketOvers.format(120))

    // ── Rules ──

    @Test fun rulesDefault() = assertEquals(t20, CricketRules.decode("{}"))
    @Test fun rulesBox6() = assertEquals(true, CricketRules.decode("""{"eventTypeId":"box6"}""")?.lastManStands)
    @Test fun rulesOdi() = assertEquals(50, CricketRules.decode("""{"eventTypeId":"odi"}""")?.overs)
    @Test fun rulesCustomOvers() = assertEquals(15, CricketRules.decode("""{"eventTypeId":"t20","overs":15}""")?.overs)
    @Test fun rulesRejectZeroOvers() = assertNull(CricketRules.decode("""{"overs":0}"""))
    @Test fun rulesRejectBadJson() = assertNull(CricketRules.decode("{"))

    @Test fun rulesRejectOutOfRange() {
        assertNull(CricketRules.decode("""{"playersPerSide":1}"""))
        assertNull(CricketRules.decode("""{"playersPerSide":12}"""))
        assertNull(CricketRules.decode("""{"overs":51}"""))
        assertNull(CricketRules.decode("""{"ballsPerOver":3}"""))
        assertNull(CricketRules.decode("""{"ballsPerOver":9}"""))
        assertNull(CricketRules.decode("""{"overs":"20"}"""))
        assertNull(CricketRules.decode("""{"superOver":"yes"}"""))
    }

    @Test fun rulesUnknownTypeFallsBackToT20() = assertEquals(t20, CricketRules.decode("""{"eventTypeId":"test"}"""))

    @Test fun rulesCustomKeepsType() {
        val r = CricketRules.decode("""{"eventTypeId":"custom","overs":12,"playersPerSide":7}""")
        assertEquals(CricketEventType.CUSTOM, r?.eventType)
        assertEquals(12, r?.overs)
        assertEquals(7, r?.playersPerSide)
    }

    @Test fun rulesRoundTrip() {
        CricketEventType.entries.forEach { type ->
            val rules = type.defaultRules.copy(superOver = false)
            assertEquals(rules, CricketRules.decode(rules.encoded()))
        }
    }

    // ── Outcomes ──

    @Test fun chaseWonByWickets() = expectValid(
        CricketResult(listOf(inn(a, 158, 7, "20"), inn(b, 159, 4, "18.2"))), t20,
        "won by 6 wickets (10 balls left)", b,
    )

    @Test fun odiDefended() = expectValid(
        CricketResult(listOf(inn(a, 287, 6, "50"), inn(b, 241, 10, "44.3"))), odi,
        "won by 46 runs", a,
    )

    @Test fun chaseOnLastBall() = expectValid(
        CricketResult(listOf(inn(a, 140, 9, "20"), inn(b, 141, 8, "20"))), t20,
        "won by 2 wickets", b,
    )

    @Test fun defendedByRuns() = expectValid(
        CricketResult(listOf(inn(a, 180, 5, "20"), inn(b, 157, 10, "19.1"))), t20,
        "won by 23 runs", a,
    )

    @Test fun defendedOversRanOut() = expectValid(
        CricketResult(listOf(inn(a, 150, 8, "20"), inn(b, 149, 6, "20"))), t20,
        "won by 1 run", a,
    )

    @Test fun tie() = expectValid(
        CricketResult(listOf(inn(a, 150, 8, "20"), inn(b, 150, 6, "20"))), t20,
        "Match tied", null,
    )

    @Test fun tieDecidedBySuperOver() = expectValid(
        CricketResult(listOf(inn(a, 150, 8, "20"), inn(b, 150, 6, "20")), superOverWinnerRegistrationId = a), t20,
        "won the super over", a,
    )

    @Test fun noResult() = expectValid(
        CricketResult(listOf(inn(a, 80, 2, "9.3")), noResult = true), t20,
        "No result", null,
    )

    @Test fun revisedTargetChased() = expectValid(
        CricketResult(listOf(inn(a, 160, 6, "20"), inn(b, 125, 3, "13.4")), revisedTarget = CricketRevisedTarget(124, 15)), t20,
        "won by 7 wickets (8 balls left) (revised target)", b,
    )

    @Test fun revisedTargetDefended() = expectValid(
        CricketResult(listOf(inn(a, 160, 6, "20"), inn(b, 110, 5, "15")), revisedTarget = CricketRevisedTarget(124, 15)), t20,
        "won by 13 runs (revised target)", a,
    )

    @Test fun box6LastManStandsAllOut() = expectValid(
        CricketResult(listOf(inn(a, 70, 6, "5.3"), inn(b, 71, 5, "5.5"))), box6,
        "won by 1 wicket (1 ball left)", b,
    )

    @Test fun singleRunMarginWording() = expectValid(
        CricketResult(listOf(inn(a, 100, 3, "20"), inn(b, 101, 9, "19.5"))), t20,
        "won by 1 wicket (1 ball left)", b,
    )

    // ── Validation ──

    @Test fun oneInningsOnly() = expectInvalid(
        CricketResult(listOf(inn(a, 100, 3, "20"))), t20,
        "Enter both innings, or mark the match as no result.",
    )

    @Test fun sameSideBatsTwice() = expectInvalid(
        CricketResult(listOf(inn(a, 100, 3, "20"), inn(a, 90, 3, "20"))), t20,
        "Each team bats once.",
    )

    @Test fun tooManyWickets() = expectInvalid(
        CricketResult(listOf(inn(a, 100, 11, "20"), inn(b, 90, 3, "20"))), t20,
        "Wickets must be between 0 and 10.",
    )

    @Test fun tooManyOvers() = expectInvalid(
        CricketResult(listOf(inn(a, 100, 3, "21"), inn(b, 90, 3, "20"))), t20,
        "An innings is at most 20 overs.",
    )

    @Test fun firstInningsUnfinished() = expectInvalid(
        CricketResult(listOf(inn(a, 100, 3, "15"), inn(b, 90, 3, "20"))), t20,
        "The first innings isn't finished. It ends when 10 wickets fall or 20 overs are bowled.",
    )

    @Test fun chaseUnfinished() = expectInvalid(
        CricketResult(listOf(inn(a, 150, 3, "20"), inn(b, 90, 3, "12"))), t20,
        "The chase isn't finished. It ends when the target of 151 is reached, 10 wickets fall, or the overs run out.",
    )

    @Test fun chasedAndAllOut() = expectInvalid(
        CricketResult(listOf(inn(a, 150, 3, "20"), inn(b, 151, 10, "19"))), t20,
        "The chasing team reached the target, so it can't also be all out.",
    )

    @Test fun hugeOvershoot() = expectInvalid(
        CricketResult(listOf(inn(a, 150, 3, "20"), inn(b, 175, 3, "19"))), t20,
        "The chase passed the target of 151 by 24. The match ends as soon as the target is reached.",
    )

    @Test fun superOverWithoutTie() = expectInvalid(
        CricketResult(listOf(inn(a, 150, 3, "20"), inn(b, 140, 3, "20")), superOverWinnerRegistrationId = a), t20,
        "A super over is only played when the scores are level.",
    )

    @Test fun outsiderBats() = expectInvalid(
        CricketResult(listOf(inn(a, 150, 3, "20"), inn("regC", 140, 3, "20"))), t20,
        "Each innings must be batted by one of the two teams.",
    )

    @Test fun superOverDisabled() = expectInvalid(
        CricketResult(listOf(inn(a, 150, 8, "20"), inn(b, 150, 6, "20")), superOverWinnerRegistrationId = a),
        t20.copy(superOver = false),
        "This tournament doesn't use super overs.",
    )

    // ── Net run rate ──

    @Test fun nrrWorkedExample() {
        val t = CricketRunRate()
        t.add(CricketResult(listOf(inn(a, 160, 4, "20"), inn(b, 150, 7, "20"))), a, t20)
        t.add(CricketResult(listOf(inn(b, 144, 9, "20"), inn(a, 145, 4, "17.2"))), a, t20)
        assertEquals("+0.820", CricketRunRate.format(t.netRunRate()))
    }

    @Test fun nrrAllOutCountsFullOvers() {
        val u = CricketRunRate()
        u.add(CricketResult(listOf(inn(a, 160, 4, "20"), inn(b, 98, 10, "15.4"))), b, t20)
        assertTrue(abs(u.netRunRate() - (98.0 / 20 - 160.0 / 20)) < 1e-9)
        assertEquals("−3.100", CricketRunRate.format(u.netRunRate()))
    }

    @Test fun nrrExcludesNoResult() {
        val u = CricketRunRate()
        u.add(CricketResult(listOf(inn(a, 160, 4, "20"), inn(b, 98, 10, "15.4"))), b, t20)
        val before = u.runsFor
        u.add(CricketResult(listOf(inn(a, 50, 1, "5")), noResult = true), b, t20)
        assertEquals(before, u.runsFor)
    }

    @Test fun nrrZeroFormatsPositive() = assertEquals("+0.000", CricketRunRate.format(0.0))

    // ── Stored JSON round trip ──

    @Test fun decodeResult() {
        val stored = CricketResult(listOf(inn(a, 158, 7, "20"), inn(b, 159, 4, "18.2"))).encoded()
        assertEquals(110, CricketResult.decode(stored)?.innings?.get(1)?.balls)
    }

    @Test fun decodeServerShape() {
        val json = """{"innings":[{"battingRegistrationId":"A","runs":158,"wickets":7,"balls":120},""" +
            """{"battingRegistrationId":"B","runs":159,"wickets":4,"balls":110}],"revisedTarget":{"runs":150,"overs":18},""" +
            """"superOverWinnerRegistrationId":"A","noResult":true}"""
        val r = CricketResult.decode(json)!!
        assertEquals(2, r.innings.size)
        assertEquals(CricketRevisedTarget(150, 18), r.revisedTarget)
        assertEquals("A", r.superOverWinnerRegistrationId)
        assertTrue(r.isNoResult)
        assertEquals(r, CricketResult.decode(r.encoded()))
    }

    @Test fun decodeRejectsJunk() = assertNull(CricketResult.decode("""{"innings":[{"runs":"x"}]}"""))

    @Test fun encodedKeysSorted() {
        val json = CricketResult(listOf(CricketInnings(a, 1, 2, 3)), CricketRevisedTarget(5, 6), b, true).encoded()
        assertEquals(
            """{"innings":[{"balls":3,"battingRegistrationId":"regA","runs":1,"wickets":2}],"noResult":true,""" +
                """"revisedTarget":{"overs":6,"runs":5},"superOverWinnerRegistrationId":"regB"}""",
            json,
        )
    }

    @Test fun summaryUsesTeamName() {
        val o = CricketScoring.outcome(CricketResult(listOf(inn(a, 158, 7, "20"), inn(b, 159, 4, "18.2"))), t20)
        assertEquals("Royals won by 6 wickets (10 balls left)", o.summary { if (it == b) "Royals" else "Kings" })
        assertEquals("Match tied", CricketOutcome(CricketResultType.TIE, null, "Match tied").summary { it })
    }
}
