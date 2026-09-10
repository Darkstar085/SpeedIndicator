package com.sipun.netspeedindicator.presentation.home

import com.sipun.netspeedindicator.domain.model.SpeedInfo
import com.sipun.netspeedindicator.domain.model.UsageInfo

data class HomeUiState(
    val isServiceRunning: Boolean = false,
    val currentSpeed: SpeedInfo = SpeedInfo(),
    val todayUsage: UsageInfo = UsageInfo(),
    val peakSpeed: Long = 0L,
    val sessionDurationSeconds: Long = 0L,
    val showStopDialog: Boolean = false,
    val shouldFinishActivity: Boolean = false
)
