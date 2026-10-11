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
    /** Team-sport result (cricket): "win" | "tie" | "noResult". Null for set-based sports. */
    val resultType: String? = null,
    /** Scorecard JSON ([CricketResult]). */
    val resultData: String? = null,
    /** "Dallas Royals won by 6 wickets (10 balls left)". */
    val summary: String? = null,
    /** Player of the Match (cricket, optional): [TeamRoles] key, name, and the registration id of their team. */
    val playerOfMatchKey: String? = null,
    val playerOfMatchName: String? = null,
    val playerOfMatchTeamId: String? = null,
    /** Optional batting/bowling figures per player (JSON [CricketPlayerStats]). */
    val playerStatsData: String? = null,
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

    /** Decoded scorecard, if this match has one. */
    val cricketResult: CricketResult?
        get() = CricketResult.decode(resultData)

    val playerOfMatch: PlayerOfMatch?
        get() {
            val key = playerOfMatchKey ?: return null
            val name = playerOfMatchName ?: return null
            val team = playerOfMatchTeamId ?: return null
            return PlayerOfMatch(key, name, team)
        }

    /** Decoded player figures, if any were entered with the result. */
    val playerStats: CricketPlayerStats?
        get() = CricketPlayerStats.decode(playerStatsData)

    /** This match with [pom] as Player of the Match (`null` clears it). */
    fun withPlayerOfMatch(pom: PlayerOfMatch?): Match =
        copy(playerOfMatchKey = pom?.key, playerOfMatchName = pom?.name, playerOfMatchTeamId = pom?.teamRegistrationId)

    /** `true` when the result is a team-sport scorecard (rated on the server only). */
    val hasTeamResult: Boolean
        get() = resultData != null || resultType != null

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
            cricketScoreLine?.let { return it }
            if (setScores.isNotEmpty()) {
                return setScores.joinToString("  ") { "${it.teamAPoints}-${it.teamBPoints}" }
            }
            val a = scoreA ?: return "vs"
            val b = scoreB ?: return "vs"
            return "$a - $b"
        }

    /** "158/7 (20) · 159/4 (18.2)", or the summary for a no result; null without a scorecard. */
    val cricketScoreLine: String?
        get() {
            val card = cricketResult ?: return null
            val bpo = tournament.cricketRules.ballsPerOver
            if (card.innings.isEmpty()) return summary ?: "No result"
            return card.innings.joinToString(" · ") { "${it.runs}/${it.wickets} (${CricketOvers.format(it.balls, bpo)})" }
        }

    fun isOnTeamA(firebaseUid: String): Boolean = teamA.containsFirebaseUid(firebaseUid)

    fun isOnTeamB(firebaseUid: String): Boolean = teamB.containsFirebaseUid(firebaseUid)

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

        /**
         * The bracket's champion, or `null` until the final has been played: the
         * last round must be the bracket's final (round == [totalRounds]) with
         * exactly one match, finished, with a winner. Mirrors iOS `bracketChampion`.
         */
        fun bracketChampion(lastRound: Int?, lastRoundMatches: List<Match>, totalRounds: Int): Registration? {
            if (totalRounds <= 0 || lastRound != totalRounds || lastRoundMatches.size != 1) return null
            val finalMatch = lastRoundMatches.single()
            if (finalMatch.status != MatchStatus.FINISHED) return null
            return finalMatch.winnerRegistration
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
