package com.sipun.netspeedindicator.core.state

import android.os.SystemClock
import com.sipun.netspeedindicator.domain.model.SpeedInfo
import com.sipun.netspeedindicator.domain.model.UsageInfo
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import javax.inject.Inject
import javax.inject.Singleton

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
    private val _monitoringStartElapsedRealtime = MutableStateFlow(0L)
    val monitoringStartElapsedRealtime = _monitoringStartElapsedRealtime.asStateFlow()

    fun updateSpeed(speed: SpeedInfo) {
        _speed.value = speed
        if (speed.totalBytesPerSecond > _peakSpeedBytesPerSecond.value) _peakSpeedBytesPerSecond.value = speed.totalBytesPerSecond
    }

    fun updateDailyUsage(usage: UsageInfo) { _dailyUsage.value = usage }

    fun setServiceRunning(isRunning: Boolean) {
        _isServiceRunning.value = isRunning
        if (isRunning) {
            _peakSpeedBytesPerSecond.value = 0L
            _monitoringStartElapsedRealtime.value = SystemClock.elapsedRealtime()
        } else {
            _monitoringStartElapsedRealtime.value = 0L
            _peakSpeedBytesPerSecond.value = 0L
            _speed.value = SpeedInfo()
        }
    }
}
