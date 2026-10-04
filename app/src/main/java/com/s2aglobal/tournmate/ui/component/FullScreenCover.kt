package com.s2aglobal.tournmate.ui.component

import android.os.Build
import android.view.WindowManager
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.window.DialogWindowProvider
import androidx.core.view.WindowCompat

/**
 * Android equivalent of SwiftUI's `.fullScreenCover`: covers the whole window,
 * including the tab bar, edge to edge. Content handles its own insets
 * (statusBarsPadding / navigationBarsPadding). System bar icons stay dark
 * because the app is light-only. Back presses go to [onDismissRequest] unless
 * the content installs its own BackHandler.
 */
@Composable
fun FullScreenCover(
    onDismissRequest: () -> Unit,
    background: Color = Color.White,
    content: @Composable () -> Unit,
) {
    Dialog(
        onDismissRequest = onDismissRequest,
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            decorFitsSystemWindows = false,
            dismissOnClickOutside = false,
        ),
    ) {
        val view = LocalView.current
        val window = (view.parent as? DialogWindowProvider)?.window
        SideEffect {
            window?.let {
                val apply = {
                    // Dialog windows stop below the status bar and ignore bar appearance
                    // unless they draw the system-bar backgrounds themselves.
                    it.clearFlags(WindowManager.LayoutParams.FLAG_DIM_BEHIND)
                    it.addFlags(WindowManager.LayoutParams.FLAG_DRAWS_SYSTEM_BAR_BACKGROUNDS)
                    it.setLayout(WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.MATCH_PARENT)
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                        it.attributes = it.attributes.apply {
                            layoutInDisplayCutoutMode = WindowManager.LayoutParams.LAYOUT_IN_DISPLAY_CUTOUT_MODE_SHORT_EDGES
                        }
                    }
                    WindowCompat.setDecorFitsSystemWindows(it, false)
                    @Suppress("DEPRECATION")
                    it.statusBarColor = android.graphics.Color.TRANSPARENT
                    @Suppress("DEPRECATION")
                    it.navigationBarColor = android.graphics.Color.TRANSPARENT
                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                        it.isStatusBarContrastEnforced = false
                        it.isNavigationBarContrastEnforced = false
                    }
                    // Dark icons — the app is light-only.
                    WindowCompat.getInsetsController(it, it.decorView).apply {
                        isAppearanceLightStatusBars = true
                        isAppearanceLightNavigationBars = true
                    }
                }
                apply()
                // Compose re-applies the dialog's own layout params when shown; run again after.
                view.post { apply() }
            }
        }
        Box(Modifier.fillMaxSize().background(background)) { content() }
    }
}
