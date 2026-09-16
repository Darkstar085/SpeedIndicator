package com.sipun.netspeedindicator.core.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.sipun.netspeedindicator.core.service.NetworkMonitorScheduler

class BootCompletedReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED && intent.action != "android.intent.action.QUICKBOOT_POWERON") return
        NetworkMonitorScheduler.schedule(context)
    }
}
