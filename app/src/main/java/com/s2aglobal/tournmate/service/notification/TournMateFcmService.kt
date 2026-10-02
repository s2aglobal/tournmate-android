package com.s2aglobal.tournmate.service.notification

import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Intent
import androidx.core.app.NotificationCompat
import com.google.firebase.messaging.FirebaseMessagingService
import com.google.firebase.messaging.RemoteMessage
import com.s2aglobal.tournmate.MainActivity
import com.s2aglobal.tournmate.R
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Firebase Cloud Messaging service.
 *
 * - `onNewToken`       — persists the new FCM token to the player's Firestore doc
 * - `onMessageReceived` — shows a local notification and saves it to
 *                          [LocalNotificationStore] so the inbox displays it
 *
 * Dev vs prod routing is automatic: the google-services.json chosen by the
 * `dev` / `prod` build flavor determines which Firebase project receives the
 * token and topic subscriptions — no extra code needed.
 */
@AndroidEntryPoint
class TournMateFcmService : FirebaseMessagingService() {

    @Inject lateinit var notificationService: NotificationService
    @Inject lateinit var localStore: LocalNotificationStore

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    // ── Token refresh ─────────────────────────────────────────────────────────

    override fun onNewToken(token: String) {
        super.onNewToken(token)
        serviceScope.launch {
            notificationService.syncToken(token)
        }
    }

    // ── Incoming message ──────────────────────────────────────────────────────

    override fun onMessageReceived(message: RemoteMessage) {
        super.onMessageReceived(message)

        val title = message.notification?.title
            ?: message.data["title"]
            ?: return
        val body = message.notification?.body
            ?: message.data["body"]
            ?: ""

        val type         = message.data["type"] ?: ""
        val tournamentId = message.data["tournamentId"]
        val sessionId    = message.data["sessionId"]
        val createdBy    = message.data["createdBy"]

        // Persist locally for the inbox
        localStore.save(
            type = type,
            title = title,
            body = body,
            tournamentId = tournamentId,
            sessionId = sessionId,
            createdBy = createdBy,
        )

        // Build an intent that deep-links when the notification is tapped
        val deepLinkIntent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP or Intent.FLAG_ACTIVITY_CLEAR_TOP
            putExtra("notificationTitle", title)
            putExtra("notificationBody", body)
            putExtra("notificationType", type)
            tournamentId?.let { putExtra("tournamentId", it) }
            sessionId?.let    { putExtra("sessionId", it) }
            createdBy?.let    { putExtra("createdBy", it) }
        }

        val pendingIntent = PendingIntent.getActivity(
            this,
            System.currentTimeMillis().toInt(),
            deepLinkIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        // Choose channel based on type
        val channelId = when {
            type.contains("session", ignoreCase = true) -> NotificationService.CHANNEL_ID_SESSIONS
            type.contains("score",   ignoreCase = true) ||
            type.contains("match",   ignoreCase = true) -> NotificationService.CHANNEL_ID_MATCHES
            else -> NotificationService.CHANNEL_ID_GENERAL
        }

        val notification = NotificationCompat.Builder(this, channelId)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle(title)
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .setAutoCancel(true)
            .setContentIntent(pendingIntent)
            .build()

        val nm = getSystemService(NotificationManager::class.java)
        nm.notify(System.currentTimeMillis().toInt(), notification)
    }
}
