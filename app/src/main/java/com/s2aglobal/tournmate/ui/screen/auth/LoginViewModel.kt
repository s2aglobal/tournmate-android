package com.s2aglobal.tournmate.ui.screen.auth

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.s2aglobal.tournmate.data.local.CurrentUserStore
import com.s2aglobal.tournmate.service.auth.AuthResult
import com.s2aglobal.tournmate.service.auth.AuthService
import com.s2aglobal.tournmate.service.auth.friendlyAuthError
import com.s2aglobal.tournmate.service.auth.requiresEmailVerification
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
    val showResetPassword: Boolean = false,
    val resetEmail: String = "",
    val isSendingReset: Boolean = false,
    val resetConfirmation: String? = null,
)

@HiltViewModel
class LoginViewModel @Inject constructor(
    private val authService: AuthService,
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

    fun clearError() {
        _uiState.value = _uiState.value.copy(error = null)
    }

    fun signInWithGoogle(context: Context, onSuccess: () -> Unit) {
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            when (val result = authService.signInWithGoogle(context)) {
                is AuthResult.Success -> {
                    result.displayName?.takeIf { it.isNotBlank() }?.let { currentUserStore.setPendingDisplayName(it) }
                    handleAuthSuccess(result.user.uid, onSuccess)
                }
                is AuthResult.Error -> _uiState.value = _uiState.value.copy(isLoading = false, error = result.message)
                AuthResult.Cancelled -> _uiState.value = _uiState.value.copy(isLoading = false)
            }
        }
    }

    fun signInWithEmail(onSuccess: () -> Unit) {
        if (!validateEmailFields(forCreation = false)) return
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            when (val result = authService.signInWithEmail(normalizedEmail(), _uiState.value.password)) {
                is AuthResult.Success -> handleAuthSuccess(result.user.uid, onSuccess)
                is AuthResult.Error -> _uiState.value = _uiState.value.copy(isLoading = false, error = result.message)
                AuthResult.Cancelled -> _uiState.value = _uiState.value.copy(isLoading = false)
            }
        }
    }

    /** Creates the account; [displayName] is carried into Profile Setup. */
    fun createAccount(displayName: String, onSuccess: () -> Unit) {
        if (!validateEmailFields(forCreation = true)) return
        viewModelScope.launch {
            displayName.trim().takeIf { it.isNotEmpty() }?.let { currentUserStore.setPendingDisplayName(it) }
            _uiState.value = _uiState.value.copy(isLoading = true, error = null)
            when (val result = authService.createAccount(normalizedEmail(), _uiState.value.password)) {
                is AuthResult.Success -> {
                    if (requiresEmailVerification) authService.sendEmailVerification()
                    handleAuthSuccess(result.user.uid, onSuccess)
                }
                is AuthResult.Error -> _uiState.value = _uiState.value.copy(isLoading = false, error = result.message)
                AuthResult.Cancelled -> _uiState.value = _uiState.value.copy(isLoading = false)
            }
        }
    }

    fun showResetPassword() {
        _uiState.value = _uiState.value.copy(
            showResetPassword = true,
            resetEmail = _uiState.value.email,
            error = null,
        )
    }

    fun dismissResetPassword() {
        _uiState.value = _uiState.value.copy(showResetPassword = false)
    }

    fun updateResetEmail(email: String) {
        _uiState.value = _uiState.value.copy(resetEmail = email, error = null)
    }

    fun sendPasswordReset() {
        val trimmed = _uiState.value.resetEmail.trim()
        if (trimmed.isEmpty()) {
            _uiState.value = _uiState.value.copy(error = "Please enter your email address.")
            return
        }
        viewModelScope.launch {
            _uiState.value = _uiState.value.copy(isSendingReset = true, error = null)
            authService.resetPassword(trimmed.lowercase())
                .onSuccess {
                    _uiState.value = _uiState.value.copy(
                        isSendingReset = false,
                        showResetPassword = false,
                        resetConfirmation = "If an account exists for $trimmed, a password reset link has been sent. Check your inbox and spam folder.",
                    )
                }
                .onFailure {
                    _uiState.value = _uiState.value.copy(isSendingReset = false, error = friendlyAuthError(it))
                }
        }
    }

    private fun normalizedEmail(): String = _uiState.value.email.trim().lowercase()

    private fun validateEmailFields(forCreation: Boolean): Boolean {
        val state = _uiState.value
        val trimmedEmail = state.email.trim()
        val error = when {
            trimmedEmail.isEmpty() || state.password.isEmpty() -> "Please enter both email and password."
            !trimmedEmail.contains("@") || !trimmedEmail.contains(".") -> "Please enter a valid email address."
            state.password.length < 6 -> "Password must be at least 6 characters."
            forCreation && state.password != state.confirmPassword -> "Passwords do not match."
            else -> null
        }
        if (error != null) _uiState.value = state.copy(error = error)
        return error == null
    }

    /** Keeps the loading overlay up while the auth gate resolves the next screen. */
    private suspend fun handleAuthSuccess(uid: String, onSuccess: () -> Unit) {
        currentUserStore.setFirebaseUid(uid)
        onSuccess()
    }
}
