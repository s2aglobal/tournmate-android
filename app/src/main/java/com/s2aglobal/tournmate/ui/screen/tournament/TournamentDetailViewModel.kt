package com.s2aglobal.tournmate.ui.screen.tournament

import androidx.lifecycle.SavedStateHandle
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.s2aglobal.tournmate.data.local.CurrentUserStore
import com.s2aglobal.tournmate.data.repository.PlayerRepository
import com.s2aglobal.tournmate.data.repository.RegistrationRepository
import com.s2aglobal.tournmate.data.repository.TournamentRepository
import com.s2aglobal.tournmate.domain.model.Player
import com.s2aglobal.tournmate.domain.model.Registration
import com.s2aglobal.tournmate.domain.model.Tournament
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.UUID
import javax.inject.Inject

data class TournamentDetailUiState(
    val tournament: Tournament? = null,
    val registrations: List<Registration> = emptyList(),
    val currentPlayer: Player? = null,
    val currentPlayerRegistration: Registration? = null,
    val isLoading: Boolean = true,
    val errorMessage: String? = null,
    val firebaseUid: String? = null,
    val isRegistering: Boolean = false,
    val organizerName: String? = null,
) {
    val isCreator: Boolean
        get() = firebaseUid != null && tournament?.createdBy == firebaseUid

    val isRegistered: Boolean
        get() = currentPlayerRegistration != null

    val canRegister: Boolean
        get() = tournament != null &&
                !tournament.isRegistrationClosed &&
                currentPlayer != null &&
                !isRegistered &&
                tournament.status.rawValue != "cancelled"

    val teamCount: Int
        get() = registrations.size
}

@HiltViewModel
class TournamentDetailViewModel @Inject constructor(
    savedStateHandle: SavedStateHandle,
    private val tournamentRepo: TournamentRepository,
    private val registrationRepo: RegistrationRepository,
    private val playerRepo: PlayerRepository,
    private val currentUserStore: CurrentUserStore,
) : ViewModel() {

    private val tournamentId: String = savedStateHandle["tournamentId"] ?: ""

    private val _uiState = MutableStateFlow(TournamentDetailUiState())
    val uiState: StateFlow<TournamentDetailUiState> = _uiState.asStateFlow()

    init {
        load()
    }

    fun load() {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true)
            try {
                val tid = UUID.fromString(tournamentId)
                val tournament = tournamentRepo.findTournament(tid)
                    ?: run {
                        _uiState.value = _uiState.value.copy(isLoading = false, errorMessage = "Tournament not found")
                        return@launch
                    }

                val registrations = registrationRepo.listRegistrations(tournament)
                val playerId = currentUserStore.currentPlayerId()
                val firebaseUid = currentUserStore.firebaseUid()
                var currentPlayer: Player? = null
                var myReg: Registration? = null

                if (playerId != null) {
                    currentPlayer = playerRepo.findPlayerById(playerId)
                    myReg = registrations.firstOrNull { it.containsPlayerID(playerId) }
                }

                var organizerName: String? = null
                tournament.createdBy?.let { uid ->
                    organizerName = try {
                        playerRepo.findPlayerByFirebaseUid(uid)?.name
                    } catch (_: Exception) { null }
                }

                _uiState.value = TournamentDetailUiState(
                    tournament = tournament,
                    registrations = registrations,
                    currentPlayer = currentPlayer,
                    currentPlayerRegistration = myReg,
                    isLoading = false,
                    firebaseUid = firebaseUid,
                    organizerName = organizerName,
                )
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    errorMessage = e.message ?: "Failed to load tournament",
                )
            }
        }
    }

    fun register() {
        viewModelScope.launch {
            val state = _uiState.value
            val player = state.currentPlayer ?: return@launch
            val tournament = state.tournament ?: return@launch

            _uiState.value = state.copy(isRegistering = true)
            try {
                registrationRepo.createRegistration(player, null, tournament)
                load()
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isRegistering = false,
                    errorMessage = e.message,
                )
            }
        }
    }

    fun unregister() {
        viewModelScope.launch {
            val state = _uiState.value
            val reg = state.currentPlayerRegistration ?: return@launch

            try {
                registrationRepo.deleteRegistration(reg)
                load()
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(errorMessage = e.message)
            }
        }
    }

    fun clearError() {
        _uiState.value = _uiState.value.copy(errorMessage = null)
    }
}
