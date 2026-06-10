package com.s2aglobal.tournmate.util

import android.content.Context
import android.net.Uri
import androidx.browser.customtabs.CustomTabsIntent

fun openInBrowser(context: Context, url: String) {
    val customTabsIntent = CustomTabsIntent.Builder()
        .setShowTitle(true)
        .build()
    customTabsIntent.launchUrl(context, Uri.parse(url))
}
