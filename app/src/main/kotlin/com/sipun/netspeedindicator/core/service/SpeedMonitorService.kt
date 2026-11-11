package com.sipun.netspeedindicator.core.service

import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.ServiceCompat
import com.sipun.netspeedindicator.core.network.NetworkMonitor
import com.sipun.netspeedindicator.core.state.TrafficStateManager
import com.sipun.netspeedindicator.core.util.NotificationHelper
import com.sipun.netspeedindicator.core.util.SpeedNotificationUpdater
import com.sipun.netspeedindicator.data.preferences.PreferenceManager
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import javax.inject.Inject

@AndroidEntryPoint
class SpeedMonitorService : Service() {
    @Inject
    lateinit var preferenceManager: PreferenceManager

    @Inject
    lateinit var trafficStateManager: TrafficStateManager

    @Inject
    lateinit var monitorController: SpeedMonitorController

    @Inject
    lateinit var networkMonitor: NetworkMonitor

    @Inject
    lateinit var notificationUpdater: SpeedNotificationUpdater

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private lateinit var notificationManager: NotificationManager
    private var networkLossJob: Job? = null
    private var stoppingForNetworkLoss = false

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

        stoppingForNetworkLoss = false
        notificationUpdater.start()
        startForegroundMonitoring()
        networkMonitor.start(::onNetworkUnavailable)

        if (!networkMonitor.hasValidatedNetwork()) {
            stopForNetworkLoss()
            return START_NOT_STICKY
        }

        monitorController.start(serviceScope)
        return START_STICKY
    }

    private fun onNetworkUnavailable() {
        if (!preferenceManager.isMonitoringEnabled() || stoppingForNetworkLoss) return

        networkLossJob?.cancel()
        networkLossJob = serviceScope.launch(Dispatchers.Main) {
            delay(NETWORK_TRANSITION_GRACE_PERIOD_MS)
            if (!networkMonitor.hasValidatedNetwork()) {
                stopForNetworkLoss()
            }
        }
    }

    private fun stopForNetworkLoss() {
        if (stoppingForNetworkLoss || !preferenceManager.isMonitoringEnabled()) return
        stoppingForNetworkLoss = true
        NetworkMonitorScheduler.scheduleReconnect(this)
        stopMonitoringService()
    }

    private fun stopMonitoringService() {
        notificationUpdater.stop()
        networkMonitor.stop()
        monitorController.stop()
        trafficStateManager.setServiceRunning(false)
        if (preferenceManager.isMonitoringEnabled()) {
            NetworkMonitorScheduler.schedule(this)
        } else {
            NetworkMonitorScheduler.cancel(this)
        }
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
        networkLossJob?.cancel()
        notificationUpdater.stop()
        networkMonitor.stop()
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

    private companion object {
        const val NETWORK_TRANSITION_GRACE_PERIOD_MS = 1_000L
    }
}
