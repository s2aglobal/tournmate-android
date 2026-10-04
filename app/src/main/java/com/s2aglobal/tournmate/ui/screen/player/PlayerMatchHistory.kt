package com.s2aglobal.tournmate.ui.screen.player

import com.s2aglobal.tournmate.data.repository.MatchRepository
import com.s2aglobal.tournmate.data.repository.RegistrationRepository
import com.s2aglobal.tournmate.data.repository.TournamentRepository
import com.s2aglobal.tournmate.domain.model.Match
import com.s2aglobal.tournmate.domain.model.MatchStatus
import com.s2aglobal.tournmate.domain.model.MatchStatus.*
import com.s2aglobal.tournmate.domain.model.Registration
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import java.util.UUID
import javax.inject.Inject

/** Finished matches a player took part in, newest first (iOS `listFinishedMatches(for:)`). */
class PlayerMatchHistory @Inject constructor(
    private val registrationRepo: RegistrationRepository,
    private val tournamentRepo: TournamentRepository,
    private val matchRepo: MatchRepository,
) {
    suspend fun finishedMatches(playerId: UUID): List<Match> = coroutineScope {
        registrationRepo.tournamentIds(playerId)
            .map { tid ->
                async {
                    runCatching {
                        tournamentRepo.findTournament(tid)?.let { matchRepo.listMatches(it) }.orEmpty()
                    }.getOrDefault(emptyList())
                }
            }
            .awaitAll()
            .flatten()
            .filter { it.status == MatchStatus.FINISHED && (it.teamA.containsPlayerID(playerId) || it.teamB.containsPlayerID(playerId)) }
            .sortedByDescending { it.createdAt }
    }
}

fun Match.didPlayerWin(playerId: UUID): Boolean =
    status == MatchStatus.FINISHED && winnerRegistration?.containsPlayerID(playerId) == true

private fun Registration.teamDisplayName(): String =
    listOfNotNull(player.name, partner?.name).joinToString(" & ")

/** "John & Mike vs Sam & Alex" (iOS `Match.displayTitle`). */
val Match.historyTitle: String
    get() = "${teamA.teamDisplayName()} vs ${teamB.teamDisplayName()}"

/** "21-18, 15-21, 21-19 (2-1)" or "21 - 18" (iOS `Match.displayScoreLine`). */
val Match.historyScoreLine: String
    get() {
        if (setScores.isNotEmpty()) {
            return setScores.joinToString(", ") { "${it.teamAPoints}-${it.teamBPoints}" } + " ($setsWonByA-$setsWonByB)"
        }
        val a = scoreA
        val b = scoreB
        if (a == null || b == null) {
            return when (status) {
                SCORE_SUBMITTED -> "Awaiting Confirmation"
                DISPUTED -> "Score Disputed ⚠️"
                FINISHED -> "Finished"
                SCHEDULED -> "Scheduled"
            }
        }
        return "$a - $b"
    }
