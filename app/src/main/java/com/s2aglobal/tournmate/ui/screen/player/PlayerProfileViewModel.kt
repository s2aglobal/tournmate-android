package com.s2aglobal.tournmate.ui.screen.player

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.s2aglobal.tournmate.data.local.CurrentUserStore
import com.s2aglobal.tournmate.data.repository.MatchRepository
import com.s2aglobal.tournmate.data.repository.PlayerRepository
import com.s2aglobal.tournmate.data.repository.RatingRepository
import com.s2aglobal.tournmate.data.repository.RegistrationRepository
import com.s2aglobal.tournmate.domain.model.MatchStatus
import com.s2aglobal.tournmate.domain.model.Player
import com.s2aglobal.tournmate.domain.model.PlayerRating
import com.s2aglobal.tournmate.domain.model.SportType
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.Date
import java.util.UUID
import javax.inject.Inject

data class PlayerProfileUiState(
    val player: Player? = null,
    val ratings: List<PlayerRating> = emptyList(),
    val averageRating: Double = 0.0,
    val matchStats: MatchStats = MatchStats(),
    val currentPlayerId: String? = null,
    val hasRated: Boolean = false,
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val showRateSheet: Boolean = false,
    val ratingSubmitted: Boolean = false,
    val viewerSport: SportType = SportType.BADMINTON,
)

@HiltViewModel
class PlayerProfileViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val playerRepo: PlayerRepository,
    private val ratingRepo: RatingRepository,
    private val matchRepo: MatchRepository,
    private val registrationRepo: RegistrationRepository,
    private val currentUserStore: CurrentUserStore,
) : ViewModel() {

    private val playerId: String = savedStateHandle["playerId"] ?: ""

    private val _uiState = MutableStateFlow(PlayerProfileUiState())
    val uiState: StateFlow<PlayerProfileUiState> = _uiState.asStateFlow()

    init {
        load()
        viewModelScope.launch {
            currentUserStore.preferredSportFlow.collect { sport ->
                _uiState.value = _uiState.value.copy(viewerSport = sport)
            }
        }
    }

    fun load() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            try {
                val player = playerRepo.findPlayerById(playerId)
                val ratings = ratingRepo.ratingsForPlayer(playerId)
                val avgRating = if (ratings.isNotEmpty()) ratings.map { it.stars }.average() else 0.0
                val currentPid = currentUserStore.currentPlayerId()?.toString()?.uppercase()
                val hasRated = if (currentPid != null) {
                    ratingRepo.hasRated(raterId = currentPid, playerId = playerId)
                } else false

                val matchStats = computeMatchStats(playerId)

                _uiState.value = _uiState.value.copy(
                    player = player,
                    ratings = ratings,
                    averageRating = avgRating,
                    matchStats = matchStats,
                    currentPlayerId = currentPid,
                    hasRated = hasRated,
                    isLoading = false,
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = e.message,
                )
            }
        }
    }

    fun showRateSheet() {
        _uiState.value = _uiState.value.copy(showRateSheet = true)
    }

    fun hideRateSheet() {
        _uiState.value = _uiState.value.copy(showRateSheet = false)
    }

    fun submitRating(stars: Int, comment: String?) {
        viewModelScope.launch {
            val currentPlayerId = _uiState.value.currentPlayerId ?: return@launch
            try {
                val rating = PlayerRating(
                    id = UUID.randomUUID(),
                    playerId = playerId,
                    raterId = currentPlayerId,
                    stars = stars,
                    comment = comment?.ifBlank { null },
                    createdAt = Date(),
                )
                ratingRepo.addRating(rating)
                _uiState.value = _uiState.value.copy(
                    showRateSheet = false,
                    ratingSubmitted = true,
                    hasRated = true,
                )
                load()
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(errorMessage = e.message)
            }
        }
    }

    private suspend fun computeMatchStats(playerId: String): MatchStats {
        try {
            val playerUUID = try { UUID.fromString(playerId) } catch (_: Exception) { return MatchStats() }
            val registeredTournamentIds = registrationRepo.tournamentIds(playerUUID)
            if (registeredTournamentIds.isEmpty()) return MatchStats()

            var totalPlayed = 0
            var totalWins = 0

            for (tournamentId in registeredTournamentIds.take(10)) {
                val matches = matchRepo.matchesForTournament(tournamentId.toString().uppercase())

                for (match in matches) {
                    if (match.status != MatchStatus.FINISHED) continue
                    val isTeamA = match.teamAId == playerId
                    val isTeamB = match.teamBId == playerId
                    if (!isTeamA && !isTeamB) continue
                    totalPlayed++
                    if (match.winnerRegistrationId == playerId) totalWins++
                }
            }

            return MatchStats(played = totalPlayed, wins = totalWins)
        } catch (_: Exception) {
            return MatchStats()
        }
    }
}
