package com.s2aglobal.tournmate

import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.lifecycleScope
import com.s2aglobal.tournmate.data.local.CurrentUserStore
import com.s2aglobal.tournmate.service.AnalyticsService
import com.s2aglobal.tournmate.service.config.AppVersionGate
import com.s2aglobal.tournmate.service.notification.LocalNotificationStore
import com.s2aglobal.tournmate.ui.navigation.DeepLinkParser
import com.s2aglobal.tournmate.ui.navigation.TournMateNavHost
import com.s2aglobal.tournmate.ui.screen.update.UPDATE_FALLBACK_URL
import com.s2aglobal.tournmate.ui.screen.update.UpdateRequiredScreen
import com.s2aglobal.tournmate.ui.theme.CurrentSport
import com.s2aglobal.tournmate.ui.theme.TournMateTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject lateinit var localNotificationStore: LocalNotificationStore
    @Inject lateinit var versionGate: AppVersionGate
    @Inject lateinit var analytics: AnalyticsService
    @Inject lateinit var currentUserStore: CurrentUserStore

    private val pendingDeepLink = mutableStateOf<DeepLinkParser.Target?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        // Resolve the sport before first frame so the accent never flashes the default.
        CurrentSport.sport = runBlocking { currentUserStore.preferredSportFlow.first() }
        lifecycleScope.launch {
            currentUserStore.preferredSportFlow.collect { CurrentSport.sport = it }
        }
        // QA: launch with `--ez simulateUpdateRequired true` to preview the update blocker (debug only).
        if (BuildConfig.DEBUG && intent?.getBooleanExtra(EXTRA_SIMULATE_UPDATE_REQUIRED, false) == true) {
            versionGate.simulateUpdateRequired()
        }
        persistNotificationFromIntent(intent)
        pendingDeepLink.value = DeepLinkParser.parse(intent?.data)
            ?: DeepLinkParser.parseExtras(intent?.extras)
        setContent {
            TournMateTheme {
                val gate by versionGate.state.collectAsStateWithLifecycle()
                Box(Modifier.fillMaxSize()) {
                    // The blocker replaces the whole app (including sign-in) so no sheet or
                    // dialog window can sit above it.
                    if (gate.isUpdateRequired) {
                        UpdateRequiredScreen(policy = gate.policy, onUpdate = ::openStore)
                    } else {
                        TournMateNavHost(
                            pendingDeepLink = pendingDeepLink.value,
                            onDeepLinkConsumed = { pendingDeepLink.value = null },
                        )
                    }
                }
            }
        }
    }

    override fun onStart() {
        super.onStart()
        // Checked at launch and whenever the app returns to the foreground.
        lifecycleScope.launch { versionGate.check() }
    }

    private fun openStore(url: String) {
        analytics.log(AnalyticsService.EventName.UPDATE_TAPPED)
        val opened = runCatching { startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(url))) }.isSuccess
        if (!opened && url != UPDATE_FALLBACK_URL) {
            runCatching { startActivity(Intent(Intent.ACTION_VIEW, Uri.parse(UPDATE_FALLBACK_URL))) }
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
     *
     * Checks both custom keys (set by TournMateFcmService foreground path) and
     * FCM data payload keys (present when the system handles background notifications).
     */
    private fun persistNotificationFromIntent(intent: Intent?) {
        val extras = intent?.extras ?: return

        val title = extras.getString("notificationTitle")?.takeIf { it.isNotBlank() }
            ?: extras.getString("gcm.notification.title")?.takeIf { it.isNotBlank() }
            ?: extras.getString("title")?.takeIf { it.isNotBlank() }
            ?: return
        val body = extras.getString("notificationBody")
            ?: extras.getString("gcm.notification.body")
            ?: extras.getString("body")
            ?: ""
        val type = extras.getString("notificationType")
            ?: extras.getString("type")
            ?: ""
        val tournamentId = extras.getString("tournamentId")
        val sessionId = extras.getString("sessionId")
        val createdBy = extras.getString("createdBy")

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
            createdBy = createdBy,
        )
    }

    companion object {
        const val EXTRA_SIMULATE_UPDATE_REQUIRED = "simulateUpdateRequired"
    }
}
