package com.sipun.netspeedindicator.core.service

import android.app.Notification
import android.app.NotificationManager
import android.app.Service
import android.content.Intent
import android.content.pm.ServiceInfo
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.net.wifi.WifiInfo
import android.net.wifi.WifiManager
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.ServiceCompat
import com.sipun.netspeedindicator.core.util.FormatUtils
import com.sipun.netspeedindicator.core.util.NotificationHelper
import com.sipun.netspeedindicator.data.manager.TrafficStateManager
import com.sipun.netspeedindicator.data.preferences.PreferenceManager
import com.sipun.netspeedindicator.domain.model.UsageInfo
import com.sipun.netspeedindicator.domain.usecase.GetCurrentSpeedUseCase
import com.sipun.netspeedindicator.domain.usecase.GetDailyUsageUseCase
import com.sipun.netspeedindicator.domain.usecase.SaveUsageUseCase
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withTimeoutOrNull
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import javax.inject.Inject

/**
 * Foreground service for real-time device-wide speed monitoring.
 *
 * Android 15+ limits dataSync foreground services to six hours in a rolling
 * 24-hour period, so this service must not be treated as an indefinite worker.
 */
@AndroidEntryPoint
class SpeedMonitorService : Service() {

    companion object {
        private const val TAG = "SpeedMonitorService"
        private const val USAGE_SAVE_INTERVAL_MS = 60_000L
        private const val SHUTDOWN_SAVE_TIMEOUT_MS = 2_000L
        private val DATE_FORMATTER = DateTimeFormatter.ISO_LOCAL_DATE
    }

    @Inject lateinit var getCurrentSpeedUseCase: GetCurrentSpeedUseCase
    @Inject lateinit var getDailyUsageUseCase: GetDailyUsageUseCase
    @Inject lateinit var saveUsageUseCase: SaveUsageUseCase
    @Inject lateinit var trafficStateManager: TrafficStateManager
    @Inject lateinit var preferenceManager: PreferenceManager

    private val serviceScope = CoroutineScope(Dispatchers.Default + Job())
    private val usageMutex = Mutex()
    private var monitoringJob: Job? = null
    private var periodicSaveJob: Job? = null
    private var showOnLockScreen = true
    private var showUploadSpeed = false

    private var sessionWifiRxBytes = 0L
    private var sessionWifiTxBytes = 0L
    private var sessionMobileRxBytes = 0L
    private var sessionMobileTxBytes = 0L
    private var baseUsage: UsageInfo? = null

    private lateinit var notificationManager: NotificationManager
    private lateinit var connectivityManager: ConnectivityManager

    override fun onCreate() {
        super.onCreate()
        NotificationHelper.createNotificationChannel(this)
        notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        connectivityManager = getSystemService(CONNECTIVITY_SERVICE) as ConnectivityManager

        serviceScope.launch {
            preferenceManager.lockScreenNotification.collect { showOnLockScreen = it }
        }
        serviceScope.launch {
            preferenceManager.showUploadSpeed.collect { showUploadSpeed = it }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val notification = NotificationHelper.buildNotification(
            this, "0 B/s", null, "0 B/s", "0 B", "0 B", "", "", ""
        )

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ServiceCompat.startForeground(
                this, NotificationHelper.NOTIFICATION_ID, notification,
                ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
            )
        } else {
            startForeground(NotificationHelper.NOTIFICATION_ID, notification)
        }

        trafficStateManager.setServiceRunning(true)

        if (monitoringJob?.isActive != true) startMonitoring()
        return START_STICKY
    }

    private fun startMonitoring() {
        monitoringJob?.cancel()
        periodicSaveJob?.cancel()

        monitoringJob = serviceScope.launch {
            loadTodayUsage()

            getCurrentSpeedUseCase()
                .catch { e -> Log.e(TAG, "Speed monitoring stream failed", e) }
                .collect { speed ->
                    val liveUsage = usageMutex.withLock {
                        rolloverIfNeededLocked()

                        when {
                            isWifiConnected() -> {
                                sessionWifiRxBytes += speed.downloadBytesPerSecond
                                sessionWifiTxBytes += speed.uploadBytesPerSecond
                            }
                            isMobileConnected() -> {
                                sessionMobileRxBytes += speed.downloadBytesPerSecond
                                sessionMobileTxBytes += speed.uploadBytesPerSecond
                            }
                            else -> {
                                // Do not attribute VPN, Ethernet, disconnected, or
                                // otherwise unknown traffic to mobile usage.
                            }
                        }

                        buildCurrentUsageLocked()
                    } ?: return@collect

                    trafficStateManager.updateSpeed(speed)
                    trafficStateManager.updateDailyUsage(liveUsage)

                    val totalSpeedStr = FormatUtils.formatSpeed(speed.totalBytesPerSecond)
                    val downloadSpeedStr = FormatUtils.formatSpeed(speed.downloadBytesPerSecond)
                    val uploadSpeedStr =
                        if (showUploadSpeed) FormatUtils.formatSpeed(speed.uploadBytesPerSecond) else null

                    val mobileUsageStr = FormatUtils.formatBytes(
                        liveUsage.mobileRxBytes + liveUsage.mobileTxBytes
                    )
                    val wifiUsageStr = FormatUtils.formatBytes(
                        liveUsage.wifiRxBytes + liveUsage.wifiTxBytes
                    )
                    val (speedValue, speedUnit) =
                        FormatUtils.formatSpeedCompact(speed.totalBytesPerSecond)

                    val notification = NotificationHelper.buildNotification(
                        this@SpeedMonitorService,
                        downloadSpeedStr,
                        uploadSpeedStr,
                        totalSpeedStr,
                        mobileUsageStr,
                        wifiUsageStr,
                        getSignalStrength(),
                        speedValue,
                        speedUnit
                    )
                    notification.visibility =
                        if (showOnLockScreen) Notification.VISIBILITY_PUBLIC
                        else Notification.VISIBILITY_SECRET
                    notificationManager.notify(NotificationHelper.NOTIFICATION_ID, notification)
                }
        }

        periodicSaveJob = serviceScope.launch {
            while (true) {
                delay(USAGE_SAVE_INTERVAL_MS)
                saveCurrentUsage()
            }
        }
    }

