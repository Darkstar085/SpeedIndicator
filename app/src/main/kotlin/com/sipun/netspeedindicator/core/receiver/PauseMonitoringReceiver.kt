package com.sipun.netspeedindicator.core.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.sipun.netspeedindicator.core.service.NetworkMonitorScheduler
import com.sipun.netspeedindicator.core.service.SpeedMonitorService
import com.sipun.netspeedindicator.core.util.NotificationHelper
import com.sipun.netspeedindicator.data.preferences.PreferenceManager
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Pauses background speed monitoring from the notification action.
 * Mirrors the home-screen stop flow: persists the flag first so the
 * service's cold-start check observes it, then stops the service.
 */
@AndroidEntryPoint
class PauseMonitoringReceiver : BroadcastReceiver() {
    @Inject
    lateinit var preferenceManager: PreferenceManager

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Default)

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != NotificationHelper.ACTION_PAUSE_MONITORING) return
        val pendingResult = goAsync()
        val appContext = context.applicationContext
        scope.launch {
            try {
                preferenceManager.setMonitoringEnabled(false)
                NetworkMonitorScheduler.cancel(appContext)
                appContext.stopService(Intent(appContext, SpeedMonitorService::class.java))
            } finally {
                pendingResult.finish()
            }
        }
    }
}
