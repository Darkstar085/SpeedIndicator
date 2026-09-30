package com.sipun.netspeedindicator.ui.screens.main.history

import com.sipun.netspeedindicator.domain.model.UsageInfo

enum class HistoryRange {
    SEVEN_DAYS,
    THIS_MONTH,
    THREE_MONTHS
}

data class HistoryUiState(
    val dailyUsage: List<UsageInfo> = emptyList(),
    val isLoading: Boolean = false,
    val selectedRange: HistoryRange = HistoryRange.SEVEN_DAYS
)
