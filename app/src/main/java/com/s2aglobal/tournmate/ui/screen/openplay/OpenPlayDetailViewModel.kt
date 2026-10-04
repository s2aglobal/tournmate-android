package com.s2aglobal.tournmate.ui.screen.openplay

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.s2aglobal.tournmate.data.local.CurrentUserStore
import com.s2aglobal.tournmate.data.repository.PlaySessionRepository
import com.s2aglobal.tournmate.data.repository.PlayerRepository
import com.s2aglobal.tournmate.domain.model.AgeGroup
import com.s2aglobal.tournmate.domain.model.CasualGameType
import com.s2aglobal.tournmate.domain.model.PlaySession
import com.s2aglobal.tournmate.domain.model.PlaySessionStatus
import com.s2aglobal.tournmate.domain.model.Player
import com.s2aglobal.tournmate.domain.model.SkillLevel
import com.s2aglobal.tournmate.data.repository.CalorieRecordRepository
import com.s2aglobal.tournmate.domain.model.CalorieActivityType
import com.s2aglobal.tournmate.domain.model.CalorieRecord
import com.s2aglobal.tournmate.domain.model.CalorieSource
import com.s2aglobal.tournmate.service.calorie.HealthConnectService
import com.s2aglobal.tournmate.service.calorie.METEstimator
import com.s2aglobal.tournmate.service.calorie.PlayIntensity
import dagger.hilt.android.lifecycle.HiltViewModel
import java.time.Instant
import kotlin.math.roundToInt
import java.util.Date
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class OpenPlayDetailUiState(
    val session: PlaySession? = null,
    val attendees: List<Player> = emptyList(),
    val currentPlayerId: String? = null,
    val firebaseUid: String? = null,
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val didDelete: Boolean = false,
    val statusMessage: String? = null,
    val currentPlayer: Player? = null,
    // Calorie tracking
    val existingCalorieRecord: CalorieRecord? = null,
    val isLoggingCalories: Boolean = false,
    val calorieWeight: Double = 70.0,
    val isFetchingHealthConnect: Boolean = false,
    /** Calories found in Health Connect for the session window (null = none / not checked). */
    val healthConnectCalories: Double? = null,
) {
    val hasJoined: Boolean
        get() = currentPlayerId != null && session?.attendeeIds?.contains(currentPlayerId) == true

    /** Session has ended and the current player attended (iOS `canLogCalories`). */
    val canLogCalories: Boolean
        get() {
            val s = session ?: return false
            if (!hasJoined) return false
            return s.date.time + (s.durationMinutes ?: 120) * 60_000L < System.currentTimeMillis()
        }
}

