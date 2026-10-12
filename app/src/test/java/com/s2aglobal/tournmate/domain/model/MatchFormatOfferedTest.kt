package com.s2aglobal.tournmate.domain.model

import com.s2aglobal.tournmate.domain.model.MatchFormat.DOUBLE_ELIMINATION
import com.s2aglobal.tournmate.domain.model.MatchFormat.GROUP_KNOCKOUT
import com.s2aglobal.tournmate.domain.model.MatchFormat.MANUAL_DRAW
import com.s2aglobal.tournmate.domain.model.MatchFormat.ROUND_ROBIN
import com.s2aglobal.tournmate.domain.model.MatchFormat.SINGLE_ELIMINATION
import com.s2aglobal.tournmate.domain.model.MatchFormat.SWISS
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/** Mirrors iOS Tools/DrawTests "Formats offered per sport". */
class MatchFormatOfferedTest {

    @Test fun cricket() {
        assertEquals(
            listOf(SINGLE_ELIMINATION, ROUND_ROBIN, GROUP_KNOCKOUT, MANUAL_DRAW),
            MatchFormat.offered(SportType.CRICKET),
        )
    }

    @Test fun racketSports() {
        assertEquals(
            listOf(SINGLE_ELIMINATION, ROUND_ROBIN, GROUP_KNOCKOUT, SWISS, MANUAL_DRAW),
            MatchFormat.offered(SportType.BADMINTON),
        )
    }

    @Test fun currentKeptCricketSwiss() {
        assertEquals(
            listOf(SINGLE_ELIMINATION, ROUND_ROBIN, GROUP_KNOCKOUT, SWISS, MANUAL_DRAW),
            MatchFormat.offered(SportType.CRICKET, current = SWISS),
        )
    }

    @Test fun currentKeptDoubleElimination() {
        assertTrue(DOUBLE_ELIMINATION in MatchFormat.offered(SportType.TENNIS, current = DOUBLE_ELIMINATION))
    }

    @Test fun noDoubleEliminationForNew() {
        assertTrue(SportType.entries.none { DOUBLE_ELIMINATION in MatchFormat.offered(it) })
    }
}
