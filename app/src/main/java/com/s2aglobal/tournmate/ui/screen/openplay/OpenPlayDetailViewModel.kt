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
import dagger.hilt.android.lifecycle.HiltViewModel
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
)

@HiltViewModel
class OpenPlayDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val sessionRepo: PlaySessionRepository,
    private val playerRepo: PlayerRepository,
    private val currentUserStore: CurrentUserStore,
) : ViewModel() {

    private val sessionId: String = savedStateHandle["sessionId"] ?: ""

    private val _uiState = MutableStateFlow(OpenPlayDetailUiState())
    val uiState: StateFlow<OpenPlayDetailUiState> = _uiState.asStateFlow()

    init {
        load()
    }

    fun load() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            try {
                val session = sessionRepo.find(sessionId)
                val playerId = currentUserStore.currentPlayerId()?.toString()?.uppercase()
                val firebaseUid = currentUserStore.firebaseUid()

                val attendees = if (session != null) {
                    fetchAttendees(session.attendeeIds)
                } else emptyList()

                _uiState.value = _uiState.value.copy(
                    session = session,
                    attendees = attendees,
                    currentPlayerId = playerId,
                    firebaseUid = firebaseUid,
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

    fun join() {
        viewModelScope.launch {
            val playerId = _uiState.value.currentPlayerId ?: return@launch
            try {
                sessionRepo.join(sessionId, playerId)
                load()
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(errorMessage = e.message)
            }
        }
    }

    fun leave() {
        viewModelScope.launch {
            val playerId = _uiState.value.currentPlayerId ?: return@launch
            try {
                sessionRepo.leave(sessionId, playerId)
                load()
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(errorMessage = e.message)
            }
        }
    }

    fun cancel() {
        viewModelScope.launch {
            try {
                sessionRepo.cancel(sessionId)
                load()
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(errorMessage = e.message)
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
                load()
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(errorMessage = e.message)
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
                load()
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(errorMessage = e.message)
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

    private suspend fun fetchAttendees(ids: List<String>): List<Player> {
        if (ids.isEmpty()) return emptyList()
        return ids.chunked(30).flatMap { chunk ->
            chunk.mapNotNull { id -> playerRepo.findPlayerById(id) }
        }
    }
}
