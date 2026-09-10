package com.sipun.netspeedindicator.presentation.history

import com.sipun.netspeedindicator.domain.model.UsageInfo

data class HistoryUiState(
    val dailyUsage: List<UsageInfo> = emptyList(),
    val isLoading: Boolean = false,
    val selectedMonthIndex: Int = 0
)
