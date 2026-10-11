package com.s2aglobal.tournmate.service.auth

import android.util.Log
import com.s2aglobal.tournmate.BuildConfig
import com.s2aglobal.tournmate.data.local.CurrentUserStore
import com.s2aglobal.tournmate.data.repository.PlayerRepository
import com.s2aglobal.tournmate.domain.model.Player
import kotlinx.coroutines.CancellationException
import javax.inject.Inject
import javax.inject.Singleton

/**
 * The signed-in player (iOS `AppContainer.currentPlayer()`). Looks up the stored player id, then
 * falls back to the Firebase uid (and repairs the stored id): right after signing in, the id can be
 * missing or the first read can fail, which used to show "Profile Not Found".
 */
@Singleton
class CurrentPlayerLookup @Inject constructor(
    private val currentUserStore: CurrentUserStore,
    private val playerRepo: PlayerRepository,
    private val authService: AuthService,
) {
    suspend fun currentPlayer(): Player? {
        currentUserStore.currentPlayerId()?.let { id ->
            try {
                playerRepo.findPlayerById(id)?.let { return it }
            } catch (e: CancellationException) {
                throw e
            } catch (e: Exception) {
                if (BuildConfig.DEBUG) Log.w(TAG, "lookup by id failed: ${e.message}")
            }
        }
        val uid = authService.currentUser?.uid ?: currentUserStore.firebaseUid() ?: return null
        val player = try {
            playerRepo.findPlayerByFirebaseUid(uid)
        } catch (e: CancellationException) {
            throw e
        } catch (_: Exception) {
            null
        } ?: return null
        currentUserStore.setCurrentPlayerId(player.id)
        return player
    }

    private companion object {
        const val TAG = "CurrentPlayerLookup"
    }
}
