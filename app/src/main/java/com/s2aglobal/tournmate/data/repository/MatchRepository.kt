package com.s2aglobal.tournmate.data.repository

import com.s2aglobal.tournmate.domain.model.*
import java.util.UUID

interface MatchRepository {
    suspend fun createMatch(
        tournament: Tournament, teamA: Registration, teamB: Registration,
        round: Int?, bracketPosition: Int? = null,
    )
    suspend fun createMatches(
        tournament: Tournament,
        schedule: List<Triple<Registration, Registration, Int>>,
    )
    suspend fun createGroupMatches(
        tournament: Tournament,
        schedule: List<GroupMatchEntry>,
    )
    suspend fun listMatches(tournament: Tournament): List<Match>
    suspend fun matchesForTournament(tournamentId: String): List<Match>
    suspend fun finalizeMatch(match: Match, scoreA: Int, scoreB: Int)
    suspend fun submitSetScores(match: Match, setScores: List<SetScore>, submittedBy: String)
    suspend fun confirmScore(match: Match, confirmedBy: String)
    suspend fun disputeScore(match: Match, disputedBy: String)
    suspend fun resolveDispute(match: Match, setScores: List<SetScore>)
    suspend fun deleteMatches(tournament: Tournament)
}

data class GroupMatchEntry(
    val teamA: Registration,
    val teamB: Registration,
    val round: Int,
    val group: String,
)
