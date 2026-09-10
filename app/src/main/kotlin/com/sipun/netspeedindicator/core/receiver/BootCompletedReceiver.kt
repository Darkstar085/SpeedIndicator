package com.sipun.netspeedindicator.core.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.content.ContextCompat
import com.sipun.netspeedindicator.core.service.SpeedMonitorService

class BootCompletedReceiver : BroadcastReceiver() {

    companion object {
        private const val TAG = "BootCompletedReceiver"
    }

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_BOOT_COMPLETED &&
            intent.action != "android.intent.action.QUICKBOOT_POWERON"
        ) {
            return
        }

        // Android 15+ forbids BOOT_COMPLETED receivers from launching dataSync
        // foreground services. Starting it here would throw
        // ForegroundServiceStartNotAllowedException on affected devices.
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.VANILLA_ICE_CREAM) {
            Log.i(TAG, "Skipping automatic monitoring start after boot on Android 15+")
            return
        }

        ContextCompat.startForegroundService(
            context,
            Intent(context, SpeedMonitorService::class.java)
        )
    }
}
