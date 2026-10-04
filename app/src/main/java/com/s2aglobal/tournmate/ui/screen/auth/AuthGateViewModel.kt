package com.s2aglobal.tournmate.ui.screen.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.s2aglobal.tournmate.data.local.CurrentUserStore
import com.s2aglobal.tournmate.data.repository.PlayerRepository
import com.s2aglobal.tournmate.domain.model.Player
import com.s2aglobal.tournmate.service.auth.AuthService
import com.s2aglobal.tournmate.service.auth.requiresEmailVerification
import com.s2aglobal.tournmate.service.notification.NotificationService
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeoutOrNull
import javax.inject.Inject

enum class AuthState {
    LOADING,
    SIGNED_OUT,
    NEEDS_VERIFICATION,
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

    val currentUserEmail: String get() = authService.currentUser?.email.orEmpty()

    init {
        checkAuthState()
    }

    /** Re-resolves the auth state; call after any sign-in. */
    fun checkAuthState() {
        viewModelScope.launch {
            _authState.value = AuthState.LOADING

            val firebaseUser = authService.currentUser
            if (firebaseUser == null) {
                _authState.value = AuthState.SIGNED_OUT
                return@launch
            }
            currentUserStore.setFirebaseUid(firebaseUser.uid)

            if (requiresEmailVerification) {
                authService.reloadCurrentUser()
                if (authService.currentUser?.isEmailVerified != true) {
                    _authState.value = AuthState.NEEDS_VERIFICATION
                    return@launch
                }
            }

            _authState.value = resolvePlayer(firebaseUser.uid)
        }
    }

    private suspend fun resolvePlayer(uid: String): AuthState {
        // Fast path: verify the cached player still exists.
        currentUserStore.currentPlayerId()?.let { cachedId ->
            val player = runCatching { playerRepo.findPlayerById(cachedId) }.getOrNull()
            if (player != null) {
                adopt(player)
                return if (currentUserStore.hasSeenOnboarding()) AuthState.SIGNED_IN
                else AuthState.NEEDS_ONBOARDING
            }
            currentUserStore.setCurrentPlayerId(null)
        }

        // Slow path: look up by Firebase UID, falling back to profile setup after 6s.
        val existing = withTimeoutOrNull(6_000) {
            runCatching { playerRepo.findPlayerByFirebaseUid(uid) }.getOrNull()
        }
        if (existing != null) {
            currentUserStore.setCurrentPlayerId(existing.id)
            adopt(existing)
            return AuthState.SIGNED_IN
        }
        return AuthState.NEEDS_PROFILE
    }

    /**
     * Player doc is the source of truth for sport; the local copy can be stale.
     * Runs on every sign-in (not once per process), so after [signOut] deleted the
     * FCM token a fresh one is synced and the topics are resubscribed.
     */
    private suspend fun adopt(player: Player) {
        currentUserStore.setPreferredSport(player.preferredSport)
        notificationService.subscribeToHomeRegion(player)
        viewModelScope.launch { notificationService.syncCurrentToken() }
    }

    fun onProfileComplete() {
        viewModelScope.launch {
            // New player doc: sync the FCM token and subscribe its topics.
            currentUserStore.currentPlayerId()?.let { id ->
                runCatching { playerRepo.findPlayerById(id) }.getOrNull()?.let { adopt(it) }
            }
            _authState.value = if (currentUserStore.hasSeenOnboarding()) AuthState.SIGNED_IN
            else AuthState.NEEDS_ONBOARDING
        }
    }

    fun onOnboardingComplete() {
        viewModelScope.launch {
            currentUserStore.setHasSeenOnboarding(true)
            _authState.value = AuthState.SIGNED_IN
        }
    }

    /** Re-checks verification; advances to profile setup / main once verified. */
    fun checkEmailVerification() {
        viewModelScope.launch {
            authService.reloadCurrentUser()
            val user = authService.currentUser ?: return@launch
            if (!user.isEmailVerified) return@launch

            currentUserStore.currentPlayerId()?.let { id ->
                runCatching { playerRepo.findPlayerById(id) }.getOrNull()?.let {
                    adopt(it)
                    _authState.value = AuthState.SIGNED_IN
                    return@launch
                }
            }
            runCatching { playerRepo.findPlayerByFirebaseUid(user.uid) }.getOrNull()?.let {
                currentUserStore.setCurrentPlayerId(it.id)
                adopt(it)
                _authState.value = AuthState.SIGNED_IN
                return@launch
            }
            _authState.value = AuthState.NEEDS_PROFILE
        }
    }

    suspend fun resendVerificationEmail(): Result<Unit> = authService.sendEmailVerification()

    fun continueAsGuest() {
        _isGuestMode.value = true
        _authState.value = AuthState.SIGNED_IN
    }

    /** Also used after account deletion (ProfileViewModel.deleteAccount -> onSignOut). */
    fun signOut() {
        viewModelScope.launch {
            // Detach pushes and clear the local inbox before dropping the session (iOS parity).
            notificationService.resetForSignOut()
            authService.signOut()
            currentUserStore.clear()
            _isGuestMode.value = false
            _authState.value = AuthState.SIGNED_OUT
        }
    }
}
