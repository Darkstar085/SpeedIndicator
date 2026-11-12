package com.sipun.netspeedindicator.ui.screens.main.settings

data class SettingsUiState(
    val appTheme: Int = 0,
    val dynamicColor: Boolean = true,
    val accent: Int = 0,
    val pureBlackTheme: Boolean = false,
    val usageAlertEnabled: Boolean = false,
    val usageAlertBudgetMb: Int = 1024,
    val lockScreenNotification: Boolean = true,
    val showUploadSpeed: Boolean = false,
    val hasUsagePermission: Boolean = false,
    val canInstallUnknownApps: Boolean = false,
    val isBatteryOptimizationDisabled: Boolean = false,
    val isAutoStartAvailable: Boolean = false
)
