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
        pendingDeepLink.value = DeepLinkParser.parse(intent?.data)
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
        DeepLinkParser.parse(intent.data)?.let { pendingDeepLink.value = it }
    }
}
