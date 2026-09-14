package com.sipun.netspeedindicator.core.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat
import com.sipun.netspeedindicator.core.service.NetworkMonitorScheduler
import com.sipun.netspeedindicator.core.service.SpeedMonitorService
import com.sipun.netspeedindicator.data.preferences.PreferenceManager

class BootCompletedReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED && intent.action != "android.intent.action.QUICKBOOT_POWERON") return
        val preferences = context.getSharedPreferences("app_preferences", Context.MODE_PRIVATE)
        if (!preferences.getBoolean(PreferenceManager.KEY_MONITORING_ENABLED, true)) {
            NetworkMonitorScheduler.cancel(context)
            return
        }
        NetworkMonitorScheduler.schedule(context)
        ContextCompat.startForegroundService(context, Intent(context, SpeedMonitorService::class.java))
    }
}
