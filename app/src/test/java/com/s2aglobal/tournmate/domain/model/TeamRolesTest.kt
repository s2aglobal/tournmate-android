package com.s2aglobal.tournmate.domain.model

import com.s2aglobal.tournmate.service.pairing.makeTeams
import com.s2aglobal.tournmate.service.pairing.makeTournament
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/** Team roles and Player of the Match. Same checks as iOS `Tools/DrawTests/main.swift`. */
class TeamRolesTest {
    private val t = makeTournament(MatchFormat.ROUND_ROBIN)
    private val teams = makeTeams(2, t, rosterSize = 3)
    private val reg = teams[0].copy(guestNames = listOf("Ravi Kumar"))
    private val keys = reg.rosterMembers.map { it.key }
    private val p0 = keys[0]
    private val p1 = keys[1]
    private val p2 = keys[2]
    private val guest = keys[3]

    @Test fun membersAndKeys() {
        assertTrue("roles: members = roster then guests", keys.size == 4 && guest == "guest:Ravi Kumar" && reg.rosterMembers[3].isGuest)
        assertEquals("roles: player keys upper-case", reg.rosterIds[0].uppercase(), p0)
    }

    @Test fun captainDefaults() {
        var r = TeamRoles().normalized(keys, preferredCaptain = p1.lowercase())
        assertEquals("roles: preferred captain (any case)", p1, r.captain)
        r = TeamRoles().normalized(keys, preferredCaptain = "NOT-ON-ROSTER")
        assertEquals("roles: first member captain when registrant not playing", p0, r.captain)
        assertEquals("roles: no one playing → no roles", TeamRoles(), TeamRoles(captain = p0).normalized(emptyList(), preferredCaptain = p0))
    }

    /** The iOS sequence of role edits, shared by the rules and removal checks. */
    private fun edited(): TeamRoles {
        var r = TeamRoles().normalized(keys, preferredCaptain = "NOT-ON-ROSTER")
        r = r.setCaptain(guest)
        r = r.toggleViceCaptain(guest)
        r = r.toggleViceCaptain(p2)
        r = r.setCaptain(p2)
        r = r.toggleKeeper(p2)
        r = r.toggleKeeper(p0)
        r = r.toggleKeeper(p0)
        r = r.toggleViceCaptain(p1)
        r = r.setPlayingRole(PlayingRole.ALL_ROUNDER, p1.lowercase())
        r = r.setPlayingRole(PlayingRole.BOWLER, guest)
        r = r.setPlayingRole(null, guest)
        return r
    }

    @Test fun captainViceCaptainKeeperRules() {
        var r = TeamRoles().normalized(keys, preferredCaptain = "NOT-ON-ROSTER")
        r = r.setCaptain(guest)
        assertTrue("roles: guest can captain", r.isCaptain(guest) && r.badges(guest) == listOf("C"))
        r = r.toggleViceCaptain(guest)
        assertNull("roles: captain can't be vice-captain", r.viceCaptain)
        r = r.toggleViceCaptain(p2)
        r = r.setCaptain(p2)
        assertTrue("roles: vice-captain made captain drops VC", r.captain == p2 && r.viceCaptain == null)
        r = r.toggleKeeper(p2)
        assertEquals("roles: captain may keep wicket", listOf("C", "WK"), r.badges(p2))
        r = r.toggleKeeper(p0)
        assertTrue("roles: one keeper", r.keeper == p0 && !r.isKeeper(p2))
        r = r.toggleKeeper(p0)
        assertNull("roles: keeper toggles off", r.keeper)
        r = r.toggleViceCaptain(p1)
        r = r.setPlayingRole(PlayingRole.ALL_ROUNDER, p1.lowercase())
        r = r.setPlayingRole(PlayingRole.BOWLER, guest)
        assertTrue(
            "roles: playing role by any-case key",
            r.playingRole(p1) == PlayingRole.ALL_ROUNDER && r.playingRole(guest) == PlayingRole.BOWLER,
        )
        r = r.setPlayingRole(null, guest)
        assertNull("roles: playing role cleared", r.playingRole(guest))
        assertEquals(edited(), r)
    }

