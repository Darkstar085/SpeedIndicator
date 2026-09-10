package com.sipun.netspeedindicator.presentation.home

sealed interface HomeUiEvent {
    data object OnStopClick : HomeUiEvent
    data object OnDismissDialog : HomeUiEvent
    data object OnConfirmStop : HomeUiEvent
    data object OnActivityFinished : HomeUiEvent
}
