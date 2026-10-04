package com.s2aglobal.tournmate

import android.app.Activity
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import androidx.browser.customtabs.CustomTabsIntent

/**
 * Health Connect entry point for "why does this app need this data?":
 * `androidx.health.ACTION_SHOW_PERMISSIONS_RATIONALE` (Android 13 and below) and
 * `VIEW_PERMISSION_USAGE` (Android 14+, via the manifest activity-alias).
 * Health Connect requires it to show the app's privacy policy.
 */
class HealthPermissionsRationaleActivity : Activity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        val uri = Uri.parse(PRIVACY_POLICY_URL)
        runCatching { CustomTabsIntent.Builder().build().launchUrl(this, uri) }
            .recoverCatching { startActivity(Intent(Intent.ACTION_VIEW, uri)) }
        finish()
    }

    private companion object {
        const val PRIVACY_POLICY_URL = "https://www.tournmate.com/privacy"
    }
}
