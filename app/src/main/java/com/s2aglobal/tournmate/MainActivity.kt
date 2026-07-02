package com.s2aglobal.tournmate

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.mutableStateOf
import com.s2aglobal.tournmate.service.notification.LocalNotificationStore
import com.s2aglobal.tournmate.ui.navigation.DeepLinkParser
import com.s2aglobal.tournmate.ui.navigation.TournMateNavHost
import com.s2aglobal.tournmate.ui.theme.TournMateTheme
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject lateinit var localNotificationStore: LocalNotificationStore

    private val pendingDeepLink = mutableStateOf<DeepLinkParser.Target?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        persistNotificationFromIntent(intent)
        pendingDeepLink.value = DeepLinkParser.parse(intent?.data)
            ?: DeepLinkParser.parseExtras(intent?.extras)
        setContent {
            TournMateTheme {
                TournMateNavHost(
                    pendingDeepLink = pendingDeepLink.value,
                    onDeepLinkConsumed = { pendingDeepLink.value = null },
                )
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        persistNotificationFromIntent(intent)
        val target = DeepLinkParser.parse(intent.data)
            ?: DeepLinkParser.parseExtras(intent.extras)
        target?.let { pendingDeepLink.value = it }
    }

    /**
     * When a push arrives while the app is in the background, Android shows the
     * system notification without calling [TournMateFcmService.onMessageReceived].
     * Persist the payload when the user taps so it appears in the inbox (iOS parity).
     */
    private fun persistNotificationFromIntent(intent: Intent?) {
        val extras = intent?.extras ?: return
        val title = extras.getString("notificationTitle")?.takeIf { it.isNotBlank() } ?: return
        val body = extras.getString("notificationBody") ?: ""
        val type = extras.getString("notificationType") ?: ""
        val tournamentId = extras.getString("tournamentId")
        val sessionId = extras.getString("sessionId")

        val alreadySaved = localNotificationStore.loadAll().any { entry ->
            entry.title == title &&
                entry.body == body &&
                entry.tournamentId == tournamentId &&
                entry.sessionId == sessionId
        }
        if (alreadySaved) return

        localNotificationStore.save(
            type = type,
            title = title,
            body = body,
            tournamentId = tournamentId,
            sessionId = sessionId,
            createdBy = extras.getString("createdBy"),
        )
    }
}
