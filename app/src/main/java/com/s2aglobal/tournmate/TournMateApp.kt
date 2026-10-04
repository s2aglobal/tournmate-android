package com.s2aglobal.tournmate

import android.app.Application
import android.content.Context
import com.google.firebase.crashlytics.FirebaseCrashlytics
import com.s2aglobal.tournmate.service.notification.NotificationService
import com.s2aglobal.tournmate.service.validation.EventRateLimiter
import dagger.hilt.android.HiltAndroidApp
import javax.inject.Inject

@HiltAndroidApp
class TournMateApp : Application() {

    @Inject lateinit var notificationService: NotificationService

    companion object {
        lateinit var appContext: Context
            private set
    }

    override fun onCreate() {
        super.onCreate()
        appContext = applicationContext
        // Crash reports only from shipped builds, not local debug runs (iOS parity).
        FirebaseCrashlytics.getInstance().isCrashlyticsCollectionEnabled = !BuildConfig.DEBUG
        EventRateLimiter.init(this)
        notificationService.createNotificationChannels()
    }
}
