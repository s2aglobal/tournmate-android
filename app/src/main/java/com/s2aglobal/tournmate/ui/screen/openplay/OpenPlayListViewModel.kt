package com.s2aglobal.tournmate.ui.screen.openplay

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.s2aglobal.tournmate.data.local.CurrentUserStore
import com.s2aglobal.tournmate.data.repository.PlaySessionRepository
import com.s2aglobal.tournmate.data.repository.PlayerRepository
import com.s2aglobal.tournmate.domain.model.*
import com.s2aglobal.tournmate.service.region.RegionNormalizer
import com.s2aglobal.tournmate.service.validation.EventRateLimiter
import com.s2aglobal.tournmate.service.validation.EventType
import com.s2aglobal.tournmate.service.validation.InputValidator
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.Date
import java.util.UUID
import javax.inject.Inject

enum class OpenPlayFilter(val displayName: String) {
    ALL_SESSIONS("All Sessions"),
    MY_SESSIONS("My Sessions"),
    COMPLETED("Completed"),
}

data class OpenPlayListUiState(
    val allUpcoming: List<PlaySession> = emptyList(),
    val allPast: List<PlaySession> = emptyList(),
    val filter: OpenPlayFilter = OpenPlayFilter.ALL_SESSIONS,
    val isLoading: Boolean = false,
    val errorMessage: String? = null,
    val currentPlayer: Player? = null,
    val currentPlayerId: String? = null,
    val firebaseUid: String? = null,
) {
    val displaySessions: List<PlaySession>
        get() = when (filter) {
            OpenPlayFilter.ALL_SESSIONS -> allUpcoming.filter { passesRegion(it) }
            OpenPlayFilter.MY_SESSIONS -> allUpcoming.filter { isMine(it) }
            OpenPlayFilter.COMPLETED -> allPast.filter { passesRegion(it) }
        }

    val isEmpty: Boolean
        get() = displaySessions.isEmpty()

    private fun isMine(session: PlaySession): Boolean {
        val uid = firebaseUid ?: return false
        val pid = currentPlayerId ?: return false
        return session.hostId == uid || session.attendeeIds.contains(pid)
    }

    private fun passesRegion(session: PlaySession): Boolean {
        val player = currentPlayer ?: return true
        return RegionNormalizer.sessionMatchesPlayerRegion(
            sessionCountry = session.countryCode,
            sessionPostal = session.postalCode,
            playerCountry = player.homeCountryCode,
            playerPostal = player.homePostalCode,
        )
    }
}

