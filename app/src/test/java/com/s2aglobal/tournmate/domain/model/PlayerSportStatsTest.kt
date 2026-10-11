package com.s2aglobal.tournmate.domain.model

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Date
import java.util.UUID

/** Mirrors iOS Tools/DrawTests "Per-sport profile stats". */
class PlayerSportStatsTest {

    private fun id(k: Int): UUID = UUID.fromString(String.format("00000000-0000-0000-0000-%012d", k))

    private val me = Player(id = id(1001), name = "Me")
    private val badmintonT = Tournament(sportType = SportType.BADMINTON)
    private val cricketT = Tournament(sportType = SportType.CRICKET)

    private fun regs(t: Tournament, base: Int, first: Player): List<Registration> {
        val other = Player(id = id(base + 50), name = "Other $base")
        return listOf(first, other).mapIndexed { i, p ->
            Registration(
                id = id(base + i), tournament = t, player = p, playerId = p.id.toString().uppercase(),
                rosterIds = listOf(p.id.toString().uppercase()), roster = listOf(p),
            )
        }
    }

    // Badminton: me vs other. Cricket: my team (with me on the roster) vs other.
    private val b = regs(badmintonT, 10, me)
    private val c = regs(cricketT, 20, Player(id = id(2001), name = "Captain")).let { (mine, theirs) ->
        listOf(
            mine.copy(rosterIds = listOf(me.id.toString().uppercase()) + mine.rosterIds, roster = listOf(me) + mine.roster),
            theirs,
        )
    }

    private var t0 = 1_000_000_000L
    private fun m(r: List<Registration>, t: Tournament, winner: Int?, finished: Boolean = true): Match {
        t0 += 60_000
        val a = r[0].id.toString().uppercase()
        val bId = r[1].id.toString().uppercase()
        return Match(
            tournamentId = t.id.toString(), teamAId = a, teamBId = bId,
            winnerRegistrationId = winner?.let { listOf(a, bId)[it] },
            statusRaw = (if (finished) MatchStatus.FINISHED else MatchStatus.SCORE_SUBMITTED).rawValue,
            createdAt = Date(t0), round = 1, sportType = t.sportType,
            tournament = t, teamA = r[0], teamB = r[1],
        )
    }

    // Oldest → newest: badminton W W L, cricket W, tie, W, pending.
    private val all = listOf(
        m(b, badmintonT, 0), m(b, badmintonT, 0), m(b, badmintonT, 1),
        m(c, cricketT, 0), m(c, cricketT, null), m(c, cricketT, 0), m(c, cricketT, 1, finished = false),
    )

    @Test fun badmintonOnly() {
        val s = PlayerSportStats(all, me, SportType.BADMINTON)
        assertEquals(3, s.matches.size)
        assertEquals(2, s.wins)
        assertEquals(-1, s.streak)
    }

    @Test fun cricketOnlyTieSkippedPendingIgnored() {
        val s = PlayerSportStats(all, me, SportType.CRICKET)
        assertEquals(3, s.matches.size)
        assertEquals(2, s.wins)
        assertEquals(2, s.streak)
    }

    @Test fun newestFirst() {
        assertEquals(all[5].createdAt, PlayerSportStats(all, me, SportType.CRICKET).matches.first().createdAt)
    }

    @Test fun otherSportEmpty() {
        assertTrue(PlayerSportStats(all, me, SportType.TENNIS).matches.isEmpty())
    }
}
