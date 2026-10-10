package com.s2aglobal.tournmate.ui.screen.tournament

import com.s2aglobal.tournmate.domain.model.CricketInnings
import com.s2aglobal.tournmate.domain.model.CricketResult
import com.s2aglobal.tournmate.domain.model.CricketRevisedTarget
import com.s2aglobal.tournmate.domain.model.CricketRules
import com.s2aglobal.tournmate.domain.model.CricketScoring
import com.s2aglobal.tournmate.domain.model.Match
import com.s2aglobal.tournmate.domain.model.MatchFormat
import com.s2aglobal.tournmate.domain.model.SportType
import com.s2aglobal.tournmate.domain.model.Tournament
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** Knockout cricket rules (super over forced, ties/no results need a winner). Same cases as iOS `CricketKnockout`. */
class KnockoutCricketTest {
    private val a = "A"
    private val b = "B"
    private val noSuperOver = CricketRules.DEFAULT.copy(superOver = false)
    private fun tournament(f: MatchFormat) = Tournament(sportType = SportType.CRICKET, matchFormatRaw = f.rawValue).withCricketRules(noSuperOver)
    private fun match(t: Tournament, group: String? = null) = Match(tournament = t, teamAId = a, teamBId = b, groupLabel = group)
    private val tie = CricketResult(listOf(CricketInnings(a, 150, 6, 120), CricketInnings(b, 150, 10, 120)))
    private val noResult = CricketResult(emptyList(), noResult = true)
    private val tieMsg = "This is a knockout match, so a tie needs a winner. Enter who won the super over."
    private val nrMsg = "A knockout match needs a winner. Replay it, or decide it with a super over and enter that result."

    @Test fun requiresWinner() {
        assertTrue(cricketRequiresWinner(tournament(MatchFormat.SINGLE_ELIMINATION), match(tournament(MatchFormat.SINGLE_ELIMINATION))))
        assertTrue(cricketRequiresWinner(tournament(MatchFormat.DOUBLE_ELIMINATION), match(tournament(MatchFormat.DOUBLE_ELIMINATION))))
        val gk = tournament(MatchFormat.GROUP_KNOCKOUT)
        assertTrue(cricketRequiresWinner(gk, match(gk)))
        assertTrue(!cricketRequiresWinner(gk, match(gk, "A")))
        for (f in listOf(MatchFormat.ROUND_ROBIN, MatchFormat.SWISS, MatchFormat.MANUAL_DRAW)) {
            assertTrue(!cricketRequiresWinner(tournament(f), match(tournament(f))))
        }
    }

    @Test fun knockoutForcesSuperOverAndBlocksTiesAndNoResults() {
        val ko = tournament(MatchFormat.SINGLE_ELIMINATION)
        val rules = cricketRulesFor(ko, match(ko))
        assertTrue(rules.superOver)
        assertEquals(tieMsg, knockoutIssue(ko, match(ko), tie, rules))
        assertEquals(nrMsg, knockoutIssue(ko, match(ko), noResult, rules))
        val decided = tie.copy(superOverWinnerRegistrationId = b)
        assertTrue(CricketScoring.validate(decided, rules, a, b).isEmpty())
        assertNull(knockoutIssue(ko, match(ko), decided, rules))
        // Rain-revised tie in a knockout: blocked until the super over is entered, then fine.
        val revisedTie = CricketResult(listOf(CricketInnings(a, 160, 6, 120), CricketInnings(b, 123, 5, 90)), CricketRevisedTarget(124, 15))
        assertEquals(tieMsg, knockoutIssue(ko, match(ko), revisedTie, rules))
        val revisedDecided = revisedTie.copy(superOverWinnerRegistrationId = a)
        assertTrue(CricketScoring.validate(revisedDecided, rules, a, b).isEmpty())
        assertNull(knockoutIssue(ko, match(ko), revisedDecided, rules))
    }

    @Test fun leaguesAllowTiesAndNoResults() {
        val gk = tournament(MatchFormat.GROUP_KNOCKOUT)
        assertTrue(!cricketRulesFor(gk, match(gk, "A")).superOver)
        assertNull(knockoutIssue(gk, match(gk, "B"), noResult, noSuperOver))
        assertEquals(nrMsg, knockoutIssue(gk, match(gk), noResult, cricketRulesFor(gk, match(gk))))
        for (f in listOf(MatchFormat.ROUND_ROBIN, MatchFormat.SWISS, MatchFormat.MANUAL_DRAW)) {
            val t = tournament(f)
            assertNull(knockoutIssue(t, match(t), tie, noSuperOver))
            assertNull(knockoutIssue(t, match(t), noResult, noSuperOver))
        }
        val decided = tie.copy(superOverWinnerRegistrationId = b)
        assertEquals("This tournament doesn't use super overs.", CricketScoring.validate(decided, noSuperOver, a, b).first().message)
    }
}
