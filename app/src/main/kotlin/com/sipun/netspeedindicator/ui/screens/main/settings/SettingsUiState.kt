package com.sipun.netspeedindicator.ui.screens.main.settings

data class SettingsUiState(
    val appTheme: Int = 0,
    val dynamicColor: Boolean = true,
    val pureBlackTheme: Boolean = false,
    val lockScreenNotification: Boolean = true,
    val showUploadSpeed: Boolean = false,
    val hasUsagePermission: Boolean = false,
    val isBatteryOptimizationDisabled: Boolean = false,
    val isAutoStartAvailable: Boolean = false
)
