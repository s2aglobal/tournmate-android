package com.s2aglobal.tournmate.ui.screen.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.s2aglobal.tournmate.data.local.CurrentUserStore
import com.s2aglobal.tournmate.data.repository.PlayerRepository
import com.s2aglobal.tournmate.service.auth.AuthService
import com.s2aglobal.tournmate.service.notification.NotificationService
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

enum class AuthState {
    LOADING,
    SIGNED_OUT,
    NEEDS_PROFILE,
    NEEDS_ONBOARDING,
    SIGNED_IN,
}

@HiltViewModel
class AuthGateViewModel @Inject constructor(
    private val authService: AuthService,
    private val playerRepo: PlayerRepository,
    private val currentUserStore: CurrentUserStore,
    private val notificationService: NotificationService,
) : ViewModel() {

    private val _authState = MutableStateFlow(AuthState.LOADING)
    val authState: StateFlow<AuthState> = _authState.asStateFlow()

    private val _isGuestMode = MutableStateFlow(false)
    val isGuestMode: StateFlow<Boolean> = _isGuestMode.asStateFlow()

    init {
        checkAuthState()
    }

    fun checkAuthState() {
        viewModelScope.launch {
            _authState.value = AuthState.LOADING

            val firebaseUser = authService.currentUser
            if (firebaseUser == null) {
                _authState.value = AuthState.SIGNED_OUT
                return@launch
            }

            val savedPlayerId = currentUserStore.currentPlayerId()
            if (savedPlayerId != null) {
                val player = playerRepo.findPlayerById(savedPlayerId)
                if (player != null) {
                    // Player doc is the source of truth for sport; local copy can be stale.
                    currentUserStore.setPreferredSport(player.preferredSport)
                    notificationService.subscribeToHomeRegion(player)
                    val hasSeenOnboarding = currentUserStore.hasSeenOnboarding()
                    _authState.value = if (hasSeenOnboarding) AuthState.SIGNED_IN
                    else AuthState.NEEDS_ONBOARDING
                    return@launch
                }
            }

            val player = playerRepo.findPlayerByFirebaseUid(firebaseUser.uid)
            if (player != null) {
                currentUserStore.setCurrentPlayerId(player.id)
                currentUserStore.setFirebaseUid(firebaseUser.uid)
                currentUserStore.setPreferredSport(player.preferredSport)
                notificationService.subscribeToHomeRegion(player)
                val hasSeenOnboarding = currentUserStore.hasSeenOnboarding()
                _authState.value = if (hasSeenOnboarding) AuthState.SIGNED_IN
                else AuthState.NEEDS_ONBOARDING
            } else {
                currentUserStore.setFirebaseUid(firebaseUser.uid)
                _authState.value = AuthState.NEEDS_PROFILE
            }
        }
    }

    fun continueAsGuest() {
        _isGuestMode.value = true
        _authState.value = AuthState.SIGNED_IN
    }

    fun signOut() {
        viewModelScope.launch {
            authService.signOut()
            currentUserStore.clear()
            _isGuestMode.value = false
            _authState.value = AuthState.SIGNED_OUT
        }
    }
}
