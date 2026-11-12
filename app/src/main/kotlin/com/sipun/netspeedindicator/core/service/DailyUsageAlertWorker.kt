package com.sipun.netspeedindicator.core.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.os.Build
import android.util.Log
import androidx.core.app.NotificationCompat
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.sipun.netspeedindicator.MainActivity
import com.sipun.netspeedindicator.R
import com.sipun.netspeedindicator.data.datasource.UsageDataSource
import com.sipun.netspeedindicator.data.preferences.PreferenceManager
import com.sipun.netspeedindicator.data.preferences.appPreferencesDataStore
import kotlinx.coroutines.flow.first
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import java.util.concurrent.TimeUnit

/**
 * Periodically checks today's mobile data usage against the user's budget and
 * posts at most one alert per day. No-ops unless the alert is enabled in
 * Settings. Reads preferences straight through the shared DataStore delegate
 * (the same singleton the Hilt [PreferenceManager] uses) to avoid creating a
 * second DataStore instance.
 */
class DailyUsageAlertWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {

    companion object {
        private const val TAG = "DailyUsageAlertWorker"
        private const val WORK_NAME = "daily_usage_alert"
        private const val CHANNEL_ID = "usage_alerts_channel"
        private const val NOTIFICATION_ID = 4101

        /** Enqueue the periodic alert. Cheap to call on every app start. */
        fun enqueue(context: Context) {
            try {
                val request = PeriodicWorkRequestBuilder<DailyUsageAlertWorker>(
                    6,
                    TimeUnit.HOURS
                ).build()
                WorkManager.getInstance(context).enqueueUniquePeriodicWork(
                    WORK_NAME,
                    ExistingPeriodicWorkPolicy.UPDATE,
                    request
                )
            } catch (error: Exception) {
                Log.w(TAG, "Unable to enqueue daily usage alert", error)
            }
        }
    }

    override suspend fun doWork(): Result {
        return try {
            val appContext = applicationContext
            val prefs = appContext.appPreferencesDataStore.data.first()
            val enabled = prefs[booleanPreferencesKey(
                PreferenceManager.KEY_USAGE_ALERT_ENABLED
            )] ?: false
            if (!enabled) return Result.success()

            val budgetMb = prefs[intPreferencesKey(
                PreferenceManager.KEY_USAGE_ALERT_BUDGET_MB
            )] ?: PreferenceManager.DEFAULT_USAGE_ALERT_BUDGET_MB

            val today = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE)
            val usage = UsageDataSource(appContext).getUsageForDate(today)
                ?: return Result.success()
            val usedMb = usage.mobileTotalBytes / (1024L * 1024L)
            if (usedMb < budgetMb) return Result.success()

            val lastNotified = prefs[stringPreferencesKey(
                PreferenceManager.KEY_USAGE_ALERT_LAST_NOTIFIED
            )]
            if (lastNotified == today) return Result.success()

            showBudgetReachedNotification(appContext, usedMb, budgetMb)
            appContext.appPreferencesDataStore.edit {
                it[stringPreferencesKey(
                    PreferenceManager.KEY_USAGE_ALERT_LAST_NOTIFIED
                )] = today
            }
            Result.success()
        } catch (error: Exception) {
            Log.w(TAG, "Usage alert check failed", error)
            Result.retry()
        }
    }

    private fun showBudgetReachedNotification(
        context: Context,
        usedMb: Long,
        budgetMb: Int
    ) {
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            manager.createNotificationChannel(
                NotificationChannel(
                    CHANNEL_ID,
                    context.getString(R.string.usage_alerts_channel),
                    NotificationManager.IMPORTANCE_DEFAULT
                ).apply {
                    description = context.getString(R.string.usage_alerts_channel_desc)
                    enableVibration(false)
                    setSound(null, null)
                }
            )
        }

        val pendingIntent = PendingIntent.getActivity(
            context,
            0,
            Intent(context, MainActivity::class.java).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
            },
            PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        )

        val notification = NotificationCompat.Builder(context, CHANNEL_ID)
            .setSmallIcon(R.drawable.ic_speed)
            .setContentTitle(context.getString(R.string.usage_alert_title))
            .setContentText(
                context.getString(R.string.usage_alert_text, usedMb, budgetMb)
            )
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .setPriority(NotificationCompat.PRIORITY_DEFAULT)
            .build()

        manager.notify(NOTIFICATION_ID, notification)
    }
}
