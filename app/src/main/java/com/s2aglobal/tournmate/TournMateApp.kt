package com.s2aglobal.tournmate

import android.app.Application
import android.content.Context
import com.s2aglobal.tournmate.service.validation.EventRateLimiter
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class TournMateApp : Application() {

    companion object {
        lateinit var appContext: Context
            private set
    }

    override fun onCreate() {
        super.onCreate()
        appContext = applicationContext
        EventRateLimiter.init(this)
    }
}
