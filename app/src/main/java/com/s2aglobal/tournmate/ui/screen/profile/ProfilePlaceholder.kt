package com.s2aglobal.tournmate.ui.screen.profile

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.s2aglobal.tournmate.data.local.CurrentUserStore
import com.s2aglobal.tournmate.data.repository.CalorieRecordRepository
import com.s2aglobal.tournmate.data.repository.PlayerRepository
import com.s2aglobal.tournmate.domain.model.CalorieActivityType
import com.s2aglobal.tournmate.domain.model.CalorieRecord
import com.s2aglobal.tournmate.domain.model.CalorieSource
import com.s2aglobal.tournmate.domain.model.Player
import com.s2aglobal.tournmate.domain.model.SportType
import com.s2aglobal.tournmate.service.AnalyticsService
import com.s2aglobal.tournmate.service.auth.AuthService
import com.s2aglobal.tournmate.service.calorie.HealthConnectService
import com.s2aglobal.tournmate.service.notification.NotificationService
import com.s2aglobal.tournmate.service.sport.PreferredSportUpdater
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
    val isGuest: Boolean = false,
    val player: Player? = null,
    val totalCalories: Double = 0.0,
    val sessionsTracked: Int = 0,
    val avgCaloriesPerSession: Double = 0.0,
    val calorieRecords: List<CalorieRecord> = emptyList(),
)

@HiltViewModel
class ProfileViewModel @Inject constructor(
    private val authService: AuthService,
    private val playerRepo: PlayerRepository,
    private val calorieRepo: CalorieRecordRepository,
    private val currentUserStore: CurrentUserStore,
    private val notificationService: NotificationService,
    private val analytics: AnalyticsService,
    private val sportUpdater: PreferredSportUpdater,
    val healthConnectService: HealthConnectService,
) : ViewModel() {

    private val _uiState = MutableStateFlow(ProfileUiState())
    val uiState: StateFlow<ProfileUiState> = _uiState.asStateFlow()

    fun load() {
        viewModelScope.launch {
            val firebaseUser = authService.currentUser
            if (firebaseUser == null) {
                _uiState.value = ProfileUiState(isLoading = false, isGuest = true)
                return@launch
            }

            val playerId = currentUserStore.currentPlayerId()
            val player = if (playerId != null) {
                playerRepo.findPlayerById(playerId)
            } else {
                playerRepo.findPlayerByFirebaseUid(firebaseUser.uid)
            }

            if (player != null) {
                val records = try {
                    calorieRepo.listRecords(player.id)
                } catch (_: Exception) {
                    emptyList()
                }
                val total = records.sumOf { it.calories }
                val count = records.size
                val avg = if (count > 0) total / count else 0.0

                _uiState.value = ProfileUiState(
                    isLoading = false,
                    isGuest = false,
                    player = player,
                    totalCalories = total,
                    sessionsTracked = count,
                    avgCaloriesPerSession = avg,
                    calorieRecords = records,
                )
            } else {
                _uiState.value = ProfileUiState(isLoading = false, isGuest = false, player = null)
            }
        }
    }

    fun updateSport(sport: SportType) {
        val player = _uiState.value.player ?: return
        if (!sportUpdater.setPreferredSport(sport, player)) return
        _uiState.value = _uiState.value.copy(player = player.copy(preferredSport = sport))
    }

    fun updateHomeRegion(countryCode: String, postalCode: String, onResult: (String) -> Unit) {
        val player = _uiState.value.player ?: return
        val trimmedPostal = postalCode.trim()
        val updated = player.copy(
            homeCountryCode = countryCode,
            homePostalCode = trimmedPostal.ifBlank { null },
        )
        _uiState.value = _uiState.value.copy(player = updated)
        viewModelScope.launch {
            try {
                val fields = mutableMapOf<String, Any?>("homeCountryCode" to countryCode)
                if (trimmedPostal.isNotBlank()) fields["homePostalCode"] = trimmedPostal
                playerRepo.updatePlayerFields(player.id, fields)
                onResult("Saved. Pull to refresh on Tournaments to apply filtering.")
            } catch (e: Exception) {
                onResult(e.localizedMessage ?: "Failed to save")
            }
        }
    }

    fun updateAvatar(avatarId: String) {
        val player = _uiState.value.player ?: return
        val updated = player.copy(avatarId = avatarId)
        _uiState.value = _uiState.value.copy(player = updated)
        viewModelScope.launch {
            try {
                playerRepo.updatePlayerFields(player.id, mapOf("avatarId" to avatarId))
            } catch (_: Exception) {}
        }
    }

    fun saveQuickPlay(durationMinutes: Int, weightKg: Double, forceSave: Boolean = false, onResult: (String) -> Unit) {
        val player = _uiState.value.player ?: return

        viewModelScope.launch {
            val recordId = "quickplay_${player.id.toString().uppercase()}_${(Date().time / 3600000)}"

            // Check for duplicate within this hour
            if (!forceSave) {
                val existing = try { calorieRepo.listRecords(player.id) } catch (_: Exception) { emptyList() }
                if (existing.any { it.id == recordId }) {
                    onResult("DUPLICATE")
                    return@launch
                }
            }
            // Try Health Connect first
            val hcResult = try {
                if (healthConnectService.isAvailable && healthConnectService.hasPermissions()) {
                    healthConnectService.queryWorkoutCalories(durationMinutes)
                } else null
            } catch (_: Exception) { null }

            val calories: Double
            val source: CalorieSource
            if (hcResult != null && hcResult.fromHealthConnect && hcResult.calories > 0) {
                calories = hcResult.calories
                source = CalorieSource.HEALTH_CONNECT
            } else {
                val met = 5.5
                val hours = durationMinutes / 60.0
                calories = met * weightKg * hours
                source = CalorieSource.ESTIMATED
            }

            val record = CalorieRecord(
                id = recordId,
                sessionId = UUID.randomUUID(),
                playerId = player.id,
                calories = calories,
                source = source,
                weightUsedKg = weightKg,
                durationMinutes = durationMinutes,
                date = Date(),
                sessionTitle = "Quick Play",
                activityType = CalorieActivityType.QUICK_PLAY,
            )

            try {
                calorieRepo.save(record)
                val sourceLabel = if (source == CalorieSource.HEALTH_CONNECT) "from Health Connect" else "estimated"
                onResult("Logged ${calories.toInt()} cal ($sourceLabel)!")
                load()
            } catch (e: Exception) {
                onResult("Failed: ${e.localizedMessage}")
            }
        }
    }
}