@HiltViewModel
class OpenPlayListViewModel @Inject constructor(
    private val sessionRepo: PlaySessionRepository,
    private val playerRepo: PlayerRepository,
    private val currentUserStore: CurrentUserStore,
) : ViewModel() {

    private val _uiState = MutableStateFlow(OpenPlayListUiState())
    val uiState: StateFlow<OpenPlayListUiState> = _uiState.asStateFlow()

    init {
        load()
    }

    fun load() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            try {
                val upcoming = sessionRepo.listUpcoming()
                val past = sessionRepo.listPast()
                val playerUUID = currentUserStore.currentPlayerId()
                val playerId = playerUUID?.toString()?.uppercase()
                val firebaseUid = currentUserStore.firebaseUid()

                var player: Player? = null
                if (playerId != null) {
                    player = playerRepo.findPlayerById(playerId)
                }

                _uiState.value = _uiState.value.copy(
                    allUpcoming = upcoming,
                    allPast = past,
                    isLoading = false,
                    errorMessage = null,
                    currentPlayer = player,
                    currentPlayerId = playerId,
                    firebaseUid = firebaseUid,
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = e.message ?: "Failed to load sessions",
                )
            }
        }
    }

    fun setFilter(filter: OpenPlayFilter) {
        _uiState.value = _uiState.value.copy(filter = filter)
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(errorMessage = null)
    }

    fun createSession(
        title: String,
        venue: String,
        venueAddress: String,
        venueLatitude: Double?,
        venueLongitude: Double?,
        date: Date,
        durationMinutes: Int?,
        skillLevel: SkillLevel,
        gameType: CasualGameType,
        costPerPerson: Double?,
        currency: String,
        notes: String?,
        ageGroup: AgeGroup,
        sportType: SportType,
    ) {
        viewModelScope.launch {
            val state = _uiState.value

            val profileCheck = EventRateLimiter.hasCompleteProfile(state.currentPlayer)
            if (!profileCheck.isValid) { setError(profileCheck.errorMessage); return@launch }

            val cooldownCheck = EventRateLimiter.canCreate(EventType.OPEN_PLAY)
            if (!cooldownCheck.isValid) { setError(cooldownCheck.errorMessage); return@launch }

            val dailyCheck = EventRateLimiter.checkDailyLimit(EventType.OPEN_PLAY)
            if (!dailyCheck.isValid) { setError(dailyCheck.errorMessage); return@launch }

            val titleCheck = InputValidator.validateEventTitle(title)
            if (!titleCheck.isValid) { setError(titleCheck.errorMessage); return@launch }

            val venueCheck = InputValidator.validateEventVenue(venue)
            if (!venueCheck.isValid) { setError(venueCheck.errorMessage); return@launch }

            if (notes != null) {
                val notesCheck = InputValidator.validateEventNotes(notes)
                if (!notesCheck.isValid) { setError(notesCheck.errorMessage); return@launch }
            }

            if (date.before(Date())) {
                setError("Session date must be in the future.")
                return@launch
            }

            try {
                val session = PlaySession(
                    id = UUID.randomUUID(),
                    title = title,
                    venue = venue,
                    venueAddress = venueAddress,
                    venueLatitude = venueLatitude,
                    venueLongitude = venueLongitude,
                    countryCode = state.currentPlayer?.homeCountryCode,
                    postalCode = state.currentPlayer?.homePostalCode,
                    date = date,
                    durationMinutes = durationMinutes,
                    skillLevelRaw = skillLevel.rawValue,
                    gameTypeRaw = gameType.rawValue,
                    costPerPerson = costPerPerson,
                    currency = currency,
                    notes = notes,
                    preferredAgeGroupRaw = ageGroup.rawValue,
                    statusRaw = PlaySessionStatus.ACTIVE.rawValue,
                    hostId = state.firebaseUid,
                    hostName = state.currentPlayer?.name,
                    hostAvatarId = state.currentPlayer?.avatarId,
                    createdAt = Date(),
                    timeZone = java.util.TimeZone.getDefault().id,
                    sportType = sportType,
                    attendeeIds = listOf(state.currentPlayerId ?: ""),
                )
                sessionRepo.create(session)
                EventRateLimiter.recordCreation(EventType.OPEN_PLAY)
                load()
            } catch (e: Exception) {
                setError(e.message)
            }
        }
    }

    fun joinSession(sessionId: String) {
        viewModelScope.launch {
            val playerId = _uiState.value.currentPlayerId ?: return@launch
            try {
                sessionRepo.join(sessionId, playerId)
                load()
            } catch (e: Exception) {
                setError(e.message)
            }
        }
    }

    fun leaveSession(sessionId: String) {
        viewModelScope.launch {
            val playerId = _uiState.value.currentPlayerId ?: return@launch
            try {
                sessionRepo.leave(sessionId, playerId)
                load()
            } catch (e: Exception) {
                setError(e.message)
            }
        }
    }

    fun cancelSession(sessionId: String) {
        viewModelScope.launch {
            try {
                sessionRepo.cancel(sessionId)
                load()
            } catch (e: Exception) {
                setError(e.message)
            }
        }
    }

    fun deleteSession(sessionId: String) {
        viewModelScope.launch {
            try {
                sessionRepo.delete(sessionId)
                load()
            } catch (e: Exception) {
                setError(e.message)
            }
        }
    }

    private fun setError(message: String?) {
        _uiState.value = _uiState.value.copy(errorMessage = message)
    }
}
