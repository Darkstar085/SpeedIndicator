package com.sipun.netspeedindicator.ui.screens.main.home

import com.sipun.netspeedindicator.domain.model.LiveAppRate
import com.sipun.netspeedindicator.domain.model.SpeedInfo
import com.sipun.netspeedindicator.domain.model.UsageInfo

data class HomeUiState(
    val isServiceRunning: Boolean = false,
    val currentSpeed: SpeedInfo = SpeedInfo(),
    val todayUsage: UsageInfo = UsageInfo(),
    val peakSpeed: Long = 0L,
    val sessionDurationSeconds: Long = 0L,
    val showStopDialog: Boolean = false,
    /** All apps with a non-zero (or freshly held) live rate, sorted desc. */
    val activeApps: List<LiveAppRate> = emptyList(),
)
