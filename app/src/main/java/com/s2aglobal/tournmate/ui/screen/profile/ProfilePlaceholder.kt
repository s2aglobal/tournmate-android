package com.s2aglobal.tournmate.ui.screen.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.google.firebase.auth.FirebaseAuthRecentLoginRequiredException
import com.google.firebase.firestore.FieldValue
import com.s2aglobal.tournmate.data.local.CurrentUserStore
import com.s2aglobal.tournmate.data.repository.CalorieRecordRepository
import com.s2aglobal.tournmate.data.repository.PlayerRepository
import com.s2aglobal.tournmate.data.repository.RatingRepository
import com.s2aglobal.tournmate.domain.model.CalorieActivityType
import com.s2aglobal.tournmate.domain.model.CalorieRecord
import com.s2aglobal.tournmate.domain.model.CalorieSource
import com.s2aglobal.tournmate.domain.model.Player
import com.s2aglobal.tournmate.domain.model.SportType
import com.s2aglobal.tournmate.service.AnalyticsService
import com.s2aglobal.tournmate.service.auth.AuthService
import com.s2aglobal.tournmate.service.calorie.HealthConnectCalorieResult
import com.s2aglobal.tournmate.service.calorie.HealthConnectService
import com.s2aglobal.tournmate.service.calorie.METEstimator
import com.s2aglobal.tournmate.service.calorie.PlayIntensity
import com.s2aglobal.tournmate.ui.theme.CurrentSport
import com.s2aglobal.tournmate.service.notification.NotificationService
import com.s2aglobal.tournmate.service.region.RegionNormalizer
import com.s2aglobal.tournmate.service.sport.PreferredSportUpdater
import com.s2aglobal.tournmate.ui.screen.player.PlayerMatchHistory
import com.s2aglobal.tournmate.ui.screen.player.didPlayerWin
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.Date
import java.util.UUID
import javax.inject.Inject

