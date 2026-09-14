package com.sipun.netspeedindicator.ui.screens.main.settings

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
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
import androidx.compose.material.icons.filled.DarkMode
import androidx.compose.material.icons.filled.DataUsage
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
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.core.graphics.drawable.toBitmap
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import com.sipun.netspeedindicator.R
import com.sipun.netspeedindicator.core.update.AppUpdate
import com.sipun.netspeedindicator.core.update.UpdateManager
import com.sipun.netspeedindicator.ui.components.AppTopBar
import com.sipun.netspeedindicator.ui.components.UpdateDialog
import com.sipun.netspeedindicator.ui.theme.dimens
import com.sipun.netspeedindicator.ui.util.LocalSnackBarHostState
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.sin

@Composable
fun SettingsScreen(viewModel: SettingsViewModel = hiltViewModel()) {
    val uiState by viewModel.uiState.collectAsState()
    val lifecycleOwner = LocalLifecycleOwner.current
    LaunchedEffect(lifecycleOwner) { lifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) { viewModel.onEvent(SettingsUiEvent.OnResume) } }
    SettingsScreenContent(uiState = uiState, onEvent = viewModel::onEvent)
}

@Composable
private fun SettingsScreenContent(uiState: SettingsUiState, onEvent: (SettingsUiEvent) -> Unit = {}) {
    val systemDarkTheme = isSystemInDarkTheme()
    val darkThemeEnabled = uiState.appTheme == 2 || (uiState.appTheme == 0 && systemDarkTheme)
    var showAboutDialog by remember { mutableStateOf(false) }
    var manualUpdate by remember { mutableStateOf<AppUpdate?>(null) }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val snackbarHostState = LocalSnackBarHostState.current
    val versionName = remember { context.packageManager.getPackageInfo(context.packageName, 0).versionName.orEmpty() }
    val appIcon = remember { context.packageManager.getApplicationIcon(context.applicationInfo).toBitmap().asImageBitmap() }

    Scaffold(topBar = { AppTopBar(title = stringResource(R.string.settings), subTitle = stringResource(R.string.preferences_and_customization), showTrailingIcon = false) }, containerColor = MaterialTheme.colorScheme.background) { paddingValues ->
        Column(Modifier.fillMaxSize().padding(top = paddingValues.calculateTopPadding()).verticalScroll(rememberScrollState()).padding(horizontal = dimens.horizontalPadding), verticalArrangement = Arrangement.spacedBy(14.dp)) {
            SettingsSection(title = stringResource(R.string.appearance)) {
                SettingsItem(Icons.Default.Palette, MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.primary.copy(alpha = 0.12f), stringResource(R.string.app_theme), stringResource(R.string.theme_options_desc), if (uiState.pureBlackTheme) null else ({ onEvent(SettingsUiEvent.OnThemeCycle) })) { TrailingValue(when (uiState.appTheme) { 1 -> stringResource(R.string.light); 2 -> stringResource(R.string.dark); else -> stringResource(R.string.system) }, true) }
                SettingsDivider()
                SettingsItem(Icons.Default.DarkMode, MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.primary.copy(alpha = 0.12f), stringResource(R.string.pure_black_theme), stringResource(R.string.use_true_black_background), trailingContent = { CustomSwitch(uiState.pureBlackTheme, enabled = darkThemeEnabled) { onEvent(SettingsUiEvent.OnPureBlackThemeChanged(it)) } })
                SettingsDivider()
                SettingsItem(Icons.Default.FormatPaint, MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.primary.copy(alpha = 0.12f), stringResource(R.string.dynamic_color), stringResource(R.string.match_system_wallpaper), trailingContent = { CustomSwitch(uiState.dynamicColor, enabled = !uiState.pureBlackTheme) { onEvent(SettingsUiEvent.OnDynamicColorChanged(it)) } })
            }
            SettingsSection(title = stringResource(R.string.display)) {
                SettingsItem(Icons.Default.LockClock, MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.primary.copy(alpha = 0.12f), stringResource(R.string.lock_screen_widget), stringResource(R.string.show_speed_on_lockscreen)) { CustomSwitch(uiState.lockScreenNotification) { onEvent(SettingsUiEvent.OnLockScreenNotificationChanged(it)) } }
                SettingsDivider()
                SettingsItem(Icons.Default.NotificationsActive, MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.primary.copy(alpha = 0.12f), stringResource(R.string.notification_bar), stringResource(R.string.persistent_speed_monitor)) { CustomSwitch(uiState.showUploadSpeed) { onEvent(SettingsUiEvent.OnNotificationBarChanged(it)) } }
            }
            SettingsSection(title = stringResource(R.string.system)) {
                SettingsItem(Icons.Default.DataUsage, MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.primary.copy(alpha = 0.12f), stringResource(R.string.usage_access), stringResource(R.string.required_for_data_tracking), { onEvent(SettingsUiEvent.OnRequestUsagePermission) }) {
                    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                        Box(Modifier.clip(RoundedCornerShape(7.dp)).background(if (uiState.hasUsagePermission) MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.55f) else MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.55f)).padding(horizontal = 7.dp, vertical = 3.dp)) {
                            Text(stringResource(if (uiState.hasUsagePermission) R.string.granted else R.string.not_granted), fontSize = 10.sp, color = if (uiState.hasUsagePermission) MaterialTheme.colorScheme.onPrimaryContainer else MaterialTheme.colorScheme.onErrorContainer)
                        }
                        Icon(Icons.Default.ChevronRight, null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(19.dp))
                    }
                }
                SettingsDivider()
                SettingsItem(Icons.Default.BatteryChargingFull, MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.primary.copy(alpha = 0.12f), stringResource(R.string.battery_optimization), stringResource(R.string.disable_for_accurate_monitoring), { onEvent(SettingsUiEvent.OnRequestBatteryOptimization) }) { CustomSwitch(uiState.isBatteryOptimizationDisabled) { onEvent(SettingsUiEvent.OnRequestBatteryOptimization) } }
                if (uiState.isAutoStartAvailable) {
                    SettingsDivider()
                    SettingsItem(Icons.Default.RocketLaunch, MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.primary.copy(alpha = 0.12f), stringResource(R.string.auto_start), stringResource(R.string.launch_on_device_boot)) { Icon(Icons.Default.ChevronRight, null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(19.dp)) }
                }
            }
            SettingsSection(title = stringResource(R.string.about)) {
                SettingsItem(Icons.Default.Info, MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.primary.copy(alpha = 0.12f), stringResource(R.string.about_app_title), stringResource(R.string.about_app_desc), { showAboutDialog = true }) {
                    Icon(Icons.Default.ChevronRight, null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(19.dp))
                }
            }
            Spacer(Modifier.height(10.dp))
        }
    }

    if (showAboutDialog) {
        Dialog(onDismissRequest = { showAboutDialog = false }) {
            Surface(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(28.dp),
                color = MaterialTheme.colorScheme.surfaceVariant
            ) {
                Column(
                    modifier = Modifier.padding(horizontal = 24.dp, vertical = 28.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(14.dp)
                ) {
                    Image(
                        bitmap = appIcon,
                        contentDescription = stringResource(R.string.app_name),
                        modifier = Modifier.size(68.dp).clip(RoundedCornerShape(18.dp))
                    )
                    Text(stringResource(R.string.app_name), fontWeight = FontWeight.Bold, fontSize = 22.sp)
                    Text(stringResource(R.string.about_app_tagline), color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 14.sp)
                    SpeedWave()
                    Text(
                        stringResource(R.string.version_format, versionName),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 13.sp,
                        modifier = Modifier.clickable {
                            scope.launch {
                                val update = withContext(Dispatchers.IO) { UpdateManager.findLatestUpdate(context) }
                                if (update != null) {
                                    UpdateManager.savePendingUpdate(context, update)
                                    manualUpdate = update
                                } else {
                                    snackbarHostState.showSnackbar("No updates available.")
                                }
                            }
                        }
                    )
                    AnimatedHeartCredit()
                }
            }
        }
    }

    manualUpdate?.let { update ->
        UpdateDialog(
            update = update,
            appIcon = appIcon,
            onDownload = {
                UpdateManager.enqueueDownload(context, update)
                manualUpdate = null
                snackbarHostState.currentSnackbarData?.dismiss()
                scope.launch { snackbarHostState.showSnackbar(context.getString(R.string.update_download_started)) }
            },
            onDismiss = { manualUpdate = null }
        )
    }
}

