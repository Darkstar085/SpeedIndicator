package com.sipun.netspeedindicator.data.manager

import android.os.SystemClock
import com.sipun.netspeedindicator.domain.model.SpeedInfo
import com.sipun.netspeedindicator.domain.model.UsageInfo
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Singleton manager to hold real-time traffic state.
 * Acts as a single source of truth for both the Foreground Service and UI.
 *
 * Peak speed and the monitoring start time live here (not in a screen ViewModel) so they
 * survive ViewModel recreation (e.g. switching bottom-nav tabs) and only reset when the
 * service actually (re)starts monitoring.
 */
@Singleton
class TrafficStateManager @Inject constructor() {

    private val _speed = MutableStateFlow(SpeedInfo())
    val speed = _speed.asStateFlow()

    private val _dailyUsage = MutableStateFlow<UsageInfo>(UsageInfo())
    val dailyUsage = _dailyUsage.asStateFlow()

    private val _isServiceRunning = MutableStateFlow(false)
    val isServiceRunning = _isServiceRunning.asStateFlow()

    private val _peakSpeedBytesPerSecond = MutableStateFlow(0L)
    val peakSpeedBytesPerSecond = _peakSpeedBytesPerSecond.asStateFlow()

    /** [SystemClock.elapsedRealtime] when monitoring last started, or 0L if not running. */
    private val _monitoringStartElapsedRealtime = MutableStateFlow(0L)
    val monitoringStartElapsedRealtime = _monitoringStartElapsedRealtime.asStateFlow()

    fun updateSpeed(speed: SpeedInfo) {
        _speed.value = speed
        if (speed.totalBytesPerSecond > _peakSpeedBytesPerSecond.value) {
            _peakSpeedBytesPerSecond.value = speed.totalBytesPerSecond
        }
    }

    fun updateDailyUsage(usage: UsageInfo) {
        _dailyUsage.value = usage
    }

    fun setServiceRunning(isRunning: Boolean) {
        _isServiceRunning.value = isRunning
        if (isRunning) {
            _speed.value = SpeedInfo()
            _peakSpeedBytesPerSecond.value = 0L
            _monitoringStartElapsedRealtime.value = SystemClock.elapsedRealtime()
        } else {
            _speed.value = SpeedInfo()
            _peakSpeedBytesPerSecond.value = 0L
            _monitoringStartElapsedRealtime.value = 0L
        }
    }
}
