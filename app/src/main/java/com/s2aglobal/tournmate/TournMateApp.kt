package com.s2aglobal.tournmate

import android.app.Application
import com.s2aglobal.tournmate.service.validation.EventRateLimiter
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class TournMateApp : Application() {
    override fun onCreate() {
        super.onCreate()
        EventRateLimiter.init(this)
    }
}