@HiltViewModel
class OpenPlayDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val sessionRepo: PlaySessionRepository,
    private val playerRepo: PlayerRepository,
    private val currentUserStore: CurrentUserStore,
    private val calorieRepo: CalorieRecordRepository,
    private val healthConnectService: HealthConnectService,
) : ViewModel() {

    private var healthConnectChecked = false

    private val sessionId: String = savedStateHandle["sessionId"] ?: ""

    private val _uiState = MutableStateFlow(OpenPlayDetailUiState())
    val uiState: StateFlow<OpenPlayDetailUiState> = _uiState.asStateFlow()

    init {
        load()
    }

    fun load() {
        viewModelScope.launch { loadInternal() }
    }

    private suspend fun loadInternal() {
        _uiState.value = _uiState.value.copy(isLoading = true)
        try {
            val session = sessionRepo.find(sessionId)
            val playerUUID = currentUserStore.currentPlayerId()
            val playerId = playerUUID?.toString()?.uppercase()
            val firebaseUid = currentUserStore.firebaseUid()
            val player = playerUUID?.let { runCatching { playerRepo.findPlayerById(it) }.getOrNull() }

            val attendees = if (session != null) {
                fetchAttendees(session.attendeeIds)
            } else emptyList()

            val record = if (session != null && playerUUID != null) {
                runCatching { calorieRepo.findRecord(session.id, playerUUID) }.getOrNull()
            } else null

            _uiState.value = _uiState.value.copy(
                session = session,
                attendees = attendees,
                currentPlayerId = playerId,
                firebaseUid = firebaseUid,
                currentPlayer = player,
                calorieWeight = player?.weightKg ?: _uiState.value.calorieWeight,
                existingCalorieRecord = record,
                isLoading = false,
            )
        } catch (e: Exception) {
            _uiState.value = _uiState.value.copy(
                isLoading = false,
                errorMessage = e.message,
            )
        }

        val state = _uiState.value
        if (state.canLogCalories && state.existingCalorieRecord == null) {
            fetchHealthConnectCalories()
        }
    }

    /** Looks for Health Connect data covering the session window; auto-saves it when found. */
    private suspend fun fetchHealthConnectCalories() {
        if (healthConnectChecked) return
        val session = _uiState.value.session ?: return
        _uiState.value = _uiState.value.copy(isFetchingHealthConnect = true)
        val plannedEnd = session.date.time + (session.durationMinutes ?: 120) * 60_000L
        val end = Instant.ofEpochMilli(maxOf(plannedEnd, System.currentTimeMillis()))
        val calories = healthConnectService.queryCalories(Instant.ofEpochMilli(session.date.time), end, session.sportType)
        healthConnectChecked = true
        _uiState.value = _uiState.value.copy(isFetchingHealthConnect = false, healthConnectCalories = calories)
        if (calories != null) logCaloriesInternal()
    }

    fun setCalorieWeight(weight: Double) {
        _uiState.value = _uiState.value.copy(calorieWeight = weight)
    }

    fun logCalories() {
        viewModelScope.launch { logCaloriesInternal() }
    }

    private suspend fun logCaloriesInternal() {
        val state = _uiState.value
        val session = state.session ?: return
        val playerUUID = currentUserStore.currentPlayerId() ?: return
        if (state.isLoggingCalories) return
        _uiState.value = state.copy(isLoggingCalories = true)

        val weight = state.calorieWeight
        state.currentPlayer?.let { player ->
            if (player.weightKg != weight) {
                runCatching { playerRepo.updatePlayer(player.copy(weightKg = weight)) }
                _uiState.value = _uiState.value.copy(currentPlayer = player.copy(weightKg = weight))
            }
        }

        val duration = session.durationMinutes ?: 120
        val hcCalories = state.healthConnectCalories
        val calories = hcCalories ?: METEstimator.estimate(
            durationMinutes = duration,
            weightKg = weight,
            intensity = if (session.gameType == CasualGameType.SINGLES) PlayIntensity.COMPETITIVE else PlayIntensity.CASUAL,
            sport = session.sportType,
        )
        val record = CalorieRecord(
            sessionId = session.id,
            playerId = playerUUID,
            calories = calories,
            source = if (hcCalories != null) CalorieSource.HEALTH_CONNECT else CalorieSource.ESTIMATED,
            weightUsedKg = weight,
            durationMinutes = duration,
            date = session.date,
            sessionTitle = session.title,
            activityType = CalorieActivityType.OPEN_PLAY,
        )

        _uiState.value = try {
            calorieRepo.save(record)
            _uiState.value.copy(
                existingCalorieRecord = record,
                isLoggingCalories = false,
                statusMessage = "Calories logged: ${formatKcal(record.calories)}",
            )
        } catch (e: Exception) {
            _uiState.value.copy(
                isLoggingCalories = false,
                statusMessage = "Failed to log calories: ${e.message}",
            )
        }
    }

    fun join() {
        viewModelScope.launch {
            val playerId = _uiState.value.currentPlayerId ?: run {
                setStatus("You must be signed in to join.")
                return@launch
            }
            val session = _uiState.value.session
            if (session != null && !session.isJoinable) { setStatus("This session is no longer open."); return@launch }
            if (_uiState.value.hasJoined) { setStatus("You've already joined this session."); return@launch }
            try {
                sessionRepo.join(sessionId, playerId)
                setStatus("You're in! See you on the court!")
                loadInternal()
            } catch (e: Exception) {
                setStatus("Failed to join: ${e.message}")
            }
        }
    }

    fun leave() {
        viewModelScope.launch {
            val playerId = _uiState.value.currentPlayerId ?: return@launch
            try {
                sessionRepo.leave(sessionId, playerId)
                setStatus("You've left this session.")
                loadInternal()
            } catch (e: Exception) {
                setStatus("Failed to leave: ${e.message}")
            }
        }
    }

    fun cancel() {
        viewModelScope.launch {
            try {
                sessionRepo.cancel(sessionId)
                setStatus("Session cancelled.")
                loadInternal()
            } catch (e: Exception) {
                setStatus("Failed to cancel: ${e.message}")
            }
        }
    }

    fun finish() {
        viewModelScope.launch {
            val session = _uiState.value.session ?: return@launch
            try {
                sessionRepo.update(
                    session.copy(statusRaw = PlaySessionStatus.COMPLETED.rawValue),
                )
                setStatus("Session marked as complete!")
                loadInternal()
            } catch (e: Exception) {
                setStatus("Failed to finish: ${e.message}")
            }
        }
    }

    fun update(
        title: String,
        date: Date,
        durationMinutes: Int?,
        skillLevel: SkillLevel,
        gameType: CasualGameType,
        preferredAgeGroup: AgeGroup,
        costPerPerson: Double?,
        notes: String?,
    ) {
        viewModelScope.launch {
            val session = _uiState.value.session ?: return@launch
            try {
                sessionRepo.update(
                    session.copy(
                        title = title,
                        date = date,
                        durationMinutes = durationMinutes,
                        skillLevelRaw = skillLevel.rawValue,
                        gameTypeRaw = gameType.rawValue,
                        preferredAgeGroupRaw = preferredAgeGroup.rawValue,
                        costPerPerson = costPerPerson,
                        notes = notes,
                    ),
                )
                setStatus("Session updated.")
                loadInternal()
            } catch (e: Exception) {
                setStatus("Failed to update: ${e.message}")
            }
        }
    }

    fun delete() {
        viewModelScope.launch {
            try {
                sessionRepo.delete(sessionId)
                _uiState.value = _uiState.value.copy(didDelete = true)
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(errorMessage = e.message)
            }
        }
    }

    private fun setStatus(message: String) {
        _uiState.value = _uiState.value.copy(statusMessage = message)
    }

    private suspend fun fetchAttendees(ids: List<String>): List<Player> {
        if (ids.isEmpty()) return emptyList()
        return ids.chunked(30).flatMap { chunk ->
            chunk.mapNotNull { id -> playerRepo.findPlayerById(id) }
        }
    }
}

internal fun formatKcal(calories: Double): String = "${calories.roundToInt()} kcal"
