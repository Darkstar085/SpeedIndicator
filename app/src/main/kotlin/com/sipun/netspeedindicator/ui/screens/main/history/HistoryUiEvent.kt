package com.sipun.netspeedindicator.ui.screens.main.history

sealed interface HistoryUiEvent {
    data class OnSelectMonth(val monthsBack: Int) : HistoryUiEvent
}
