package com.sipun.netspeedindicator.core.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.content.ContextCompat
import com.sipun.netspeedindicator.core.service.SpeedMonitorService

class BootCompletedReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED ||
            intent.action == "android.intent.action.QUICKBOOT_POWERON"
        ) {
            ContextCompat.startForegroundService(
                context,
                Intent(context, SpeedMonitorService::class.java)
            )
        }
    }
}