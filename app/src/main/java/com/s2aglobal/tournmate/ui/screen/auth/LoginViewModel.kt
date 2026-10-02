package com.s2aglobal.tournmate.ui.screen.auth

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.s2aglobal.tournmate.data.local.CurrentUserStore
import com.s2aglobal.tournmate.data.repository.PlayerRepository
import com.s2aglobal.tournmate.service.auth.AuthResult
import com.s2aglobal.tournmate.service.auth.AuthService
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import javax.inject.Inject

data class LoginUiState(
    val isLoading: Boolean = false,
    val error: String? = null,
    val isCreateAccount: Boolean = false,
    val email: String = "",
    val password: String = "",
    val confirmPassword: String = "",
)

@HiltViewModel
class LoginViewModel @Inject constructor(
    private val authService: AuthService,
    private val playerRepo: PlayerRepository,
    private val currentUserStore: CurrentUserStore,
) : ViewModel() {

    private val _uiState = MutableStateFlow(LoginUiState())
    val uiState: StateFlow<LoginUiState> = _uiState.asStateFlow()

    fun updateEmail(email: String) {
        _uiState.value = _uiState.value.copy(email = email, error = null)
    }

    fun updatePassword(password: String) {
        _uiState.value = _uiState.value.copy(password = password, error = null)
    }

    fun updateConfirmPassword(password: String) {
        _uiState.value = _uiState.value.copy(confirmPassword = password, error = null)
    }

    fun toggleCreateAccount() {
        _uiState.value = _uiState.value.copy(
            isCreateAccount = !_uiState.value.isCreateAccount,
            error = null,
        )
    }

    fun signInWithGoogle(context: Context, onSuccess: (needsProfile: Boolean) -> Unit) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            when (val result = authService.signInWithGoogle(context)) {
                is AuthResult.Success -> handleAuthSuccess(result.user.uid, onSuccess)
                is AuthResult.Error -> _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = result.message,
                )
            }
        }
    }

    fun signInWithEmail(onSuccess: (needsProfile: Boolean) -> Unit) {
        val state = _uiState.value
        if (state.email.isBlank() || state.password.isBlank()) {
            _uiState.value = state.copy(error = "Please enter email and password")
            return
        }
        viewModelScope.launch {
            _uiState.value = state.copy(isLoading = true, error = null)
            when (val result = authService.signInWithEmail(state.email, state.password)) {
                is AuthResult.Success -> handleAuthSuccess(result.user.uid, onSuccess)
                is AuthResult.Error -> _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = result.message,
                )
            }
        }
    }

    fun createAccount(onSuccess: (needsProfile: Boolean) -> Unit) {
        val state = _uiState.value
        if (state.email.isBlank() || state.password.isBlank()) {
            _uiState.value = state.copy(error = "Please enter email and password")
            return
        }
        if (state.password.length < 6) {
            _uiState.value = state.copy(error = "Password must be at least 6 characters")
            return
        }
        viewModelScope.launch {
            _uiState.value = state.copy(isLoading = true, error = null)
            when (val result = authService.createAccount(state.email, state.password)) {
                is AuthResult.Success -> handleAuthSuccess(result.user.uid, onSuccess)
                is AuthResult.Error -> _uiState.value = _uiState.value.copy(
                    isLoading = false,
                    error = result.message,
                )
            }
        }
    }

    fun resetPassword() {
        val email = _uiState.value.email
        if (email.isBlank()) {
            _uiState.value = _uiState.value.copy(error = "Enter your email first")
            return
        }
        viewModelScope.launch {
            authService.resetPassword(email)
            _uiState.value = _uiState.value.copy(error = "Password reset email sent")
        }
    }

    private suspend fun handleAuthSuccess(uid: String, onSuccess: (needsProfile: Boolean) -> Unit) {
        currentUserStore.setFirebaseUid(uid)
        val player = playerRepo.findPlayerByFirebaseUid(uid)
        if (player != null) {
            currentUserStore.setCurrentPlayerId(player.id)
            currentUserStore.setPreferredSport(player.preferredSport)
        }
        _uiState.value = _uiState.value.copy(isLoading = false)
        onSuccess(player == null)
    }
}
