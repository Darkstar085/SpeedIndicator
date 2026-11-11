package com.sipun.netspeedindicator.core.service

import com.sipun.netspeedindicator.core.state.TrafficStateManager
import com.sipun.netspeedindicator.core.util.FormatUtils
import com.sipun.netspeedindicator.core.util.SpeedNotificationUpdater
import com.sipun.netspeedindicator.data.preferences.PreferenceManager
import com.sipun.netspeedindicator.domain.usecase.GetCurrentSpeedUseCase
import com.sipun.netspeedindicator.domain.usecase.GetDailyUsageUseCase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.retryWhen
import kotlinx.coroutines.launch
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import javax.inject.Inject

/**
 * Coordinates the long-running monitoring work independently from the Android service lifecycle.
 */
class SpeedMonitorController @Inject constructor(
    private val getCurrentSpeedUseCase: GetCurrentSpeedUseCase,
    private val getDailyUsageUseCase: GetDailyUsageUseCase,
    private val trafficStateManager: TrafficStateManager,
    private val preferenceManager: PreferenceManager,
    private val notificationUpdater: SpeedNotificationUpdater
) {
    private var monitoringJob: Job? = null
    private var usageRefreshJob: Job? = null
    private var preferenceJob: Job? = null
    private var showOnLockScreen = true
    private var showUploadSpeed = false

    fun start(scope: CoroutineScope) {
        if (monitoringJob?.isActive == true) return


        preferenceJob?.cancel()
        preferenceJob = scope.launch {
            launch {
                preferenceManager.lockScreenNotification.collect { showOnLockScreen = it }
            }
            launch {
                preferenceManager.showUploadSpeed.collect { showUploadSpeed = it }
            }
        }

        monitoringJob = scope.launch {
            refreshDailyUsage()
            getCurrentSpeedUseCase()
                .retryWhen { cause, attempt ->
                    android.util.Log.e(
                        "SpeedMonitorController",
                        "Speed monitoring stream failed; retrying",
                        cause
                    )
                    delay(minOf(5_000L * (1L shl attempt.coerceAtMost(4).toInt()), 60_000L))
                    true
                }
                .collect { speed ->
                    trafficStateManager.updateSpeed(speed)
                    val usage = trafficStateManager.dailyUsage.value
                    notificationUpdater.update(
                        downloadSpeed = FormatUtils.formatSpeed(speed.downloadBytesPerSecond),
                        uploadSpeed = if (showUploadSpeed) {
                            FormatUtils.formatSpeed(speed.uploadBytesPerSecond)
                        } else {
                            null
                        },
                        totalSpeed = FormatUtils.formatSpeed(speed.totalBytesPerSecond),
                        mobileUsage = FormatUtils.formatBytes(
                            usage.mobileRxBytes + usage.mobileTxBytes
                        ),
                        wifiUsage = FormatUtils.formatBytes(
                            usage.wifiRxBytes + usage.wifiTxBytes
                        ),
                        speedValue = FormatUtils.formatSpeedCompact(speed.totalBytesPerSecond).first,
                        speedUnit = FormatUtils.formatSpeedCompact(speed.totalBytesPerSecond).second,
                        showOnLockScreen = showOnLockScreen
                    )
                }
        }

        usageRefreshJob = scope.launch {
            while (true) {
                delay(60_000L)
                refreshDailyUsage()
            }
        }
    }

    fun stop() {
        monitoringJob?.cancel()
        usageRefreshJob?.cancel()
        preferenceJob?.cancel()
        monitoringJob = null
        usageRefreshJob = null
        preferenceJob = null
    }

    private suspend fun refreshDailyUsage() {
        val today = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE)
        getDailyUsageUseCase.getByDate(today)?.let(trafficStateManager::updateDailyUsage)
    }
}
