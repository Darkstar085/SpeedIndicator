package com.sipun.netspeedindicator.presentation.settings

sealed interface SettingsUiEvent {
    data object OnResume : SettingsUiEvent
    data object OnThemeCycle : SettingsUiEvent
    data class OnDynamicColorChanged(val enabled: Boolean) : SettingsUiEvent
    data class OnLockScreenNotificationChanged(val enabled: Boolean) : SettingsUiEvent
    data class OnNotificationBarChanged(val enabled: Boolean) : SettingsUiEvent
    data object OnRequestUsagePermission : SettingsUiEvent
    data object OnRequestBatteryOptimization : SettingsUiEvent
    data object OnRequestAutoStart : SettingsUiEvent
}
