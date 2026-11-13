package com.sipun.netspeedindicator

import android.app.Application
import androidx.work.Configuration
import androidx.work.WorkManager
import com.sipun.netspeedindicator.core.update.UpdateManager
import com.sipun.netspeedindicator.core.update.UpdateNotificationHelper
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class NetSpeedApplication : Application(), Configuration.Provider {
    override val workManagerConfiguration: Configuration
        get() = Configuration.Builder().build()

    override fun onCreate() {
        super.onCreate()
        UpdateNotificationHelper.createChannel(this)
        UpdateManager.enqueuePeriodicCheck(this)
        try {
            WorkManager.getInstance(this).cancelUniqueWork("daily_usage_alert")
        } catch (_: Exception) {}
    }
}
