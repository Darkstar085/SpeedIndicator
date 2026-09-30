package com.sipun.netspeedindicator.core.util

import android.app.Notification
import android.app.NotificationManager
import android.content.Context
import androidx.core.content.getSystemService
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Owns dynamic foreground-notification updates for the monitoring controller.
 */
@Singleton
class SpeedNotificationUpdater @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val notificationManager: NotificationManager =
        context.getSystemService() ?: error("NotificationManager unavailable")

    fun update(
        downloadSpeed: String,
        uploadSpeed: String?,
        totalSpeed: String,
        mobileUsage: String,
        wifiUsage: String,
        speedValue: String,
        speedUnit: String,
        showOnLockScreen: Boolean
    ) {
        notificationManager.notify(
            NotificationHelper.NOTIFICATION_ID,
            NotificationHelper.buildNotification(
                context = context,
                downloadSpeed = downloadSpeed,
                uploadSpeed = uploadSpeed,
                totalSpeed = totalSpeed,
                mobileUsage = mobileUsage,
                wifiUsage = wifiUsage,
                speedValue = speedValue,
                speedUnit = speedUnit
            ).apply {
                visibility = if (showOnLockScreen) {
                    Notification.VISIBILITY_PUBLIC
                } else {
                    Notification.VISIBILITY_SECRET
                }
            }
        )
    }
}
