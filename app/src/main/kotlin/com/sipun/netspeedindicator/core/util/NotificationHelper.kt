package com.sipun.netspeedindicator.core.util

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import com.sipun.netspeedindicator.MainActivity
import com.sipun.netspeedindicator.R
import com.sipun.netspeedindicator.core.receiver.PauseMonitoringReceiver

/**
 * Helper class for creating and managing notifications
 * Handles notification channel creation and notification building
 */
object NotificationHelper {

    const val CHANNEL_ID = "speed_monitor_channel_v5"
    const val CHANNEL_NAME = "Speed Monitor"
    const val NOTIFICATION_ID = 1001
    const val ACTION_PAUSE_MONITORING = "com.sipun.netspeedindicator.PAUSE_MONITORING"

    /**
     * Create notification channel (required for Android O+)
     */
    fun createNotificationChannel(context: Context) {
        val channel = NotificationChannel(
            CHANNEL_ID,
            CHANNEL_NAME,
            NotificationManager.IMPORTANCE_DEFAULT // Minimized/Silent list
        ).apply {
            description = "Shows real-time internet speed"
            setShowBadge(false)
            enableVibration(false)
            setSound(null, null)
        }

        val notificationManager =
            context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.createNotificationChannel(channel)
    }

    /**
     * Build notification for speed monitoring
     * @param downloadSpeed Current download speed text (e.g., "29 KB/s")
     * @param uploadSpeed Current upload speed text (e.g. "10 KB/s") - Optional
     * @param mobileUsage Mobile data usage text (e.g., "57.7 MB")
     * @param wifiUsage WiFi data usage text (e.g., "1.35 GB")
     * @param speedValue Speed value string (e.g., "1.5") - for the status-bar pill text
     * @param speedUnit Speed unit string (e.g., "MB/s") - for the status-bar pill text
     */
    fun buildNotification(
        context: Context,
        downloadSpeed: String,
        uploadSpeed: String? = null,
        totalSpeed: String,
        mobileUsage: String,
        wifiUsage: String,
        speedValue: String? = null,
        speedUnit: String? = null
    ): Notification {
        val intent = Intent(context, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            intent,
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val titleText = if (uploadSpeed != null) {
            "Down: $downloadSpeed   Up: $uploadSpeed"
        } else {
            "Speed: $totalSpeed"
        }

        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setContentTitle(titleText)
            .setContentText("Mobile: $mobileUsage | Wi-Fi: $wifiUsage")
            .setShowWhen(false)
            .setOngoing(true) // Cannot be dismissed
            .setOnlyAlertOnce(true) // No sound/vibration on updates
            .setSilent(true)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setRequestPromotedOngoing(true)
            .setShortCriticalText("${speedValue ?: "0"}${speedUnit ?: "KB/s"}".take(7))
            .setCategory(NotificationCompat.CATEGORY_SERVICE)

        // Static monochrome glyph: the speed itself is rendered by the system as
        // the pill's short-critical text, so the digest icon stays a fixed glyph.
        builder.setSmallIcon(R.drawable.ic_speed)

        val pausePendingIntent = PendingIntent.getBroadcast(
            context,
            1,
            Intent(context, PauseMonitoringReceiver::class.java).apply {
                action = ACTION_PAUSE_MONITORING
            },
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )
        builder.addAction(
            NotificationCompat.Action.Builder(
                R.drawable.ic_notifications_off,
                context.getString(R.string.pause_monitoring),
                pausePendingIntent
            ).build()
        )

        return builder.build()
    }
}
