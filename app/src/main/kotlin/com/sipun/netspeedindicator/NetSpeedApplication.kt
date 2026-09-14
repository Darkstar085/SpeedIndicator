package com.sipun.netspeedindicator

import android.app.Application
import com.sipun.netspeedindicator.core.update.UpdateManager
import com.sipun.netspeedindicator.core.update.UpdateNotificationHelper
import dagger.hilt.android.HiltAndroidApp

@HiltAndroidApp
class NetSpeedApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        UpdateNotificationHelper.createChannel(this)
        UpdateManager.enqueuePeriodicCheck(this)
    }
}
