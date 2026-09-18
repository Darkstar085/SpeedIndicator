package com.sipun.netspeedindicator.core.util

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Rect
import android.graphics.Typeface
import androidx.core.app.NotificationCompat
import androidx.core.graphics.createBitmap
import androidx.core.graphics.drawable.IconCompat
import com.sipun.netspeedindicator.MainActivity

/**
 * Helper class for creating and managing notifications
 * Handles notification channel creation and notification building
 */
object NotificationHelper {

    const val CHANNEL_ID = "speed_monitor_channel_v5"
    const val CHANNEL_NAME = "Speed Monitor"
    const val NOTIFICATION_ID = 1001
    private var cachedTypeface: Typeface? = null

    private fun getStatusTypeface(): Typeface {
        return cachedTypeface ?: Typeface.create("sans-serif-condensed", Typeface.BOLD)
            .also { cachedTypeface = it }
    }

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
     * @param speedValue Speed value string (e.g., "1.5") - for icon
     * @param speedUnit Speed unit string (e.g., "MB") - for icon
     */
    fun buildNotification(
        context: Context,
        downloadSpeed: String,
        uploadSpeed: String? = null,
        totalSpeed: String,
        mobileUsage: String,
        wifiUsage: String,
        signal: String,
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
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)

        val icon = createStatusIcon(
            speedValue ?: "0",
            speedUnit ?: "KB/s"
        )
        builder.setSmallIcon(icon)

        return builder.build()
    }

    private fun createStatusIcon(value: String, unit: String): IconCompat {
        val size = 96
        val bitmap = createBitmap(size, size)
        val canvas = Canvas(bitmap)

        // Android scales small notification icons down heavily, so render at a high
        // resolution and fit both width and height before drawing.
        val maxTextWidth = size * 0.94f
        val maxTextHeight = size * 0.90f

        val valuePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            this.typeface = getStatusTypeface()
            color = Color.WHITE
            textAlign = Paint.Align.CENTER
            style = Paint.Style.FILL
        }

        val unitPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
            this.typeface = getStatusTypeface()
            color = Color.WHITE
            textAlign = Paint.Align.CENTER
            style = Paint.Style.FILL
        }

        // Start large and reduce only when the actual rendered text no longer fits.
        var valueTextSize = 108f
        var unitTextSize = 48f
        while (valueTextSize >= 40f) {
            valuePaint.textSize = valueTextSize
            unitPaint.textSize = unitTextSize

            val vBounds = Rect()
            valuePaint.getTextBounds(value, 0, value.length, vBounds)
            val uBounds = Rect()
            unitPaint.getTextBounds(unit, 0, unit.length, uBounds)

            val spacing = 2f
            val totalHeight = vBounds.height() + uBounds.height() + spacing
            val fits = valuePaint.measureText(value) <= maxTextWidth &&
                unitPaint.measureText(unit) <= maxTextWidth &&
                totalHeight <= maxTextHeight

            if (fits) break
            valueTextSize -= 2f
            if (valueTextSize < 64f) {
                unitTextSize = maxOf(32f, unitTextSize - 2f)
            }
        }

        val vBounds = Rect()
        valuePaint.getTextBounds(value, 0, value.length, vBounds)
        val uBounds = Rect()
        unitPaint.getTextBounds(unit, 0, unit.length, uBounds)

        val spacing = 2f
        val totalHeight = vBounds.height() + uBounds.height() + spacing
        val centerX = size / 2f
        val startY = (size - totalHeight) / 2f

        val valueBaseline = startY - vBounds.top
        canvas.drawText(value, centerX, valueBaseline, valuePaint)

        val unitBaseline = valueBaseline + vBounds.bottom + spacing - uBounds.top
        canvas.drawText(unit, centerX, unitBaseline, unitPaint)

        return IconCompat.createWithBitmap(bitmap)
    }
}