    @Test fun removingPlayersDropsRoles() {
        val r = edited().setPlayingRole(PlayingRole.BATTER, p2)
        val dropped = r.normalized(listOf(p0, p1), preferredCaptain = p0)
        assertEquals("roles: removed captain → preferred", p0, dropped.captain)
        assertTrue("roles: removed player loses role", dropped.playingRole(p2) == null && dropped.viceCaptain == p1)
        val dropVC = r.normalized(listOf(p2, p0), preferredCaptain = null)
        assertTrue("roles: removed VC cleared", dropVC.viceCaptain == null && dropVC.captain == p2)
        assertEquals("roles: normalize idempotent", dropped, dropped.normalized(listOf(p0, p1), preferredCaptain = p1))
    }

    @Test fun firestoreRoundTrip() {
        var full = TeamRoles(
            captain = p0, viceCaptain = p1, keeper = guest,
            playing = mapOf(p2 to PlayingRole.WICKETKEEPER_BATTER, guest to PlayingRole.BOWLER),
        )
        val stored = full.firestoreFields.toMutableMap<String, Any?>()
        stored["captainId"] = p0.lowercase()
        stored["playerRoles"] = mapOf(p2.lowercase() to "wicketkeeperBatter", guest to "bowler", "X" to "unknownRole")
        assertEquals("roles: firestore round trip", full, TeamRoles.fromFirestore(stored))
        assertTrue("roles: empty roles write nothing", TeamRoles().firestoreFields.isEmpty())
        full = full.copy(playing = emptyMap())
        assertFalse("roles: no playerRoles when none", full.firestoreFields.containsKey("playerRoles"))
    }

    @Test fun captainName() {
        assertEquals("roles: captainName defaults to registrant", reg.player.name, reg.captainName)
        assertEquals("roles: captainName guest", "Ravi Kumar", reg.copy(roles = TeamRoles(captain = guest)).captainName)
        assertEquals(
            "roles: captainName falls back when captain left",
            reg.player.name, reg.copy(roles = TeamRoles(captain = "guest:Left The Side")).captainName,
        )
    }

    private fun pom(a: Registration, b: Registration, key: String?, name: String, team: Registration, finished: Boolean = true): Match {
        val m = Match(
            tournament = t, teamA = a, teamB = b, round = 1,
            teamAId = a.id.toString().uppercase(), teamBId = b.id.toString().uppercase(),
            statusRaw = (if (finished) MatchStatus.FINISHED else MatchStatus.SCORE_SUBMITTED).rawValue,
        )
        return if (key == null) m else m.withPlayerOfMatch(PlayerOfMatch(key, name, team.id.toString().uppercase()))
    }

    @Test fun playerOfTheTournamentTally() {
        val x = reg
        val y = teams[1]
        val yKey = y.rosterMembers[0].key
        val ms = listOf(
            pom(x, y, p0, "P0-0", team = x), pom(x, y, p0.lowercase(), "P0-0", team = x),
            pom(x, y, yKey, "P1-0", team = y), pom(x, y, yKey, "P1-0", team = y, finished = false),
            pom(x, y, null, "", team = x), pom(x, y, guest, "Ravi Kumar", team = x),
        )
        val tally = PlayerAwardTally.tally(ms)
        assertTrue("pom: leader", tally.first().key == p0 && tally.first().awards == 2)
        assertTrue(
            "pom: pending result ignored, ties by name",
            tally.map { it.awards } == listOf(2, 1, 1) && tally[1].name == "P1-0" && tally[2].name == "Ravi Kumar",
        )
        assertTrue("pom: no awards → empty", PlayerAwardTally.tally(listOf(pom(x, y, null, "", team = x))).isEmpty())
        val m = ms[0].withPlayerOfMatch(null)
        assertTrue("pom: cleared", m.playerOfMatchKey == null && m.playerOfMatchName == null && m.playerOfMatchTeamId == null)
    }

    @Test fun playingRoleRawValuesAndTitles() {
        assertEquals(listOf("batter", "bowler", "allRounder", "wicketkeeperBatter"), PlayingRole.entries.map { it.rawValue })
        assertEquals(listOf("Batter", "Bowler", "All-rounder", "Wicketkeeper-batter"), PlayingRole.entries.map { it.title })
        assertEquals("guest:Ravi Kumar", TeamRoles.key("guest:Ravi Kumar"))
    }
}
