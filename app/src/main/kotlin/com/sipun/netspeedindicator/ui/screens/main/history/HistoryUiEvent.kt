package com.sipun.netspeedindicator.ui.screens.main.history

sealed interface HistoryUiEvent {
    data class OnSelectRange(val range: HistoryRange) : HistoryUiEvent
}