data class ProfileUiState(
    val isLoading: Boolean = true,
    val isRefreshing: Boolean = false,
    val isGuest: Boolean = false,
    val player: Player? = null,
    val matchesPlayed: Int = 0,
    val wins: Int = 0,
    val winRate: Double = 0.0,
    val sportsmanshipAvg: Double = 0.0,
    val sportsmanshipCount: Int = 0,
    val totalCalories: Double = 0.0,
    val sessionsTracked: Int = 0,
    val avgCaloriesPerSession: Double = 0.0,
    val calorieRecords: List<CalorieRecord> = emptyList(),
    val isDeletingAccount: Boolean = false,
    val deleteErrorMessage: String? = null,
)

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val authService: AuthService,
    private val playerRepo: PlayerRepository,
    private val calorieRepo: CalorieRecordRepository,
    private val ratingRepo: RatingRepository,
    private val matchHistory: PlayerMatchHistory,
    private val currentUserStore: CurrentUserStore,
    private val notificationService: NotificationService,
    private val analytics: AnalyticsService,
    private val sportUpdater: PreferredSportUpdater,
    val healthConnectService: HealthConnectService,
) : ViewModel() {

    private val _uiState = MutableStateFlow(ProfileUiState())
    val uiState: StateFlow<ProfileUiState> = _uiState.asStateFlow()

    fun logScreenView() = analytics.screenView(AnalyticsService.ScreenName.PROFILE_TAB)

    fun load() {
        viewModelScope.launch { loadProfile() }
    }

    fun refresh(onDone: () -> Unit) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isRefreshing = true)
            loadProfile()
            _uiState.value = _uiState.value.copy(isRefreshing = false)
            onDone()
        }
    }

    private suspend fun loadProfile() {
        val firebaseUser = authService.currentUser
        if (firebaseUser == null) {
            _uiState.value = ProfileUiState(isLoading = false, isGuest = true)
            return
        }

        val player = try {
            val playerId = currentUserStore.currentPlayerId()
            if (playerId != null) playerRepo.findPlayerById(playerId) else playerRepo.findPlayerByFirebaseUid(firebaseUser.uid)
        } catch (_: Exception) {
            null
        }

        if (player == null) {
            _uiState.value = _uiState.value.copy(isLoading = false, isGuest = false, player = null)
            return
        }

        val matches = try { matchHistory.finishedMatches(player.id) } catch (_: Exception) { emptyList() }
        val wins = matches.count { it.didPlayerWin(player.id) }
        val ratings = try { ratingRepo.ratingsForPlayer(player.id.toString().uppercase()) } catch (_: Exception) { emptyList() }
        val records = try { calorieRepo.listRecords(player.id) } catch (_: Exception) { emptyList() }
        val total = records.sumOf { it.calories }
        val count = records.size

        _uiState.value = _uiState.value.copy(
            isLoading = false,
            isGuest = false,
            player = player,
            matchesPlayed = matches.size,
            wins = wins,
            winRate = if (matches.isEmpty()) 0.0 else wins.toDouble() / matches.size,
            sportsmanshipCount = ratings.size,
            sportsmanshipAvg = if (ratings.isEmpty()) 0.0 else ratings.map { it.stars }.average(),
            totalCalories = total,
            sessionsTracked = count,
            avgCaloriesPerSession = if (count > 0) total / count else 0.0,
            calorieRecords = records,
        )
    }

    fun updateSport(sport: SportType) {
        val player = _uiState.value.player ?: return
        if (!sportUpdater.setPreferredSport(sport, player)) return
        _uiState.value = _uiState.value.copy(player = player.copy(preferredSport = sport))
    }

    /** Mirrors iOS `saveHomeRegion`: validate, normalize, then persist. */
    fun updateHomeRegion(countryCode: String, postalCode: String, onResult: (success: Boolean, message: String) -> Unit) {
        val player = _uiState.value.player ?: return
        RegionNormalizer.validateHomePostalForCountry(countryCode, postalCode)?.let {
            onResult(false, it)
            return
        }
        val country = RegionNormalizer.normalizeCountryCode(countryCode) ?: run {
            onResult(false, "Please select a valid country.")
            return
        }
        val trimmed = postalCode.trim()
        val postal = if (trimmed.isEmpty()) null else RegionNormalizer.normalizePostal(trimmed, country)

        viewModelScope.launch {
            try {
                playerRepo.updatePlayerFields(
                    player.id,
                    mapOf(
                        "homeCountryCode" to country,
                        "homePostalCode" to (postal ?: FieldValue.delete()),
                    ),
                )
                val current = _uiState.value.player ?: player
                _uiState.value = _uiState.value.copy(player = current.copy(homeCountryCode = country, homePostalCode = postal))
                onResult(true, "Saved. Pull to refresh on Tournaments to apply filtering.")
            } catch (e: Exception) {
                onResult(false, e.localizedMessage ?: "Failed to save")
            }
        }
    }

    fun updateAvatar(avatarId: String, onResult: (Boolean) -> Unit) {
        val player = _uiState.value.player ?: return
        viewModelScope.launch {
            try {
                playerRepo.updatePlayerFields(player.id, mapOf("avatarId" to avatarId))
                _uiState.value = _uiState.value.copy(player = (_uiState.value.player ?: player).copy(avatarId = avatarId))
                onResult(true)
            } catch (_: Exception) {
                onResult(false)
            }
        }
    }

    /** iOS `AuthGateViewModel.deleteAccount`: auth first, then the player doc, then sign out. */
    fun deleteAccount(onDeleted: () -> Unit) {
        if (_uiState.value.isDeletingAccount) return
        _uiState.value = _uiState.value.copy(isDeletingAccount = true, deleteErrorMessage = null)
        viewModelScope.launch {
            val playerId = currentUserStore.currentPlayerId() ?: _uiState.value.player?.id
            val error = authService.deleteAccount().exceptionOrNull()
            if (error != null) {
                val message = if (error is FirebaseAuthRecentLoginRequiredException) {
                    "Please sign out and sign back in, then try again."
                } else {
                    "Failed to delete account: ${error.localizedMessage}"
                }
                _uiState.value = _uiState.value.copy(isDeletingAccount = false, deleteErrorMessage = message)
                return@launch
            }
            if (playerId != null) {
                try { playerRepo.deletePlayer(playerId) } catch (_: Exception) {}
            }
            analytics.log(AnalyticsService.EventName.ACCOUNT_DELETED)
            analytics.setUserId(null)
            _uiState.value = _uiState.value.copy(isDeletingAccount = false)
            onDeleted()
        }
    }

    /** A recent Health Connect workout, or null when Health Connect is unavailable or not permitted. */
    suspend fun checkHealthConnect(durationMinutes: Int): HealthConnectCalorieResult? = try {
        if (healthConnectService.isAvailable && healthConnectService.hasPermissions()) {
            healthConnectService.queryWorkoutCalories(durationMinutes, CurrentSport.sport)
        } else null
    } catch (_: Exception) {
        null
    }

    fun isQuickPlayDuplicate(onResult: (Boolean) -> Unit) {
        val player = _uiState.value.player ?: return
        viewModelScope.launch {
            val existing = try { calorieRepo.listRecords(player.id) } catch (_: Exception) { emptyList() }
            onResult(existing.any { it.id == quickPlayRecordId(player) })
        }
    }

    private fun quickPlayRecordId(player: Player) =
        "quickplay_${player.id.toString().uppercase()}_${(Date().time / 3600000)}"

    fun saveQuickPlay(
        durationMinutes: Int,
        weightKg: Double,
        healthResult: HealthConnectCalorieResult?,
        onResult: (success: Boolean, message: String) -> Unit,
    ) {
        val player = _uiState.value.player ?: return
        viewModelScope.launch {
            if (player.weightKg != weightKg) {
                try {
                    playerRepo.updatePlayerFields(player.id, mapOf("weightKg" to weightKg))
                } catch (_: Exception) {}
            }

            val fromHealth = healthResult != null && healthResult.fromHealthConnect && healthResult.calories > 0
            val calories = if (fromHealth) healthResult!!.calories else METEstimator.estimate(durationMinutes, weightKg, PlayIntensity.CASUAL, CurrentSport.sport)
            val record = CalorieRecord(
                id = quickPlayRecordId(player),
                sessionId = UUID.randomUUID(),
                playerId = player.id,
                calories = calories,
                source = if (fromHealth) CalorieSource.HEALTH_CONNECT else CalorieSource.ESTIMATED,
                weightUsedKg = weightKg,
                durationMinutes = durationMinutes,
                date = Date(),
                sessionTitle = "Quick Play",
                activityType = CalorieActivityType.QUICK_PLAY,
            )

            try {
                calorieRepo.save(record)
                onResult(true, "Logged ${Math.round(calories)} kcal!")
                loadProfile()
            } catch (e: Exception) {
                onResult(false, "Failed: ${e.localizedMessage}")
            }
        }
    }
}
