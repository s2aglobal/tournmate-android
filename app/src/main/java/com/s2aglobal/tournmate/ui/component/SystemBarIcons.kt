package com.s2aglobal.tournmate.ui.component

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.view.View
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.ui.platform.LocalView
import androidx.core.view.WindowCompat

/**
 * Light (white) status/navigation bar icons while this screen is shown — for
 * the dark full-bleed screens (splash, welcome, onboarding, update blocker).
 * The app default is dark icons (light-only UI); restored on dispose.
 * iOS gets this automatically from `.preferredColorScheme(.dark)` / dark backgrounds.
 */
@Composable
fun LightSystemBarIcons() {
    val view = LocalView.current
    DisposableEffect(view) {
        lightIconRequests += 1
        applyIcons(view)
        onDispose {
            lightIconRequests -= 1
            applyIcons(view)
        }
    }
}

// Ref-counted so dark→dark transitions (splash → welcome) don't flip the
// icons back when the outgoing screen disposes after the incoming one starts.
private var lightIconRequests = 0

private fun applyIcons(view: View) {
    val window = view.context.findActivity()?.window ?: return
    val dark = lightIconRequests == 0
    WindowCompat.getInsetsController(window, view).apply {
        isAppearanceLightStatusBars = dark
        isAppearanceLightNavigationBars = dark
    }
}

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}
