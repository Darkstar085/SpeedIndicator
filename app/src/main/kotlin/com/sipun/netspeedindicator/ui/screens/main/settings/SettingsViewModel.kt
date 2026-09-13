package com.sipun.netspeedindicator.ui.screens.main.settings

import android.content.Context
import android.content.Intent
import android.os.PowerManager
import android.provider.Settings
import androidx.core.net.toUri
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.sipun.netspeedindicator.core.util.AutoStartPermissionUtils
import com.sipun.netspeedindicator.core.util.PermissionUtils
import com.sipun.netspeedindicator.data.preferences.PreferenceManager
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.stateIn
import javax.inject.Inject

@HiltViewModel
class SettingsViewModel @Inject constructor(@ApplicationContext private val context: Context, private val preferenceManager: PreferenceManager) : ViewModel() {
    private val _hasUsagePermission = MutableStateFlow(false)
    private val _isBatteryOptimizationDisabled = MutableStateFlow(false)
    private val _isAutoStartAvailable = MutableStateFlow(false)

    val uiState: StateFlow<SettingsUiState> = combine(
        preferenceManager.appTheme,
        preferenceManager.dynamicColor,
        preferenceManager.lockScreenNotification,
        preferenceManager.showUploadSpeed
    ) { appTheme, dynamicColor, lockScreenNotification, showUploadSpeed ->
        SettingsUiState(appTheme, dynamicColor, lockScreenNotification, showUploadSpeed)
    }.combine(_hasUsagePermission) { state, hasUsagePermission ->
        state.copy(hasUsagePermission = hasUsagePermission)
    }.combine(_isBatteryOptimizationDisabled) { state, isDisabled ->
        state.copy(isBatteryOptimizationDisabled = isDisabled)
    }.combine(_isAutoStartAvailable) { state, isAvailable ->
        state.copy(isAutoStartAvailable = isAvailable)
    }.stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), SettingsUiState())

    init { checkPermissions() }

    fun onEvent(event: SettingsUiEvent) {
        when (event) {
            SettingsUiEvent.OnResume -> checkPermissions()
            SettingsUiEvent.OnThemeCycle -> preferenceManager.setAppTheme((uiState.value.appTheme + 1) % 3)
            is SettingsUiEvent.OnDynamicColorChanged -> preferenceManager.setDynamicColor(event.enabled)
            is SettingsUiEvent.OnLockScreenNotificationChanged -> preferenceManager.setLockScreenNotification(event.enabled)
            is SettingsUiEvent.OnNotificationBarChanged -> preferenceManager.setShowUploadSpeed(event.enabled)
            SettingsUiEvent.OnRequestUsagePermission -> PermissionUtils.openUsageAccessSettings(context)
            SettingsUiEvent.OnRequestBatteryOptimization -> requestDisableBatteryOptimization()
            SettingsUiEvent.OnRequestAutoStart -> AutoStartPermissionUtils.requestAutoStartPermission(context)
        }
    }

    private fun checkPermissions() {
        _hasUsagePermission.value = PermissionUtils.hasUsageStatsPermission(context)
        _isBatteryOptimizationDisabled.value = checkBatteryOptimization()
        _isAutoStartAvailable.value = AutoStartPermissionUtils.isAutoStartPermissionAvailable(context)
    }

    private fun checkBatteryOptimization(): Boolean {
        val powerManager = context.getSystemService(Context.POWER_SERVICE) as PowerManager
        return powerManager.isIgnoringBatteryOptimizations(context.packageName)
    }

    private fun requestDisableBatteryOptimization() {
        try {
            val intent = Intent().apply {
                action = Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS
                data = "package:${context.packageName}".toUri()
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
        } catch (e: Exception) {
            val intent = Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS).apply {
                flags = Intent.FLAG_ACTIVITY_NEW_TASK
            }
            context.startActivity(intent)
        }
    }
}
