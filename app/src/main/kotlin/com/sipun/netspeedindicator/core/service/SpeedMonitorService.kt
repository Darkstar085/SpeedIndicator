package com.sipun.netspeedindicator.core.service

import android.app.Notification
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.ServiceCompat
import com.sipun.netspeedindicator.core.util.FormatUtils
import com.sipun.netspeedindicator.core.util.NotificationHelper
import com.sipun.netspeedindicator.core.state.TrafficStateManager
import com.sipun.netspeedindicator.data.preferences.PreferenceManager
import com.sipun.netspeedindicator.domain.usecase.GetCurrentSpeedUseCase
import com.sipun.netspeedindicator.domain.usecase.GetDailyUsageUseCase
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch

@AndroidEntryPoint
class SpeedMonitorService : Service() {
    companion object { private const val TAG = "SpeedMonitorService" }
    @javax.inject.Inject lateinit var getCurrentSpeedUseCase: GetCurrentSpeedUseCase
    @javax.inject.Inject lateinit var getDailyUsageUseCase: GetDailyUsageUseCase
    @javax.inject.Inject lateinit var trafficStateManager: TrafficStateManager
    @javax.inject.Inject lateinit var preferenceManager: PreferenceManager
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var monitoringJob: Job? = null
    private var usageRefreshJob: Job? = null
    private var showOnLockScreen = true
    private var showUploadSpeed = false
    private lateinit var notificationManager: NotificationManager
    private lateinit var connectivityManager: ConnectivityManager
    private val networkCallback = object : ConnectivityManager.NetworkCallback() {
        override fun onLost(network: Network) {
            if (!hasValidatedNetwork()) stopMonitoringService()
        }

        override fun onCapabilitiesChanged(network: Network, capabilities: NetworkCapabilities) {
            if (!capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED) && !hasValidatedNetwork()) {
                stopMonitoringService()
            }
        }
    }

    override fun onCreate() {
        super.onCreate()
        NotificationHelper.createNotificationChannel(this)
        notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        connectivityManager = getSystemService(CONNECTIVITY_SERVICE) as ConnectivityManager
        try {
            connectivityManager.registerDefaultNetworkCallback(networkCallback)
        } catch (e: Exception) {
            Log.w(TAG, "Unable to register network callback", e)
        }
        trafficStateManager.setServiceRunning(true)
        serviceScope.launch { preferenceManager.lockScreenNotification.collect { showOnLockScreen = it } }
        serviceScope.launch { preferenceManager.showUploadSpeed.collect { showUploadSpeed = it } }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (!preferenceManager.isMonitoringEnabled() || !hasValidatedNetwork()) {
            stopMonitoringService()
            return START_NOT_STICKY
        }
        startForegroundMonitoring()
        if (monitoringJob?.isActive != true) startMonitoring()
        return START_NOT_STICKY
    }

    private fun stopMonitoringService() {
        monitoringJob?.cancel()
        usageRefreshJob?.cancel()
        monitoringJob = null
        usageRefreshJob = null
        trafficStateManager.setServiceRunning(false)
        NetworkMonitorScheduler.schedule(this)
        stopForeground(STOP_FOREGROUND_REMOVE)
        notificationManager.cancel(NotificationHelper.NOTIFICATION_ID)
        stopSelf()
    }

    private fun startForegroundMonitoring() {
        val notification = NotificationHelper.buildNotification(this, "0 B/s", null, "0 B/s", "0 B", "0 B", "", "0", "B/s")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val foregroundServiceType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) ServiceInfo.FOREGROUND_SERVICE_TYPE_SPECIAL_USE else 0
            ServiceCompat.startForeground(this, NotificationHelper.NOTIFICATION_ID, notification, foregroundServiceType)
        } else startForeground(NotificationHelper.NOTIFICATION_ID, notification)
    }

    private fun startMonitoring() {
        monitoringJob?.cancel()
        usageRefreshJob?.cancel()
        monitoringJob = serviceScope.launch {
            refreshDailyUsage()
            getCurrentSpeedUseCase().catch { e -> Log.e(TAG, "Speed monitoring stream failed", e) }.collect { speed ->
                if (!hasValidatedNetwork()) {
                    stopMonitoringService()
                    return@collect
                }
                trafficStateManager.updateSpeed(speed)
                val usage = trafficStateManager.dailyUsage.value
                val downloadSpeed = FormatUtils.formatSpeed(speed.downloadBytesPerSecond)
                val uploadSpeed = if (showUploadSpeed) FormatUtils.formatSpeed(speed.uploadBytesPerSecond) else null
                val (speedValue, speedUnit) = FormatUtils.formatSpeedCompact(speed.totalBytesPerSecond)
                notificationManager.notify(NotificationHelper.NOTIFICATION_ID, NotificationHelper.buildNotification(this@SpeedMonitorService, downloadSpeed, uploadSpeed, FormatUtils.formatSpeed(speed.totalBytesPerSecond), FormatUtils.formatBytes(usage.mobileRxBytes + usage.mobileTxBytes), FormatUtils.formatBytes(usage.wifiRxBytes + usage.wifiTxBytes), "", speedValue, speedUnit).apply {
                    visibility = if (showOnLockScreen) Notification.VISIBILITY_PUBLIC else Notification.VISIBILITY_SECRET
                })
            }
        }
        usageRefreshJob = serviceScope.launch {
            while (true) { delay(60_000L); refreshDailyUsage() }
        }
    }

    private suspend fun refreshDailyUsage() {
        val today = java.time.LocalDate.now().format(java.time.format.DateTimeFormatter.ISO_LOCAL_DATE)
        getDailyUsageUseCase.getByDate(today)?.let { trafficStateManager.updateDailyUsage(it) }
    }

    private fun hasValidatedNetwork(): Boolean {
        val network = connectivityManager.activeNetwork ?: return false
        val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
        return capabilities.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED)
    }

    override fun onDestroy() {
        monitoringJob?.cancel()
        usageRefreshJob?.cancel()
        monitoringJob = null
        usageRefreshJob = null
        try { connectivityManager.unregisterNetworkCallback(networkCallback) } catch (_: Exception) { }
        trafficStateManager.setServiceRunning(false)
        if (preferenceManager.isMonitoringEnabled()) NetworkMonitorScheduler.schedule(this)
        stopForeground(STOP_FOREGROUND_REMOVE)
        notificationManager.cancel(NotificationHelper.NOTIFICATION_ID)
        serviceScope.cancel()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
