package com.s2aglobal.tournmate

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.compose.runtime.mutableStateOf
import com.s2aglobal.tournmate.ui.navigation.DeepLinkParser
import com.s2aglobal.tournmate.ui.navigation.TournMateNavHost
import com.s2aglobal.tournmate.ui.theme.TournMateTheme
import dagger.hilt.android.AndroidEntryPoint

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    private val pendingDeepLink = mutableStateOf<DeepLinkParser.Target?>(null)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        // Handle both URL deep links and FCM notification tap extras
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
        val target = DeepLinkParser.parse(intent.data)
            ?: DeepLinkParser.parseExtras(intent.extras)
        target?.let { pendingDeepLink.value = it }
    }
}
