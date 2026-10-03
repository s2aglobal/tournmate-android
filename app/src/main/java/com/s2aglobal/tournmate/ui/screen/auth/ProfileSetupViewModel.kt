package com.s2aglobal.tournmate.ui.screen.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.s2aglobal.tournmate.data.local.CurrentUserStore
import com.s2aglobal.tournmate.data.repository.PlayerRepository
import com.s2aglobal.tournmate.domain.model.Gender
import com.s2aglobal.tournmate.domain.model.SportType
import com.s2aglobal.tournmate.service.auth.AuthService
import com.s2aglobal.tournmate.service.region.RegionNormalizer
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.Date
import javax.inject.Inject

data class ProfileSetupUiState(
    val isSaving: Boolean = false,
    val error: String? = null,
    /** Pending display name (email sign-up / Google) or the auth provider's name. */
    val defaultName: String? = null,
)

@HiltViewModel
class ProfileSetupViewModel @Inject constructor(
    private val authService: AuthService,
    private val playerRepo: PlayerRepository,
    private val currentUserStore: CurrentUserStore,
) : ViewModel() {

    private val _uiState = MutableStateFlow(ProfileSetupUiState())
    val uiState: StateFlow<ProfileSetupUiState> = _uiState.asStateFlow()

    init {
        viewModelScope.launch {
            val pending = currentUserStore.pendingDisplayName()?.takeIf { it.isNotBlank() }
            val authName = authService.currentUser?.displayName?.takeIf { it.isNotBlank() }
            _uiState.value = _uiState.value.copy(defaultName = pending ?: authName ?: "")
        }
    }

    fun saveProfile(
        name: String,
        gender: Gender,
        avatarId: String,
        homeCountryCode: String?,
        homePostalRaw: String,
        dateOfBirth: Date?,
        preferredSport: SportType,
        onComplete: () -> Unit,
    ) {
        if (_uiState.value.isSaving) return
        val trimmedName = name.trim()
        if (trimmedName.isEmpty()) {
            _uiState.value = _uiState.value.copy(error = "Please enter your name.")
            return
        }
        if (homePostalRaw.isNotBlank()) {
            RegionNormalizer.validateHomePostalForCountry(homeCountryCode, homePostalRaw)?.let {
                _uiState.value = _uiState.value.copy(error = it)
                return
            }
        }

        _uiState.value = _uiState.value.copy(isSaving = true, error = null)
        viewModelScope.launch {
            try {
                val uid = currentUserStore.firebaseUid() ?: authService.currentUser?.uid

                val existing = uid?.let { runCatching { playerRepo.findPlayerByFirebaseUid(it) }.getOrNull() }
                if (existing != null) {
                    currentUserStore.setCurrentPlayerId(existing.id)
                    currentUserStore.setPendingDisplayName(null)
                    _uiState.value = _uiState.value.copy(isSaving = false)
                    onComplete()
                    return@launch
                }

                val cc = RegionNormalizer.normalizeCountryCode(homeCountryCode)
                val player = playerRepo.addPlayer(
                    name = trimmedName,
                    phone = "",
                    email = authService.currentUser?.email ?: "",
                    gender = gender,
                    avatarId = avatarId,
                    homeCountryCode = cc,
                    homePostalCode = RegionNormalizer.normalizePostal(homePostalRaw, cc),
                    firebaseUid = uid,
                    dateOfBirth = dateOfBirth,
                    preferredSport = preferredSport,
                )

                currentUserStore.setCurrentPlayerId(player.id)
                uid?.let { currentUserStore.setFirebaseUid(it) }
                currentUserStore.setPreferredSport(preferredSport)
                currentUserStore.setPendingDisplayName(null)
                _uiState.value = _uiState.value.copy(isSaving = false)
                onComplete()
            } catch (e: Exception) {
                _uiState.value = _uiState.value.copy(
                    isSaving = false,
                    error = "Failed to save profile: ${e.localizedMessage}",
                )
            }
        }
    }
}
