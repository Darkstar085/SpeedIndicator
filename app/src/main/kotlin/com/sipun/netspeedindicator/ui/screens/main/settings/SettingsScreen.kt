package com.sipun.netspeedindicator.ui.screens.main.settings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BatteryChargingFull
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.FormatPaint
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LockClock
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.RocketLaunch
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import com.sipun.netspeedindicator.R
import com.sipun.netspeedindicator.ui.components.AppTopBar
import com.sipun.netspeedindicator.ui.theme.dimens

@Composable
fun SettingsScreen(viewModel: SettingsViewModel = hiltViewModel()) {
    val uiState by viewModel.uiState.collectAsState()
    val lifecycleOwner = LocalLifecycleOwner.current
    LaunchedEffect(lifecycleOwner) { lifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) { viewModel.onEvent(SettingsUiEvent.OnResume) } }
    SettingsScreenContent(uiState = uiState, onEvent = viewModel::onEvent)
}

@Composable
private fun SettingsScreenContent(uiState: SettingsUiState, onEvent: (SettingsUiEvent) -> Unit = {}) {
    Scaffold(topBar = { AppTopBar(title = stringResource(R.string.settings), subTitle = stringResource(R.string.preferences_and_customization), showTrailingIcon = false) }, containerColor = MaterialTheme.colorScheme.background) { paddingValues ->
        Column(Modifier.fillMaxSize().padding(top = paddingValues.calculateTopPadding()).verticalScroll(rememberScrollState()).padding(horizontal = dimens.horizontalPadding), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            SettingsSection(title = stringResource(R.string.appearance)) {
                SettingsItem(Icons.Default.Palette, colorResource(R.color.settings_theme_primary), colorResource(R.color.settings_theme_secondary).copy(alpha = 0.12f), stringResource(R.string.app_theme), stringResource(R.string.theme_options_desc), { onEvent(SettingsUiEvent.OnThemeCycle) }) { TrailingValue(when (uiState.appTheme) { 1 -> stringResource(R.string.light); 2 -> stringResource(R.string.dark); else -> stringResource(R.string.system) }, true) }
                SettingsDivider()
                SettingsItem(Icons.Default.FormatPaint, colorResource(R.color.settings_dynamic_primary), colorResource(R.color.settings_dynamic_secondary).copy(alpha = 0.12f), stringResource(R.string.dynamic_color), stringResource(R.string.match_system_wallpaper)) { CustomSwitch(uiState.dynamicColor) { onEvent(SettingsUiEvent.OnDynamicColorChanged(it)) } }
            }
            SettingsSection(title = stringResource(R.string.display)) {
                SettingsItem(Icons.Default.LockClock, colorResource(R.color.settings_lock_primary), colorResource(R.color.settings_lock_secondary).copy(alpha = 0.12f), stringResource(R.string.lock_screen_widget), stringResource(R.string.show_speed_on_lockscreen)) { CustomSwitch(uiState.lockScreenNotification) { onEvent(SettingsUiEvent.OnLockScreenNotificationChanged(it)) } }
                SettingsDivider()
                SettingsItem(Icons.Default.NotificationsActive, colorResource(R.color.settings_notification_primary), colorResource(R.color.settings_notification_secondary).copy(alpha = 0.12f), stringResource(R.string.notification_bar), stringResource(R.string.persistent_speed_monitor)) { CustomSwitch(uiState.showUploadSpeed) { onEvent(SettingsUiEvent.OnNotificationBarChanged(it)) } }
            }
            SettingsSection(title = stringResource(R.string.system)) {
                SettingsItem(Icons.Default.Info, colorResource(R.color.settings_usage_primary), colorResource(R.color.settings_usage_secondary).copy(alpha = 0.12f), stringResource(R.string.usage_access), stringResource(R.string.required_for_data_tracking), { onEvent(SettingsUiEvent.OnRequestUsagePermission) }) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                        if (uiState.hasUsagePermission) {
                            Box(Modifier.clip(RoundedCornerShape(7.dp)).background(colorResource(R.color.settings_usage_granted).copy(alpha = 0.12f)).padding(horizontal = 7.dp, vertical = 3.dp)) { Text(stringResource(R.string.granted), fontSize = 10.sp, color = colorResource(R.color.settings_usage_granted_text)) }
                        }
                        Icon(Icons.Default.ChevronRight, null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(19.dp))
                    }
                }
                SettingsDivider()
                SettingsItem(Icons.Default.BatteryChargingFull, colorResource(R.color.settings_battery_primary), colorResource(R.color.settings_battery_secondary).copy(alpha = 0.12f), stringResource(R.string.battery_optimization), stringResource(R.string.disable_for_accurate_monitoring), { onEvent(SettingsUiEvent.OnRequestBatteryOptimization) }) { CustomSwitch(uiState.isBatteryOptimizationDisabled) { onEvent(SettingsUiEvent.OnRequestBatteryOptimization) } }
                if (uiState.isAutoStartAvailable) {
                    SettingsDivider()
                    SettingsItem(Icons.Default.RocketLaunch, colorResource(R.color.settings_autostart_primary), colorResource(R.color.settings_autostart_secondary).copy(alpha = 0.12f), stringResource(R.string.auto_start), stringResource(R.string.launch_on_device_boot), { onEvent(SettingsUiEvent.OnRequestAutoStart) }) { Icon(Icons.Default.ChevronRight, null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(19.dp)) }
                }
            }
            Spacer(Modifier.height(10.dp))
        }
    }
}

@Composable
private fun TrailingValue(text: String, showChevron: Boolean = false) {
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(3.dp)) {
        Text(text, fontSize = 13.sp, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        if (showChevron) Icon(Icons.Default.ChevronRight, null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(19.dp))
    }
}

@Composable
private fun SettingsDivider() { HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.12f), modifier = Modifier.padding(horizontal = 14.dp)) }

@Composable
private fun SettingsSection(title: String, content: @Composable () -> Unit) {
    Column(Modifier.fillMaxWidth()) {
        Text(title.uppercase(), fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary.copy(alpha = 0.68f), letterSpacing = 1.sp, modifier = Modifier.padding(start = 4.dp, bottom = 5.dp))
        Column(Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.085f))) { content() }
    }
}

@Composable
private fun SettingsItem(icon: ImageVector, iconTint: Color, iconBgColor: Color, title: String, subtitle: String, onClick: (() -> Unit)? = null, trailingContent: @Composable () -> Unit) {
    Row(Modifier.fillMaxWidth().then(if (onClick != null) Modifier.clickable { onClick() } else Modifier).padding(horizontal = 14.dp, vertical = 10.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.SpaceBetween) {
        Row(Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            Box(Modifier.size(36.dp).clip(RoundedCornerShape(10.dp)).background(iconBgColor), contentAlignment = Alignment.Center) { Icon(icon, null, tint = iconTint, modifier = Modifier.size(20.dp)) }
            Column(verticalArrangement = Arrangement.spacedBy(1.dp)) {
                Text(title, fontWeight = FontWeight.SemiBold, fontSize = 14.sp, color = MaterialTheme.colorScheme.onSurface)
                Text(subtitle, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, maxLines = 1)
            }
        }
        trailingContent()
    }
}

@Composable
private fun CustomSwitch(checked: Boolean, onCheckedChange: (Boolean) -> Unit) { Switch(checked = checked, onCheckedChange = onCheckedChange, modifier = Modifier.scale(0.72f)) }
