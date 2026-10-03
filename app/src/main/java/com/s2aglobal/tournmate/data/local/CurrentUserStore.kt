package com.s2aglobal.tournmate.data.local

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.s2aglobal.tournmate.domain.model.SportType
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.util.UUID
import javax.inject.Inject
import javax.inject.Singleton

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "current_user")

@Singleton
class CurrentUserStore @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    private object Keys {
        val PLAYER_ID = stringPreferencesKey("currentPlayerId")
        val FIREBASE_UID = stringPreferencesKey("firebaseUid")
        val PENDING_DISPLAY_NAME = stringPreferencesKey("pendingDisplayName")
        val HAS_SEEN_ONBOARDING = booleanPreferencesKey("hasSeenOnboarding")
        val PREFERRED_SPORT = stringPreferencesKey("preferredSport")
    }

    val currentPlayerIdFlow: Flow<UUID?> = context.dataStore.data.map { prefs ->
        prefs[Keys.PLAYER_ID]?.let { runCatching { UUID.fromString(it) }.getOrNull() }
    }

    val firebaseUidFlow: Flow<String?> = context.dataStore.data.map { prefs ->
        prefs[Keys.FIREBASE_UID]
    }

    val hasSeenOnboardingFlow: Flow<Boolean> = context.dataStore.data.map { prefs ->
        prefs[Keys.HAS_SEEN_ONBOARDING] ?: false
    }

    val preferredSportFlow: Flow<SportType> = context.dataStore.data.map { prefs ->
        SportType.fromRawValue(prefs[Keys.PREFERRED_SPORT])
    }

    suspend fun currentPlayerId(): UUID? =
        currentPlayerIdFlow.first()

    suspend fun firebaseUid(): String? =
        firebaseUidFlow.first()

    suspend fun hasSeenOnboarding(): Boolean =
        hasSeenOnboardingFlow.first()

    suspend fun pendingDisplayName(): String? =
        context.dataStore.data.first()[Keys.PENDING_DISPLAY_NAME]

    suspend fun setCurrentPlayerId(id: UUID?) {
        context.dataStore.edit { prefs ->
            if (id != null) prefs[Keys.PLAYER_ID] = id.toString()
            else prefs.remove(Keys.PLAYER_ID)
        }
    }

    suspend fun setFirebaseUid(uid: String?) {
        context.dataStore.edit { prefs ->
            if (uid != null) prefs[Keys.FIREBASE_UID] = uid
            else prefs.remove(Keys.FIREBASE_UID)
        }
    }

    suspend fun setPendingDisplayName(name: String?) {
        context.dataStore.edit { prefs ->
            if (name != null) prefs[Keys.PENDING_DISPLAY_NAME] = name
            else prefs.remove(Keys.PENDING_DISPLAY_NAME)
        }
    }

    suspend fun setHasSeenOnboarding(seen: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[Keys.HAS_SEEN_ONBOARDING] = seen
        }
    }

    suspend fun setPreferredSport(sport: SportType) {
        context.dataStore.edit { prefs ->
            prefs[Keys.PREFERRED_SPORT] = sport.rawValue
        }
    }

    /** Clears the signed-in session only; the preferred sport survives sign-out (matches iOS). */
    suspend fun clear() {
        context.dataStore.edit { prefs ->
            prefs.remove(Keys.FIREBASE_UID)
            prefs.remove(Keys.PLAYER_ID)
            prefs.remove(Keys.PENDING_DISPLAY_NAME)
            prefs[Keys.HAS_SEEN_ONBOARDING] = false
        }
    }
}
