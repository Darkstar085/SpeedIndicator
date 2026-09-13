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
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import javax.inject.Inject

/**
 * Foreground service for continuous, user-visible network speed monitoring.
 *
 * The service uses the Android specialUse foreground-service type because this
 * monitoring use case is continuous and does not fit the dataSync category.
 */
@AndroidEntryPoint
class SpeedMonitorService : Service() {

    companion object {
        private const val TAG = "SpeedMonitorService"
    }

    @Inject lateinit var getCurrentSpeedUseCase: GetCurrentSpeedUseCase
    @Inject lateinit var getDailyUsageUseCase: GetDailyUsageUseCase
    @Inject lateinit var saveUsageUseCase: SaveUsageUseCase
    @Inject lateinit var trafficStateManager: TrafficStateManager
    @Inject lateinit var preferenceManager: PreferenceManager

    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Default)
    private var monitoringJob: Job? = null
    private var periodicSaveJob: Job? = null
    private var showOnLockScreen = true
    private var showUploadSpeed = false

    // Session deltas. These are updated only by the speed collector.
    private var sessionWifiRxBytes = 0L
    private var sessionWifiTxBytes = 0L
    private var sessionMobileRxBytes = 0L
    private var sessionMobileTxBytes = 0L

    private var baseUsage: UsageInfo? = null

    private lateinit var notificationManager: NotificationManager
    private lateinit var connectivityManager: ConnectivityManager

    private enum class NetworkType { WIFI, MOBILE, OTHER }

    override fun onCreate() {
        super.onCreate()
        NotificationHelper.createNotificationChannel(this)
        notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        connectivityManager = getSystemService(CONNECTIVITY_SERVICE) as ConnectivityManager
        trafficStateManager.setServiceRunning(true)

        serviceScope.launch {
            preferenceManager.lockScreenNotification.collect { showOnLockScreen = it }
        }
        serviceScope.launch {
            preferenceManager.showUploadSpeed.collect { showUploadSpeed = it }
        }
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val notification = NotificationHelper.buildNotification(
            this,
            "0 B/s",
            null,
            "0 B/s",
            "0 B",
            "0 B",
            "",
            "0",
            "B/s"
        )

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            val foregroundServiceType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.UPSIDE_DOWN_CAKE) {
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

        if (monitoringJob?.isActive != true) {
            startMonitoring()
        }

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
                    trafficStateManager.updateSpeed(speed)

                    when (activeNetworkType()) {
                        NetworkType.WIFI -> {
                            sessionWifiRxBytes += speed.downloadBytes
                            sessionWifiTxBytes += speed.uploadBytes
                        }
                        NetworkType.MOBILE -> {
                            sessionMobileRxBytes += speed.downloadBytes
                            sessionMobileTxBytes += speed.uploadBytes
                        }
                        NetworkType.OTHER -> Unit
                    }

                    val liveUsage = buildCurrentUsage()
                    trafficStateManager.updateDailyUsage(liveUsage)

                    val totalSpeedStr = FormatUtils.formatSpeed(speed.totalBytesPerSecond)
                    val downloadSpeedStr = FormatUtils.formatSpeed(speed.downloadBytesPerSecond)
                    val uploadSpeedStr = if (showUploadSpeed) {
                        FormatUtils.formatSpeed(speed.uploadBytesPerSecond)
                    } else {
                        null
                    }

                    val mobileUsageStr = FormatUtils.formatBytes(
                        liveUsage.mobileRxBytes + liveUsage.mobileTxBytes
                    )
                    val wifiUsageStr = FormatUtils.formatBytes(
                        liveUsage.wifiRxBytes + liveUsage.wifiTxBytes
                    )
                    val (speedValue, speedUnit) = FormatUtils.formatSpeedCompact(speed.totalBytesPerSecond)
                    val signalStrength = getSignalStrength()

                    val notification = NotificationHelper.buildNotification(
                        this@SpeedMonitorService,
                        downloadSpeedStr,
                        uploadSpeedStr,
                        totalSpeedStr,
                        mobileUsageStr,
                        wifiUsageStr,
                        signalStrength,
                        speedValue,
                        speedUnit
                    ).apply {
                        visibility = if (showOnLockScreen) {
                            Notification.VISIBILITY_PUBLIC
                        } else {
                            Notification.VISIBILITY_SECRET
                        }
                    }
                    notificationManager.notify(NotificationHelper.NOTIFICATION_ID, notification)
                }
        }

        periodicSaveJob = serviceScope.launch {
            while (true) {
                delay(60_000L)
                saveCurrentUsage()
            }
        }
    }

    private suspend fun loadTodayUsage() {
        val today = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE)
        baseUsage = getDailyUsageUseCase.getByDate(today) ?: UsageInfo(date = today)
        sessionWifiRxBytes = 0L
        sessionWifiTxBytes = 0L
        sessionMobileRxBytes = 0L
        sessionMobileTxBytes = 0L
    }

    private fun buildCurrentUsage(): UsageInfo {
        val currentBase = baseUsage ?: return UsageInfo(
            date = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE)
        )
        return UsageInfo(
            date = currentBase.date,
            wifiRxBytes = currentBase.wifiRxBytes + sessionWifiRxBytes,
            wifiTxBytes = currentBase.wifiTxBytes + sessionWifiTxBytes,
            mobileRxBytes = currentBase.mobileRxBytes + sessionMobileRxBytes,
            mobileTxBytes = currentBase.mobileTxBytes + sessionMobileTxBytes
        )
    }

    private suspend fun saveCurrentUsage() {
        baseUsage?.let { saveUsageUseCase(buildCurrentUsage()) }
    }

    private fun activeNetworkType(): NetworkType {
        val network = connectivityManager.activeNetwork ?: return NetworkType.OTHER
        val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return NetworkType.OTHER
        return when {
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) -> NetworkType.WIFI
            capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) -> NetworkType.MOBILE
            else -> NetworkType.OTHER
        }
    }

    private fun isWifiConnected(): Boolean = activeNetworkType() == NetworkType.WIFI

    private fun getSignalStrength(): String {
        if (!isWifiConnected()) return ""
        return try {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                val capabilities = connectivityManager.getNetworkCapabilities(connectivityManager.activeNetwork)
                val wifiInfo = capabilities?.transportInfo as? WifiInfo
                wifiInfo?.let { "${calculatePercentage(it.rssi)}%" } ?: ""
            } else {
                @Suppress("DEPRECATION")
                val wifiManager = applicationContext.getSystemService(WIFI_SERVICE) as WifiManager
                @Suppress("DEPRECATION")
                "${calculatePercentage(wifiManager.connectionInfo.rssi)}%"
            }
        } catch (e: Exception) {
            Log.e(TAG, "Failed to read signal strength", e)
            ""
        }
    }

    private fun calculatePercentage(rssi: Int): Int {
        return when {
            rssi >= -50 -> 100
            rssi >= -60 -> 80 + ((rssi + 60) * 2)
            rssi >= -70 -> 60 + ((rssi + 70) * 2)
            rssi >= -80 -> 40 + ((rssi + 80) * 2)
            rssi >= -90 -> 20 + ((rssi + 90) * 2)
            else -> maxOf(0, 100 + rssi)
        }.coerceIn(0, 100)
    }

    override fun onDestroy() {
        runBlocking(Dispatchers.IO) {
            monitoringJob?.cancelAndJoin()
            periodicSaveJob?.cancelAndJoin()
            saveCurrentUsage()
        }

        trafficStateManager.setServiceRunning(false)
        monitoringJob = null
        periodicSaveJob = null
        serviceScope.cancel()
        super.onDestroy()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
