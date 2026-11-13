package com.sipun.netspeedindicator.ui.screens.main.settings

import com.sipun.netspeedindicator.data.preferences.AccentGradient
import com.sipun.netspeedindicator.data.preferences.AccentStyle

data class SettingsUiState(
    val appTheme: Int = 0,
    val dynamicColor: Boolean = false,
    val customColor: Int = 0xFF42A5F5.toInt(),
    val accentStyle: AccentStyle = AccentStyle.SOLID,
    val accentGradient: AccentGradient = AccentGradient.SUNSET,
    val customGradientStart: Int = 0xFFE92DE2.toInt(),
    val customGradientEnd: Int = 0xFF4A00E0.toInt(),
    val pureBlackTheme: Boolean = false,
    val lockScreenNotification: Boolean = true,
    val showUploadSpeed: Boolean = false,
    val hasUsagePermission: Boolean = false,
    val canInstallUnknownApps: Boolean = false,
    val isBatteryOptimizationDisabled: Boolean = false,
    val isAutoStartAvailable: Boolean = false
)
