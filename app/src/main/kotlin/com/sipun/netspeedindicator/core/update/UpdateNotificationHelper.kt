package com.sipun.netspeedindicator.core.update

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationCompat.ProgressStyle
import com.sipun.netspeedindicator.MainActivity
import com.sipun.netspeedindicator.R
import java.io.File

object UpdateNotificationHelper {
    private const val CHANNEL_ID = "app_updates"
    private const val CHANNEL_NAME = "App Updates"
    private const val AVAILABLE_ID = 2001
    private const val DOWNLOAD_ID = 2002
    private const val READY_ID = 2003

    fun createChannel(context: Context) {
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        manager.createNotificationChannel(
            NotificationChannel(
                CHANNEL_ID,
                CHANNEL_NAME,
                NotificationManager.IMPORTANCE_DEFAULT
            ).apply {
                description = "Notifies about SpeedIndicator updates and downloads"
                setShowBadge(true)
            }
        )
    }

    fun showUpdateAvailable(context: Context, update: AppUpdate) {
        createChannel(context)
        val intent = Intent(context, MainActivity::class.java).apply {
            action = UpdateManager.ACTION_SHOW_UPDATE
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            AVAILABLE_ID,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("New update available")
            .setContentText("Version " + update.version + " is ready to download")
            .addAction(
                R.drawable.ic_launcher_foreground,
                "What's new",
                pendingIntent
            )
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setCategory(NotificationCompat.CATEGORY_RECOMMENDATION)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()
            .also {
                context.getSystemService(NotificationManager::class.java)
                    .notify(AVAILABLE_ID, it)
            }
    }

    fun showDownloadProgress(
        context: Context,
        fileName: String,
        downloadedBytes: Long,
        totalBytes: Long,
        percent: Int
    ) {
        createChannel(context)
        val builder = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("Downloading update")
            .setContentText(fileName + " • " + percent + "%")
            .setSubText(
                formatBytes(downloadedBytes) +
                    if (totalBytes > 0) " / " + formatBytes(totalBytes) else ""
            )
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setCategory(NotificationCompat.CATEGORY_PROGRESS)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setRequestPromotedOngoing(Build.VERSION.SDK_INT >= 36)

        if (Build.VERSION.SDK_INT >= 36) {
            builder.setStyle(
                ProgressStyle()
                    .setProgress(percent.coerceIn(0, 100))
                    .setStyledByProgress(true)
            )
        } else {
            builder.setProgress(100, percent.coerceIn(0, 100), totalBytes <= 0)
        }

        context.getSystemService(NotificationManager::class.java)
            .notify(DOWNLOAD_ID, builder.build())
    }

    fun showUpdateReady(context: Context, tag: String, apk: File) {
        createChannel(context)
        context.getSystemService(NotificationManager::class.java).cancel(DOWNLOAD_ID)
        val intent = Intent(context, MainActivity::class.java).apply {
            action = UpdateManager.ACTION_INSTALL_UPDATE
            putExtra("update_tag", tag)
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            context,
            READY_ID,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("Update downloaded")
            .setContentText("Tap to install the new SpeedIndicator release")
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setCategory(NotificationCompat.CATEGORY_STATUS)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()
            .also {
                context.getSystemService(NotificationManager::class.java)
                    .notify(READY_ID, it)
            }
    }

    fun showDownloadFailed(context: Context) {
        createChannel(context)
        context.getSystemService(NotificationManager::class.java).cancel(DOWNLOAD_ID)
        NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("Update download failed")
            .setContentText("Please try downloading the update again.")
            .setAutoCancel(true)
            .setCategory(NotificationCompat.CATEGORY_ERROR)
            .build()
            .also {
                context.getSystemService(NotificationManager::class.java)
                    .notify(READY_ID, it)
            }
    }

    private fun formatBytes(value: Long): String {
        if (value < 1024L) return value.toString() + " B"
        val units = arrayOf("KB", "MB", "GB")
        var size = value.toDouble()
        var unit = 0
        while (size >= 1024.0 && unit < units.lastIndex) {
            size /= 1024.0
            unit++
        }
        return "%.1f %s".format(size, units[unit])
    }
}
