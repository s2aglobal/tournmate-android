package com.s2aglobal.tournmate.domain.model

import java.util.Date
import java.util.UUID

data class SetScore(
    val teamAPoints: Int = 0,
    val teamBPoints: Int = 0,
) {
    val teamAWon: Boolean get() = teamAPoints > teamBPoints
    val teamBWon: Boolean get() = teamBPoints > teamAPoints
}

data class Match(
    val id: UUID = UUID.randomUUID(),
    val tournamentId: String = "",
    val teamAId: String = "",
    val teamBId: String = "",
    val winnerRegistrationId: String? = null,
    val scoreA: Int? = null,
    val scoreB: Int? = null,
    val setScores: List<SetScore> = emptyList(),
    val statusRaw: String = MatchStatus.SCHEDULED.rawValue,
    val createdAt: Date = Date(),
    val round: Int? = null,
    val bracketPosition: Int? = null,
    val groupLabel: String? = null,
    val sportType: SportType = SportType.BADMINTON,
    /** Original id when [sportType] is the GENERIC stand-in for a sport this build doesn't know; written back on save. */
    val sportTypeRaw: String? = null,
    val submittedBy: String? = null,
    val confirmedBy: String? = null,
    val tournament: Tournament = Tournament(),
    val teamA: Registration = Registration(),
    val teamB: Registration = Registration(),
) {
    val status: MatchStatus
        get() = MatchStatus.fromRawValue(statusRaw)

    val setsWonByA: Int
        get() = setScores.count { it.teamAWon }

    val setsWonByB: Int
        get() = setScores.count { it.teamBWon }

    val winnerRegistration: Registration?
        get() = when (winnerRegistrationId) {
            teamAId -> teamA
            teamBId -> teamB
            else -> null
        }

    val loserRegistration: Registration?
        get() = when (winnerRegistrationId) {
            teamAId -> teamB
            teamBId -> teamA
            else -> null
        }

    val displayScoreLine: String
        get() {
            if (setScores.isNotEmpty()) {
                return setScores.joinToString("  ") { "${it.teamAPoints}-${it.teamBPoints}" }
            }
            val a = scoreA ?: return "vs"
            val b = scoreB ?: return "vs"
            return "$a - $b"
        }

    fun isOnTeamA(firebaseUid: String): Boolean =
        teamA.player.firebaseUid == firebaseUid ||
            teamA.partner?.firebaseUid == firebaseUid

    fun isOnTeamB(firebaseUid: String): Boolean =
        teamB.player.firebaseUid == firebaseUid ||
            teamB.partner?.firebaseUid == firebaseUid

    fun isParticipant(firebaseUid: String): Boolean =
        isOnTeamA(firebaseUid) || isOnTeamB(firebaseUid)

    fun containsPlayer(playerId: String): Boolean =
        teamA.contains(playerId) || teamB.contains(playerId)

    companion object {
        /**
         * Winning registration id for a score (games won, or a simple score);
         * `null` for a draw. Mirrors iOS `Match.winnerId`.
         */
        fun winnerId(scoreA: Int, scoreB: Int, teamAId: String, teamBId: String): String? = when {
            scoreA > scoreB -> teamAId
            scoreB > scoreA -> teamBId
            else -> null
        }

        fun bracketRoundName(roundNumber: Int, totalRounds: Int): String =
            // Mirrors iOS `Match.bracketRoundName`.
            when (val fromFinal = totalRounds - roundNumber) {
                0 -> "Finals"
                1 -> "Semi Finals"
                2 -> "Quarter Finals"
                // Outside a real bracket (round-robin rounds, unknown total) → "Round N".
                in 3..15 -> "Round of ${1 shl (fromFinal + 1)}"
                else -> "Round $roundNumber"
            }
    }
}