@Composable
private fun SpeedWave() {
    val transition = rememberInfiniteTransition(label = "speedWave")
    val phase by transition.animateFloat(0f, (2f * kotlin.math.PI).toFloat(), infiniteRepeatable(tween(4500, easing = LinearEasing), RepeatMode.Restart), label = "speedWavePhase")
    val waveColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.78f)
    val waveFillColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.10f)
    Canvas(Modifier.fillMaxWidth().height(44.dp).padding(horizontal = 4.dp)) {
        val centerY = size.height * .5f
        val amplitude = size.height * .28f
        val step = size.width / 159f
        val path = Path()
        for (i in 0 until 160) {
            val x = i * step
            val y = centerY - sin(i / 159f * 2.15f * 2f * kotlin.math.PI.toFloat() + phase) * amplitude
            if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }
        val fill = Path().apply { moveTo(0f, size.height); addPath(path); lineTo(size.width, size.height); close() }
        drawPath(fill, waveFillColor)
        drawPath(path, waveColor, style = androidx.compose.ui.graphics.drawscope.Stroke(1.8.dp.toPx(), cap = androidx.compose.ui.graphics.StrokeCap.Round, join = androidx.compose.ui.graphics.StrokeJoin.Round))
    }
}

@Composable
private fun AnimatedHeartCredit() {
    val transition = rememberInfiniteTransition(label = "heart")
    val scale by transition.animateFloat(0.92f, 1.08f, infiniteRepeatable(tween(650, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "heartScale")
    Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.Center) {
        Text(stringResource(R.string.made_with_prefix), fontWeight = FontWeight.Medium, fontSize = 15.sp)
        Text(" ❤️ ", fontSize = 18.sp, modifier = Modifier.scale(scale))
        Text(stringResource(R.string.made_with_suffix), fontWeight = FontWeight.Medium, fontSize = 15.sp)
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
        Text(title.uppercase(), fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurfaceVariant, letterSpacing = 1.sp, modifier = Modifier.padding(start = 4.dp, bottom = 5.dp))
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
private fun CustomSwitch(checked: Boolean, enabled: Boolean = true, onCheckedChange: (Boolean) -> Unit) { Switch(checked = checked, enabled = enabled, onCheckedChange = onCheckedChange, modifier = Modifier.scale(0.72f)) }
