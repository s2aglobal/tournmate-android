package com.s2aglobal.tournmate.service.pairing

import com.s2aglobal.tournmate.domain.model.MatchFormat
import com.s2aglobal.tournmate.domain.model.SeedingMode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Seeding modes (Elo Ranked, Random; stored Manual seeds by rating).
 * Mirrors the "Seeding modes" block of iOS `Tools/DrawTests/main.swift`.
 */
class SeedingModesTest {

    private val salt = "00000000-0000-0000-0000-000000000777"

    @Test fun fnv1a64TestVectors() {
        assertEquals(0xcbf29ce484222325uL, PairingService.fnv1a64(""))
        assertEquals(0xaf63dc4c8601ec8cuL, PairingService.fnv1a64("a"))
    }

    @Test fun offeredAndEffective() {
        assertEquals(listOf(SeedingMode.ELO_RANKED, SeedingMode.RANDOM), SeedingMode.offered)
        assertEquals(SeedingMode.ELO_RANKED, SeedingMode.MANUAL.effective)
        assertEquals(SeedingMode.ELO_RANKED, SeedingMode.ELO_RANKED.effective)
        assertEquals(SeedingMode.RANDOM, SeedingMode.RANDOM.effective)
        // MatchFormat.offered is covered per sport in MatchFormatOfferedTest.
    }

    @Test fun seedingModes() {
        val t = makeTournament(MatchFormat.SINGLE_ELIMINATION)
        var anyDifferent = false
        for (n in 0..64) {
            val teams = makeTeams(n, t)
            val elo = PairingService.seededByRating(teams)
            assertEquals("elo mode = by rating $n", elo, PairingService.seeded(teams, SeedingMode.ELO_RANKED, salt))
            assertEquals("manual = by rating $n", elo, PairingService.seeded(teams, SeedingMode.MANUAL, salt))
            val r1 = PairingService.seeded(teams, SeedingMode.RANDOM, salt)
            assertTrue("random is a permutation $n", r1.map { it.id }.toSet() == teams.map { it.id }.toSet() && r1.size == n)
            assertEquals("random is repeatable $n", r1, PairingService.seeded(teams, SeedingMode.RANDOM, salt))
            assertEquals("random ignores input order $n", r1, PairingService.seeded(teams.reversed(), SeedingMode.RANDOM, salt))
            assertEquals("random salt is case-insensitive $n", r1, PairingService.seeded(teams, SeedingMode.RANDOM, salt.lowercase()))
            val other = PairingService.seeded(teams, SeedingMode.RANDOM, "00000000-0000-0000-0000-000000000778")
            if (n >= 4 && other != r1) anyDifferent = true
            if (n >= 2) {
                val a = PairingService.generateBracketFirstRoundSeeded(elo)
                val b = PairingService.generateBracketFirstRoundSeeded(r1)
                assertTrue(
                    "random bracket same shape $n",
                    a.matches.size == b.matches.size && a.byeTeams.size == b.byeTeams.size && a.totalRounds == b.totalRounds,
                )
                assertEquals("random byes are the first of the draw $n", r1.take(b.byeTeams.size), b.byeTeams)
            }
        }
        assertTrue("a different tournament draws differently", anyDifferent)
    }
}
