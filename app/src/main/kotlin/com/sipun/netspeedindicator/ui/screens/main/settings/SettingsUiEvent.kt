package com.sipun.netspeedindicator.ui.screens.main.settings

import com.sipun.netspeedindicator.data.preferences.AccentGradient
import com.sipun.netspeedindicator.data.preferences.AccentStyle

sealed interface SettingsUiEvent {
    data object OnResume : SettingsUiEvent
    data class OnThemeSelected(val theme: Int) : SettingsUiEvent
    data class OnCustomColorChanged(val color: Int) : SettingsUiEvent
    data class OnAccentStyleChanged(val style: AccentStyle) : SettingsUiEvent
    data class OnAccentGradientChanged(val gradient: AccentGradient) : SettingsUiEvent
    data class OnCustomGradientChanged(val start: Int, val end: Int) : SettingsUiEvent
    data class OnDynamicColorChanged(val enabled: Boolean) : SettingsUiEvent
    data class OnPureBlackThemeChanged(val enabled: Boolean) : SettingsUiEvent
    data class OnLockScreenNotificationChanged(val enabled: Boolean) : SettingsUiEvent
    data class OnNotificationBarChanged(val enabled: Boolean) : SettingsUiEvent
    data object OnRequestUsagePermission : SettingsUiEvent
    data object OnRequestInstallUnknownApps : SettingsUiEvent
    data object OnRequestBatteryOptimization : SettingsUiEvent
    data object OnRequestAutoStart : SettingsUiEvent
}
