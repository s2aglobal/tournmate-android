package com.s2aglobal.tournmate

import android.app.Application
import android.content.Context
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
        EventRateLimiter.init(this)
        notificationService.createNotificationChannels()
    }
}
