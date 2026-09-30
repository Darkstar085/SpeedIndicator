package com.sipun.netspeedindicator.core.service

import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.ServiceCompat
import com.sipun.netspeedindicator.core.state.TrafficStateManager
import com.sipun.netspeedindicator.core.util.NotificationHelper
import com.sipun.netspeedindicator.data.preferences.PreferenceManager
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import javax.inject.Inject

@AndroidEntryPoint
class SpeedMonitorService : Service() {
    @Inject lateinit var preferenceManager: PreferenceManager
    @Inject lateinit var trafficStateManager: TrafficStateManager
    @Inject lateinit var monitorController: SpeedMonitorController

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private lateinit var notificationManager: NotificationManager

    override fun onCreate() {
        super.onCreate()
        NotificationHelper.createNotificationChannel(this)
        notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        trafficStateManager.setServiceRunning(true)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (!preferenceManager.isMonitoringEnabled()) {
            stopMonitoringService()
            return START_NOT_STICKY
        }

        startForegroundMonitoring()
        monitorController.start(serviceScope, ::stopMonitoringService)
        return START_NOT_STICKY
    }

    private fun stopMonitoringService() {
        monitorController.stop()
        trafficStateManager.setServiceRunning(false)
        NetworkMonitorScheduler.schedule(this)
        stopForeground(STOP_FOREGROUND_REMOVE)
        notificationManager.cancel(NotificationHelper.NOTIFICATION_ID)
        stopSelf()
    }

    private fun startForegroundMonitoring() {
        val notification = NotificationHelper.buildNotification(
            this,
            "0 B/s",
            null,
            "0 B/s",
            "0 B",
            "0 B",
            "0",
            "B/s"
        )

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val foregroundServiceType =
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
                    ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE
                } else {
                    0
                }
            ServiceCompat.startForeground(
                this,
                NotificationHelper.NOTIFICATION_ID,
                notification,
                foregroundServiceType
            )
        } else {
            startForeground(NotificationHelper.NOTIFICATION_ID, notification)
        }
    }

    override fun onDestroy() {
        monitorController.stop()
        trafficStateManager.setServiceRunning(false)
        if (preferenceManager.isMonitoringEnabled()) {
            NetworkMonitorScheduler.schedule(this)
        }
        stopForeground(STOP_FOREGROUND_REMOVE)
        notificationManager.cancel(NotificationHelper.NOTIFICATION_ID)
        serviceScope.cancel()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
