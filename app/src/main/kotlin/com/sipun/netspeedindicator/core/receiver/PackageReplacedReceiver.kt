package com.sipun.netspeedindicator.core.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.sipun.netspeedindicator.core.service.NetworkMonitorScheduler

/**
 * When the app is updated, any running foreground service is killed. If the
 * user had monitoring enabled, schedule the persisted recovery job so
 * monitoring resumes on the next network availability without manual action.
 */
class PackageReplacedReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action != Intent.ACTION_MY_PACKAGE_REPLACED) return
        NetworkMonitorScheduler.schedule(context)
    }
}
