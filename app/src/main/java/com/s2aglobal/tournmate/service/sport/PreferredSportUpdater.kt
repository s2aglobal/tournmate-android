package com.s2aglobal.tournmate.service.sport

import com.s2aglobal.tournmate.data.local.CurrentUserStore
import com.s2aglobal.tournmate.data.repository.PlayerRepository
import com.s2aglobal.tournmate.domain.model.Player
import com.s2aglobal.tournmate.domain.model.SportType
import com.s2aglobal.tournmate.service.AnalyticsService
import com.s2aglobal.tournmate.service.auth.AuthService
import com.s2aglobal.tournmate.service.notification.NotificationService
import com.s2aglobal.tournmate.ui.theme.CurrentSport
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Single path for changing the user's sport (Play switcher and Profile), matching
 * iOS `AppContainer.setPreferredSport`: local preference, topics, then the player doc.
 */
@Singleton
class PreferredSportUpdater @Inject constructor(
    private val currentUserStore: CurrentUserStore,
    private val playerRepo: PlayerRepository,
    private val notificationService: NotificationService,
    private val authService: AuthService,
    private val analytics: AnalyticsService,
) {
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)

    /** Returns false when [sport] is already current. */
    fun setPreferredSport(sport: SportType, player: Player? = null): Boolean {
        val oldSport = CurrentSport.sport
        if (oldSport == sport) return false
        CurrentSport.sport = sport // re-theme immediately
        analytics.log(AnalyticsService.EventName.PREFERRED_SPORT_CHANGED, AnalyticsService.Param.SPORT to sport.rawValue)

        scope.launch {
            currentUserStore.setPreferredSport(sport)
            val resolved = player ?: currentPlayer() ?: return@launch // guest: local preference only
            notificationService.switchSportTopics(resolved.copy(preferredSport = sport), oldSport, sport)
            try {
                playerRepo.updatePlayerFields(resolved.id, mapOf("preferredSport" to sport.rawValue))
            } catch (_: Exception) {}
        }
        return true
    }

    private suspend fun currentPlayer(): Player? = runCatching {
        currentUserStore.currentPlayerId()?.let { playerRepo.findPlayerById(it) }
            ?: authService.currentUser?.uid?.let { playerRepo.findPlayerByFirebaseUid(it) }
    }.getOrNull()
}
