package com.sipun.netspeedindicator.core.update

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import androidx.core.content.FileProvider
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
            NotificationChannel(CHANNEL_ID, CHANNEL_NAME, NotificationManager.IMPORTANCE_DEFAULT).apply {
                description = "Notifies when a new SpeedIndicator release is available"
                setShowBadge(true)
            }
        )
    }

    fun showUpdateAvailable(context: Context, update: AppUpdate) {
        createChannel(context)
        val intent = Intent(context, MainActivity::class.java).apply {
            action = UpdateManager.ACTION_DOWNLOAD_UPDATE
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
            .setContentTitle("Update available")
            .setContentText("SpeedIndicator ${update.version} is ready to download")
            .setStyle(NotificationCompat.BigTextStyle().bigText(update.notes.ifBlank { "A newer version is available." }))
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setCategory(NotificationCompat.CATEGORY_RECOMMENDATION)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()
            .also { context.getSystemService(NotificationManager::class.java).notify(AVAILABLE_ID, it) }
    }

    fun showDownloadProgress(context: Context, fileName: String) {
        createChannel(context)
        NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentTitle("Downloading update")
            .setContentText(fileName)
            .setOngoing(true)
            .setOnlyAlertOnce(true)
            .setProgress(0, 0, true)
            .setCategory(NotificationCompat.CATEGORY_PROGRESS)
            .build()
            .also { context.getSystemService(NotificationManager::class.java).notify(DOWNLOAD_ID, it) }
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
            .also { context.getSystemService(NotificationManager::class.java).notify(READY_ID, it) }
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
            .also { context.getSystemService(NotificationManager::class.java).notify(READY_ID, it) }
    }
}
