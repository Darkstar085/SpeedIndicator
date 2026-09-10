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
import com.sipun.netspeedindicator.R
import com.sipun.netspeedindicator.presentation.MainActivity

object NotificationHelper {
    const val CHANNEL_ID = "speed_monitor_channel_v5"
    const val NOTIFICATION_ID = 1001
    private var cachedTypeface: Typeface? = null

    private fun getStatusTypeface(): Typeface = cachedTypeface ?: Typeface.create("sans-serif-condensed", Typeface.BOLD).also { cachedTypeface = it }

    fun createNotificationChannel(context: Context) {
        val channel = NotificationChannel(
            CHANNEL_ID,
            context.getString(R.string.notification_channel_name),
            NotificationManager.IMPORTANCE_DEFAULT
        ).apply {
            description = context.getString(R.string.notification_channel_description)
            setShowBadge(false)
            enableVibration(false)
            setSound(null, null)
        }
        (context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager).createNotificationChannel(channel)
    }

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
        val intent = Intent(context, MainActivity::class.java).apply { flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK }
        val pendingIntent = PendingIntent.getActivity(context, 0, intent, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
        val titleText = if (uploadSpeed != null) {
            context.getString(R.string.notification_down_up, downloadSpeed, uploadSpeed)
        } else {
            context.getString(R.string.notification_speed, totalSpeed)
        }.let { baseTitle ->
            if (signal.isNotEmpty()) baseTitle + context.getString(R.string.notification_signal, signal) else baseTitle
        }
        return NotificationCompat.Builder(context, CHANNEL_ID)
            .setContentTitle(titleText)
            .setContentText(context.getString(R.string.notification_usage, mobileUsage, wifiUsage))
            .setOngoing(true).setOnlyAlertOnce(true).setSilent(true)
            .setContentIntent(pendingIntent)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .setSmallIcon(createStatusIcon(speedValue ?: "0", speedUnit ?: "KB/s"))
            .build()
    }

    private fun createStatusIcon(value: String, unit: String): IconCompat {
        val size = 96
        val bitmap = createBitmap(size, size)
        val canvas = Canvas(bitmap)
        val valuePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { typeface = getStatusTypeface(); color = Color.WHITE; textAlign = Paint.Align.CENTER; textSize = if (value.length >= 3 && !value.contains(".")) 62f else 72f; style = Paint.Style.FILL }
        val unitPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply { typeface = getStatusTypeface(); color = Color.WHITE; textAlign = Paint.Align.CENTER; textSize = 42f; style = Paint.Style.FILL }
        val vBounds = Rect(); valuePaint.getTextBounds(value, 0, value.length, vBounds)
        val uBounds = Rect(); unitPaint.getTextBounds(unit, 0, unit.length, uBounds)
        val totalHeight = vBounds.height() + uBounds.height() + 4f
        val centerX = size / 2f
        val startY = (size - totalHeight) / 2f
        val valueBaseline = startY - vBounds.top
        canvas.drawText(value, centerX, valueBaseline, valuePaint)
        val unitBaseline = valueBaseline + vBounds.bottom + 4f - uBounds.top
        canvas.drawText(unit, centerX, unitBaseline, unitPaint)
        return IconCompat.createWithBitmap(bitmap)
    }
}