    private suspend fun loadTodayUsage() {
        val today = LocalDate.now().format(DATE_FORMATTER)
        val usage = getDailyUsageUseCase.getByDate(today) ?: UsageInfo(
            date = today,
            wifiRxBytes = 0L,
            wifiTxBytes = 0L,
            mobileRxBytes = 0L,
            mobileTxBytes = 0L
        )

        usageMutex.withLock {
            baseUsage = usage
            resetSessionCountersLocked()
        }
    }

    private suspend fun rolloverIfNeededLocked() {
        val today = LocalDate.now().format(DATE_FORMATTER)
        if (baseUsage?.date == today) return

        saveCurrentUsageLocked()
        baseUsage = getDailyUsageUseCase.getByDate(today) ?: UsageInfo(
            date = today,
            wifiRxBytes = 0L,
            wifiTxBytes = 0L,
            mobileRxBytes = 0L,
            mobileTxBytes = 0L
        )
        resetSessionCountersLocked()
    }

    private suspend fun saveCurrentUsage() {
        usageMutex.withLock { saveCurrentUsageLocked() }
    }

    private suspend fun saveCurrentUsageLocked() {
        val currentUsage = buildCurrentUsageLocked() ?: return
        saveUsageUseCase(currentUsage)
    }

    private fun buildCurrentUsageLocked(): UsageInfo? {
        val currentBase = baseUsage ?: return null
        return UsageInfo(
            date = currentBase.date,
            wifiRxBytes = currentBase.wifiRxBytes + sessionWifiRxBytes,
            wifiTxBytes = currentBase.wifiTxBytes + sessionWifiTxBytes,
            mobileRxBytes = currentBase.mobileRxBytes + sessionMobileRxBytes,
            mobileTxBytes = currentBase.mobileTxBytes + sessionMobileTxBytes
        )
    }

    private fun resetSessionCountersLocked() {
        sessionWifiRxBytes = 0L
        sessionWifiTxBytes = 0L
        sessionMobileRxBytes = 0L
        sessionMobileTxBytes = 0L
    }

    private fun activeNetworkCapabilities(): NetworkCapabilities? {
        val network = connectivityManager.activeNetwork ?: return null
        return connectivityManager.getNetworkCapabilities(network)
    }

    private fun isWifiConnected(): Boolean =
        activeNetworkCapabilities()?.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) == true

    private fun isMobileConnected(): Boolean =
        activeNetworkCapabilities()?.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) == true

    private fun getSignalStrength(): String {
        try {
            if (!isWifiConnected()) return ""
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val wifiInfo = activeNetworkCapabilities()?.transportInfo as? WifiInfo
                if (wifiInfo != null) return "${calculatePercentage(wifiInfo.rssi)}%"
            } else {
                @Suppress("DEPRECATION")
                val wifiManager = applicationContext.getSystemService(WIFI_SERVICE) as WifiManager
                @Suppress("DEPRECATION")
                return "${calculatePercentage(wifiManager.connectionInfo.rssi)}%"
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to read signal strength", e)
        }
        return ""
    }

    private fun calculatePercentage(rssi: Int): Int = when {
        rssi >= -50 -> 100
        rssi >= -60 -> 80 + ((rssi + 60) * 2)
        rssi >= -70 -> 60 + ((rssi + 70) * 2)
        rssi >= -80 -> 40 + ((rssi + 80) * 2)
        rssi >= -90 -> 20 + ((rssi + 90) * 2)
        else -> maxOf(0, 100 + rssi)
    }.coerceIn(0, 100)

    override fun onTimeout(startId: Int, fgsType: Int) {
        Log.w(TAG, "Foreground service timed out (type=$fgsType); stopping service")
        stopSelf(startId)
    }

    override fun onDestroy() {
        periodicSaveJob?.cancel()
        monitoringJob?.cancel()

        runBlocking(Dispatchers.IO) {
            withTimeoutOrNull(SHUTDOWN_SAVE_TIMEOUT_MS) { saveCurrentUsage() }
        }

        trafficStateManager.setServiceRunning(false)
        serviceScope.cancel()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
