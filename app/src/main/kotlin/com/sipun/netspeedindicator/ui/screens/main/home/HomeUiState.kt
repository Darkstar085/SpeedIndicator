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
    /** Top active apps right now (max 4), sorted by live rate descending. */
    val activeApps: List<LiveAppRate> = emptyList(),
    /** Total number of apps with a non-zero live rate (>= activeApps size). */
    val activeAppCount: Int = 0,
)
