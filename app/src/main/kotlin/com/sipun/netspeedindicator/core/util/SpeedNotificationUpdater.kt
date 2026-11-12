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
    private var enabled = false
    private var lastUpdateKey: String? = null

    private val tilePreferences by lazy {
        context.getSharedPreferences(TILE_PREFS_NAME, Context.MODE_PRIVATE)
    }

    companion object {
        /** Shared with the Quick Settings tile service for the last speed reading. */
        const val TILE_PREFS_NAME = "net_speed_quick_tile"
        const val KEY_TILE_SNAPSHOT = "snapshot"

        /** Pure change-detection key for the notification/tile snapshot. */
        internal fun updateKeyOf(
            downloadSpeed: String,
            uploadSpeed: String?,
            totalSpeed: String,
            mobileUsage: String,
            wifiUsage: String,
            speedValue: String,
            speedUnit: String,
            showOnLockScreen: Boolean
        ): String = listOf(
            downloadSpeed,
            uploadSpeed,
            totalSpeed,
            mobileUsage,
            wifiUsage,
            speedValue,
            speedUnit,
            showOnLockScreen
        ).joinToString("|")
    }

    @Synchronized
    fun start() {
        enabled = true
    }

    @Synchronized
    fun stop() {
        enabled = false
        lastUpdateKey = null
        tilePreferences.edit().remove(KEY_TILE_SNAPSHOT).apply()
        notificationManager.cancel(NotificationHelper.NOTIFICATION_ID)
    }

    @Synchronized
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
        if (!enabled) return
        // Only repost when something actually changed: the controller calls us
        // every second, and most values are stable most of the time.
        val key = updateKeyOf(
            downloadSpeed,
            uploadSpeed,
            totalSpeed,
            mobileUsage,
            wifiUsage,
            speedValue,
            speedUnit,
            showOnLockScreen
        )
        if (key == lastUpdateKey) return
        lastUpdateKey = key

        tilePreferences.edit()
            .putString(KEY_TILE_SNAPSHOT, "$speedValue $speedUnit")
            .apply()

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
