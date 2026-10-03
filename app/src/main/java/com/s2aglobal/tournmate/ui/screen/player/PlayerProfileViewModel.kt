package com.s2aglobal.tournmate.ui.screen.player

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.s2aglobal.tournmate.data.local.CurrentUserStore
import com.s2aglobal.tournmate.data.repository.PlayerRepository
import com.s2aglobal.tournmate.data.repository.RatingRepository
import com.s2aglobal.tournmate.domain.model.Match
import com.s2aglobal.tournmate.domain.model.Player
import com.s2aglobal.tournmate.domain.model.PlayerRating
import com.s2aglobal.tournmate.domain.model.SportType
import com.s2aglobal.tournmate.service.AnalyticsService
import com.s2aglobal.tournmate.service.auth.AuthService
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
    val recentMatches: List<Match> = emptyList(),
    val currentPlayerId: String? = null,
    val hasCurrentPlayer: Boolean = false,
    val isGuest: Boolean = false,
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
    private val matchHistory: PlayerMatchHistory,
    private val currentUserStore: CurrentUserStore,
    private val authService: AuthService,
    analytics: AnalyticsService,
) : ViewModel() {

    private val playerId: String = savedStateHandle["playerId"] ?: ""

    private val _uiState = MutableStateFlow(PlayerProfileUiState())
    val uiState: StateFlow<PlayerProfileUiState> = _uiState.asStateFlow()

    init {
        analytics.screenView(AnalyticsService.ScreenName.PLAYER_PROFILE)
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
                val ratings = try { ratingRepo.ratingsForPlayer(playerId) } catch (_: Exception) { emptyList() }
                val avgRating = if (ratings.isNotEmpty()) ratings.map { it.stars }.average() else 0.0
                val isGuest = authService.currentUser == null
                val currentUuid = if (isGuest) null else currentUserStore.currentPlayerId()
                val currentPid = currentUuid?.toString()?.uppercase()
                val hasCurrentPlayer = currentUuid != null &&
                    (try { playerRepo.findPlayerById(currentUuid) } catch (_: Exception) { null }) != null
                val hasRated = if (currentPid != null) {
                    try { ratingRepo.hasRated(raterId = currentPid, playerId = playerId) } catch (_: Exception) { false }
                } else false

                val matches = player?.let {
                    try { matchHistory.finishedMatches(it.id) } catch (_: Exception) { emptyList() }
                }.orEmpty()
                val wins = player?.let { p -> matches.count { it.didPlayerWin(p.id) } } ?: 0

                _uiState.value = _uiState.value.copy(
                    player = player,
                    ratings = ratings,
                    averageRating = avgRating,
                    matchStats = MatchStats(played = matches.size, wins = wins),
                    recentMatches = matches,
                    currentPlayerId = currentPid,
                    hasCurrentPlayer = hasCurrentPlayer,
                    isGuest = isGuest,
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
}
