package com.sipun.netspeedindicator.presentation.history

sealed interface HistoryUiEvent {
    data class OnSelectMonth(val monthsBack: Int) : HistoryUiEvent
}
