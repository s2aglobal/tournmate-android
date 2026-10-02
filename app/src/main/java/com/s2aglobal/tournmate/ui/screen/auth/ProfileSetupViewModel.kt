package com.s2aglobal.tournmate.ui.screen.auth

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.s2aglobal.tournmate.data.local.CurrentUserStore
import com.s2aglobal.tournmate.data.repository.PlayerRepository
import com.s2aglobal.tournmate.domain.model.Gender
import com.s2aglobal.tournmate.domain.model.SportType
import com.s2aglobal.tournmate.service.auth.AuthService
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.launch
import java.util.Date
import javax.inject.Inject

@HiltViewModel
class ProfileSetupViewModel @Inject constructor(
    private val authService: AuthService,
    private val playerRepo: PlayerRepository,
    private val currentUserStore: CurrentUserStore,
) : ViewModel() {

    fun saveProfile(
        name: String,
        gender: Gender,
        avatarId: String,
        homeCountryCode: String?,
        homePostalCode: String?,
        dateOfBirth: Date?,
        preferredSport: SportType,
        onComplete: () -> Unit,
    ) {
        viewModelScope.launch {
            val uid = authService.currentUser?.uid ?: return@launch
            val email = authService.currentUser?.email ?: ""

            val player = playerRepo.addPlayer(
                name = name,
                phone = "",
                email = email,
                gender = gender,
                avatarId = avatarId,
                homeCountryCode = homeCountryCode,
                homePostalCode = homePostalCode,
                firebaseUid = uid,
                dateOfBirth = dateOfBirth,
                preferredSport = preferredSport,
            )

            currentUserStore.setCurrentPlayerId(player.id)
            currentUserStore.setFirebaseUid(uid)
            currentUserStore.setPreferredSport(preferredSport)
            onComplete()
        }
    }
}
