package com.s2aglobal.tournmate.service.notification

import android.Manifest
import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.core.content.ContextCompat
import com.google.firebase.messaging.FirebaseMessaging
import com.s2aglobal.tournmate.data.local.CurrentUserStore
import com.s2aglobal.tournmate.data.repository.PlayerRepository
import com.s2aglobal.tournmate.domain.model.Player
import com.s2aglobal.tournmate.domain.model.SportType
import com.s2aglobal.tournmate.service.region.RegionNormalizer
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Manages push notification permissions, FCM token lifecycle, and
 * regional topic subscriptions — mirrors iOS NotificationService.swift.
 *
 * Topic format: "region_{CC}_{postal}"  e.g. "region_US_78641"
 * Dev vs prod routing is automatic: each build flavor uses its own
 * google-services.json (dev/ vs prod/), so FCM tokens and topic
 * subscriptions always go to the correct Firebase project.
 */
@Singleton
class NotificationService @Inject constructor(
    @ApplicationContext private val context: Context,
    private val messaging: FirebaseMessaging,
    private val playerRepo: PlayerRepository,
    private val currentUserStore: CurrentUserStore,
) {

    companion object {
        const val CHANNEL_ID_GENERAL  = "tournmate_general"
        const val CHANNEL_ID_SESSIONS = "tournmate_sessions"
        const val CHANNEL_ID_MATCHES  = "tournmate_matches"

        fun topicName(countryCode: String, postalCode: String) =
            "region_${countryCode}_${postalCode}"

        fun countryTopicName(countryCode: String) =
            "country_${countryCode}"

        /**
         * Badminton keeps the legacy name ("country_US") so released
         * badminton-only builds keep receiving pushes; other sports are
         * suffixed ("country_US_pickleball"). Mirrors iOS and the server.
         */
        fun sportCountryTopicName(countryCode: String, sport: SportType) =
            if (sport == SportType.BADMINTON) countryTopicName(countryCode)
            else "country_${countryCode}_${sport.rawValue}"

        /** Badminton keeps "region_US_78641"; other sports are suffixed. */
        fun sportRegionTopicName(countryCode: String, postalCode: String, sport: SportType) =
            if (sport == SportType.BADMINTON) topicName(countryCode, postalCode)
            else "region_${countryCode}_${postalCode}_${sport.rawValue}"
    }

    // ── Channel setup ─────────────────────────────────────────────────────────

    fun createNotificationChannels() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val nm = context.getSystemService(NotificationManager::class.java)

        nm.createNotificationChannel(NotificationChannel(
            CHANNEL_ID_GENERAL,
            "General",
            NotificationManager.IMPORTANCE_DEFAULT,
        ).apply { description = "TournMate general notifications" })

        nm.createNotificationChannel(NotificationChannel(
            CHANNEL_ID_SESSIONS,
            "Open Play",
            NotificationManager.IMPORTANCE_HIGH,
        ).apply { description = "New open play sessions in your area" })

        nm.createNotificationChannel(NotificationChannel(
            CHANNEL_ID_MATCHES,
            "Matches",
            NotificationManager.IMPORTANCE_DEFAULT,
        ).apply { description = "Match results and score updates" })
    }

    // ── Permission ────────────────────────────────────────────────────────────

    fun hasNotificationPermission(): Boolean {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return true
        return ContextCompat.checkSelfPermission(
            context, Manifest.permission.POST_NOTIFICATIONS
        ) == PackageManager.PERMISSION_GRANTED
    }

    // ── Token sync ────────────────────────────────────────────────────────────

    /** Persists the FCM token to the player's Firestore document. */
    suspend fun syncToken(token: String) = withContext(Dispatchers.IO) {
        val playerId = currentUserStore.currentPlayerId() ?: return@withContext
        val player = playerRepo.findPlayerById(playerId) ?: return@withContext
        if (player.fcmToken == token) return@withContext
        val updated = player.copy(fcmToken = token)
        try { playerRepo.updatePlayer(updated) } catch (_: Exception) {}
    }

    // ── Topic subscriptions ───────────────────────────────────────────────────

    /** Subscribes to home-region, country, sport-scoped, and personal topics. */
    fun subscribeToHomeRegion(player: Player) {
        val cc = RegionNormalizer.normalizeCountryCode(player.homeCountryCode) ?: return
        val sport = player.preferredSport

        // Personal topic — server uses this to exclude creator from broadcasts
        player.firebaseUid?.takeIf { it.isNotEmpty() }?.let { uid ->
            messaging.subscribeToTopic("user_$uid")
        }

        // Legacy topics are badminton's; drop them when another sport is selected.
        if (sport != SportType.BADMINTON) {
            unsubscribeFromRegion(cc, player.homePostalCode ?: "")
        }

        // Sport-scoped country topic (badminton = legacy "country_US")
        messaging.subscribeToTopic(sportCountryTopicName(cc, sport))

        // Sport-scoped ZIP-level topic (badminton = legacy "region_US_78641")
        RegionNormalizer.normalizePostal(player.homePostalCode, cc)
            ?.takeIf { it.isNotEmpty() }
            ?.let { postal ->
                messaging.subscribeToTopic(sportRegionTopicName(cc, postal, sport))
            }
    }

    /** Unsubscribes from the legacy region/country topics, plus the given sport's topics. */
    fun unsubscribeFromRegion(countryCode: String, postalCode: String, sport: SportType? = null) {
        val cc = RegionNormalizer.normalizeCountryCode(countryCode) ?: return
        messaging.unsubscribeFromTopic(countryTopicName(cc))
        sport?.let { messaging.unsubscribeFromTopic(sportCountryTopicName(cc, it)) }

        val postal = RegionNormalizer.normalizePostal(postalCode, cc)?.takeIf { it.isNotEmpty() }
            ?: return
        messaging.unsubscribeFromTopic(topicName(cc, postal))
        sport?.let { messaging.unsubscribeFromTopic(sportRegionTopicName(cc, postal, it)) }
    }

    /** Swaps sport-scoped topics when the user changes preferred sport. */
    fun switchSportTopics(player: Player, from: SportType, to: SportType) {
        val cc = RegionNormalizer.normalizeCountryCode(player.homeCountryCode) ?: return
        messaging.unsubscribeFromTopic(sportCountryTopicName(cc, from))
        messaging.subscribeToTopic(sportCountryTopicName(cc, to))

        RegionNormalizer.normalizePostal(player.homePostalCode, cc)
            ?.takeIf { it.isNotEmpty() }
            ?.let { postal ->
                messaging.unsubscribeFromTopic(sportRegionTopicName(cc, postal, from))
                messaging.subscribeToTopic(sportRegionTopicName(cc, postal, to))
            }
    }
}
