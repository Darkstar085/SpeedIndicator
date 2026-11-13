package com.sipun.netspeedindicator.ui.screens.main.settings

import android.content.Intent
import android.net.Uri
import android.os.Build
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.BatteryChargingFull
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ChevronRight
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Code
import androidx.compose.material.icons.filled.DataUsage
import androidx.compose.material.icons.filled.FormatPaint
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LockClock
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.RocketLaunch
import androidx.compose.material.icons.filled.Security
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.TextButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.graphics.drawable.toBitmap
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import com.airbnb.lottie.compose.LottieAnimation
import com.airbnb.lottie.compose.LottieCompositionSpec
import com.airbnb.lottie.compose.animateLottieCompositionAsState
import com.airbnb.lottie.compose.rememberLottieComposition
import com.sipun.netspeedindicator.R
import com.sipun.netspeedindicator.data.preferences.AccentGradient
import com.sipun.netspeedindicator.data.preferences.AccentStyle
import com.sipun.netspeedindicator.core.update.AppUpdate
import com.sipun.netspeedindicator.core.update.UpdateManager
import com.sipun.netspeedindicator.ui.components.AppCard
import com.sipun.netspeedindicator.ui.components.AppTopBar
import com.sipun.netspeedindicator.ui.components.UpdateFlowDialog
import com.sipun.netspeedindicator.ui.theme.AppDialogTheme
import com.sipun.netspeedindicator.ui.theme.LocalAccentBrush
import com.sipun.netspeedindicator.ui.theme.LocalAccentContainerBrush
import com.sipun.netspeedindicator.ui.theme.accentGradientColors
import com.sipun.netspeedindicator.ui.theme.dimens
import com.sipun.netspeedindicator.ui.util.AutoHideBottomBarEffect
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlin.math.sin

@Composable
fun SettingsScreen(viewModel: SettingsViewModel = hiltViewModel()) {
    val uiState by viewModel.uiState.collectAsState()
    val lifecycleOwner = LocalLifecycleOwner.current
    LaunchedEffect(lifecycleOwner) {
        lifecycleOwner.lifecycle.repeatOnLifecycle(Lifecycle.State.RESUMED) {
            viewModel.onEvent(
                SettingsUiEvent.OnResume
            )
        }
    }
    SettingsScreenContent(uiState = uiState, onEvent = viewModel::onEvent)
}

@Composable
private fun SettingsScreenContent(
    uiState: SettingsUiState,
    onEvent: (SettingsUiEvent) -> Unit = {}
) {
    val systemDarkTheme = isSystemInDarkTheme()
    val darkThemeEnabled = uiState.appTheme == 2 || (uiState.appTheme == 0 && systemDarkTheme)
    var showThemeDialog by remember { mutableStateOf(false) }
    var showAboutDialog by remember { mutableStateOf(false) }
    var manualUpdate by remember { mutableStateOf<AppUpdate?>(null) }
    var showNoUpdateDialog by remember { mutableStateOf(false) }
    val context = LocalContext.current
    val scope = rememberCoroutineScope()
    val versionName = remember {
        context.packageManager.getPackageInfo(
            context.packageName,
            0
        ).versionName.orEmpty()
    }
    val appIcon = remember {
        context.packageManager.getApplicationIcon(context.applicationInfo).toBitmap()
            .asImageBitmap()
    }

    Scaffold(
        contentWindowInsets = WindowInsets(0),
        topBar = {
            AppTopBar(
                title = stringResource(R.string.settings),
                subTitle = stringResource(R.string.preferences_and_customization),
                showTrailingIcon = false
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        val scrollState = rememberScrollState()
        AutoHideBottomBarEffect(scrollState)
        val navBarBottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
        Column(
            Modifier
                .fillMaxSize()
                .padding(top = paddingValues.calculateTopPadding())
                .verticalScroll(scrollState)
                .padding(horizontal = dimens.horizontalPadding)
                .padding(bottom = dimens.verticalPadding + navBarBottom),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            SettingsSection(title = stringResource(R.string.appearance)) {
                SettingsItem(
                    Icons.Default.Palette,
                    MaterialTheme.colorScheme.primary,
                    stringResource(R.string.app_theme),
                    stringResource(R.string.theme_options_desc),
                    { showThemeDialog = true }
                ) {
                    TrailingValue(
                        stringResource(
                            when {
                                uiState.pureBlackTheme && darkThemeEnabled -> R.string.pure_black_theme
                                uiState.appTheme == 1 -> R.string.light
                                uiState.appTheme == 2 -> R.string.dark
                                else -> R.string.system
                            }
                        ),
                        true
                    )
                }
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                    SettingsDivider()
                    SettingsItem(
                        Icons.Default.FormatPaint,
                        MaterialTheme.colorScheme.primary,
                        stringResource(R.string.dynamic_color),
                        stringResource(R.string.dynamic_color_desc)
                    ) {
                        CustomSwitch(uiState.dynamicColor) {
                            onEvent(SettingsUiEvent.OnDynamicColorChanged(it))
                        }
                    }
                }
                if (!uiState.dynamicColor || Build.VERSION.SDK_INT < Build.VERSION_CODES.S) {
                    SettingsDivider()
                    CustomColorSelector(
                        color = Color(uiState.customColor),
                        style = uiState.accentStyle,
                        gradient = uiState.accentGradient,
                        customGradientStart = uiState.customGradientStart,
                        customGradientEnd = uiState.customGradientEnd,
                        onColorChange = { onEvent(SettingsUiEvent.OnCustomColorChanged(it)) },
                        onStyleChange = { onEvent(SettingsUiEvent.OnAccentStyleChanged(it)) },
                        onGradientChange = { onEvent(SettingsUiEvent.OnAccentGradientChanged(it)) },
                        onCustomGradientChange = { start, end ->
                            onEvent(SettingsUiEvent.OnCustomGradientChanged(start, end))
                        }
                    )
                }
            }
            SettingsSection(title = stringResource(R.string.display)) {
                SettingsItem(
                    Icons.Default.LockClock,
                    MaterialTheme.colorScheme.primary,
                    stringResource(R.string.lock_screen_notification),
                    stringResource(R.string.show_speed_on_lockscreen)
                ) {
                    CustomSwitch(uiState.lockScreenNotification) {
                        onEvent(
                            SettingsUiEvent.OnLockScreenNotificationChanged(
                                it
                            )
                        )
                    }
                }
                SettingsDivider()
                SettingsItem(
                    Icons.Default.NotificationsActive,
                    MaterialTheme.colorScheme.primary,
                    stringResource(R.string.notification_bar),
                    stringResource(R.string.persistent_speed_monitor)
                ) {
                    CustomSwitch(uiState.showUploadSpeed) {
                        onEvent(
                            SettingsUiEvent.OnNotificationBarChanged(
                                it
                            )
                        )
                    }
                }
            }
            SettingsSection(title = stringResource(R.string.system)) {
                SettingsItem(
                    Icons.Default.DataUsage,
                    MaterialTheme.colorScheme.primary,
                    stringResource(R.string.usage_access),
                    stringResource(R.string.required_for_data_tracking),
                    { onEvent(SettingsUiEvent.OnRequestUsagePermission) }) {
                    StatusChip(
                        text = stringResource(if (uiState.hasUsagePermission) R.string.granted else R.string.not_granted),
                        positive = uiState.hasUsagePermission
                    )
                }
                SettingsDivider()
                SettingsItem(
                    Icons.Default.Security,
                    MaterialTheme.colorScheme.primary,
                    stringResource(R.string.install_unknown_apps),
                    stringResource(R.string.allow_install_unknown_apps),
                    { onEvent(SettingsUiEvent.OnRequestInstallUnknownApps) }) {
                    StatusChip(
                        text = stringResource(if (uiState.canInstallUnknownApps) R.string.allowed else R.string.not_allowed),
                        positive = uiState.canInstallUnknownApps
                    )
                }
                SettingsDivider()
                SettingsItem(
                    Icons.Default.BatteryChargingFull,
                    MaterialTheme.colorScheme.primary,
                    stringResource(R.string.battery_optimization),
                    stringResource(R.string.disable_for_accurate_monitoring),
                    { onEvent(SettingsUiEvent.OnRequestBatteryOptimization) }) {
                    CustomSwitch(
                        uiState.isBatteryOptimizationDisabled
                    ) { onEvent(SettingsUiEvent.OnRequestBatteryOptimization) }
                }
                if (uiState.isAutoStartAvailable) {
                    SettingsDivider()
                    SettingsItem(
                        Icons.Default.RocketLaunch,
                        MaterialTheme.colorScheme.primary,
                            stringResource(R.string.auto_start),
                        stringResource(R.string.launch_on_device_boot)
                    ) {
                        Icon(
                            Icons.Default.ChevronRight,
                            null,
                            tint = MaterialTheme.colorScheme.onSurfaceVariant,
                            modifier = Modifier.size(19.dp)
                        )
                    }
                }
            }
            SettingsSection(title = stringResource(R.string.about)) {
                SettingsItem(
                    Icons.Default.Info,
                    MaterialTheme.colorScheme.primary,
                    stringResource(R.string.about_app_title),
                    stringResource(R.string.about_app_desc),
                    { showAboutDialog = true }
                ) {
                    Icon(
                        Icons.Default.ChevronRight,
                        null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(19.dp)
                    )
                }
                SettingsDivider()
                SettingsItem(
                    Icons.Default.Code,
                    MaterialTheme.colorScheme.primary,
                    stringResource(R.string.github_source),
                    stringResource(R.string.github_source_desc),
                    {
                        context.startActivity(
                            Intent(
                                Intent.ACTION_VIEW,
                                Uri.parse("https://github.com/Darkstar085/SpeedIndicator")
                            )
                        )
                    }
                ) {
                    Icon(
                        Icons.Default.ChevronRight,
                        null,
                        tint = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.size(19.dp)
                    )
                }
            }
        }
    }

    AppDialogTheme {
        if (showThemeDialog) {
            val selectedTheme =
                if (uiState.pureBlackTheme && darkThemeEnabled) 3 else uiState.appTheme
            val themeOptions = listOf(
                Triple(0, R.string.system, R.string.theme_system_desc),
                Triple(1, R.string.light, R.string.theme_light_desc),
                Triple(2, R.string.dark, R.string.theme_dark_desc),
                Triple(3, R.string.pure_black_theme, R.string.theme_pure_black_desc)
            )
            AlertDialog(
                onDismissRequest = { showThemeDialog = false },
                title = {
                    Column(verticalArrangement = Arrangement.spacedBy(5.dp)) {
                        Text(
                            text = stringResource(R.string.choose_app_theme),
                            fontWeight = FontWeight.Bold,
                            fontSize = 21.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            text = stringResource(R.string.choose_app_theme_desc),
                            fontSize = 13.sp,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                },
                text = {
                    Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
                        themeOptions.forEach { (theme, label, description) ->
                            val selected = selectedTheme == theme
                            val swatchColor = when (theme) {
                                1 -> Color(0xFFF4F5F8)
                                2 -> Color(0xFF263247)
                                3 -> Color.Black
                                else -> MaterialTheme.colorScheme.surfaceVariant
                            }
                            Row(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .clip(RoundedCornerShape(16.dp))
                                    .background(
                                        if (selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                                        else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.42f)
                                    )
                                    .clickable {
                                        onEvent(SettingsUiEvent.OnThemeSelected(theme))
                                        showThemeDialog = false
                                    }
                                    .padding(horizontal = 12.dp, vertical = 10.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(12.dp)
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(38.dp)
                                        .clip(RoundedCornerShape(12.dp))
                                        .background(swatchColor)
                                        .border(
                                            width = 1.dp,
                                            color = MaterialTheme.colorScheme.outline.copy(alpha = 0.35f),
                                            shape = RoundedCornerShape(12.dp)
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (theme == 0) {
                                        Row(horizontalArrangement = Arrangement.spacedBy(2.dp)) {
                                            Box(
                                                Modifier
                                                    .size(8.dp)
                                                    .clip(RoundedCornerShape(3.dp))
                                                    .background(Color(0xFFF4F5F8))
                                            )
                                            Box(
                                                Modifier
                                                    .size(8.dp)
                                                    .clip(RoundedCornerShape(3.dp))
                                                    .background(Color(0xFF263247))
                                            )
                                        }
                                    } else {
                                        Box(
                                            Modifier
                                                .size(13.dp)
                                                .clip(RoundedCornerShape(50))
                                                .background(
                                                    when (theme) {
                                                        1 -> Color(0xFF397CF6)
                                                        2 -> Color(0xFF8CB4FF)
                                                        else -> Color(0xFF397CF6)
                                                    }
                                                )
                                        )
                                    }
                                }
                                Column(
                                    modifier = Modifier.weight(1f),
                                    verticalArrangement = Arrangement.spacedBy(2.dp)
                                ) {
                                    Text(
                                        text = stringResource(label),
                                        fontWeight = if (selected) FontWeight.SemiBold else FontWeight.Medium,
                                        fontSize = 15.sp,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )
                                    Text(
                                        text = stringResource(description),
                                        fontSize = 12.sp,
                                        color = MaterialTheme.colorScheme.onSurfaceVariant
                                    )
                                }
                                RadioButton(
                                    selected = selected,
                                    onClick = {
                                        onEvent(SettingsUiEvent.OnThemeSelected(theme))
                                        showThemeDialog = false
                                    }
                                )
                            }
                        }
                    }
                },
                confirmButton = {},
                shape = RoundedCornerShape(28.dp),
                containerColor = MaterialTheme.colorScheme.surface,
                tonalElevation = 0.dp
            )
        }

        if (showAboutDialog) {
            Dialog(
                onDismissRequest = { showAboutDialog = false },
                properties = DialogProperties(usePlatformDefaultWidth = false)
            ) {
                Surface(
                    modifier = Modifier
                        .fillMaxWidth(0.9f)
                        .wrapContentHeight(),
                    shape = RoundedCornerShape(28.dp),
                    color = MaterialTheme.colorScheme.surface,
                    tonalElevation = 0.dp
                ) {
                    Column(
                        modifier = Modifier.padding(horizontal = 26.dp, vertical = 24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally,
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Image(
                            bitmap = appIcon,
                            contentDescription = stringResource(R.string.app_name),
                            modifier = Modifier
                                .size(64.dp)
                                .clip(RoundedCornerShape(17.dp))
                        )
                        Text(
                            stringResource(R.string.app_name),
                            fontWeight = FontWeight.Bold,
                            fontSize = 22.sp,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        Text(
                            stringResource(R.string.about_app_tagline),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 14.sp
                        )
                        SpeedWave()
                        Text(
                            stringResource(R.string.version_format, versionName),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                            fontSize = 13.sp,
                            modifier = Modifier.clickable {
                                scope.launch {
                                    val update = withContext(Dispatchers.IO) {
                                        UpdateManager.findLatestUpdate(context)
                                    }
                                    if (update != null) {
                                        UpdateManager.savePendingUpdate(context, update)
                                        manualUpdate = update
                                    } else {
                                        showNoUpdateDialog = true
                                    }
                                }
                            }
                        )
                        AnimatedHeartCredit()
                    }
                }
            }
        }

        if (showNoUpdateDialog) {
            AlertDialog(
                onDismissRequest = { showNoUpdateDialog = false },
                icon = {
                    Icon(
                        Icons.Default.CheckCircle,
                        contentDescription = null,
                        tint = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.size(40.dp)
                    )
                },
                title = {
                    Text(
                        text = stringResource(R.string.no_update_available_title),
                        fontWeight = FontWeight.Bold,
                        fontSize = 20.sp,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                },
                text = {
                    Text(
                        text = stringResource(R.string.no_update_available_message, versionName),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        fontSize = 14.sp,
                        textAlign = androidx.compose.ui.text.style.TextAlign.Center
                    )
                },
                confirmButton = {
                    Button(
                        onClick = { showNoUpdateDialog = false },
                        shape = RoundedCornerShape(13.dp),
                        colors = androidx.compose.material3.ButtonDefaults.buttonColors(
                            containerColor = MaterialTheme.colorScheme.primary,
                            contentColor = MaterialTheme.colorScheme.onPrimary
                        )
                    ) {
                        Text(stringResource(R.string.ok), fontWeight = FontWeight.SemiBold)
                    }
                },
                shape = RoundedCornerShape(26.dp),
                containerColor = MaterialTheme.colorScheme.surface,
                tonalElevation = 0.dp
            )
        }

        manualUpdate?.let { update ->
            UpdateFlowDialog(
                update = update,
                appIcon = appIcon,
                onDismiss = { manualUpdate = null }
            )
        }
    }
}

@Composable
private fun SpeedWave() {
    val transition = rememberInfiniteTransition(label = "speedWave")
    val phase by transition.animateFloat(
        0f,
        (2f * kotlin.math.PI).toFloat(),
        infiniteRepeatable(tween(4500, easing = LinearEasing), RepeatMode.Restart),
        label = "speedWavePhase"
    )
    val waveColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.78f)
    val waveFillColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.10f)
    Canvas(
        Modifier
            .fillMaxWidth()
            .height(38.dp)
            .padding(horizontal = 4.dp)
    ) {
        val centerY = size.height * .5f
        val amplitude = size.height * .28f
        val step = size.width / 159f
        val path = Path()
        for (i in 0 until 160) {
            val x = i * step
            val y =
                centerY - sin(i / 159f * 2.15f * 2f * kotlin.math.PI.toFloat() + phase) * amplitude
            if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }
        val fill = Path().apply {
            moveTo(0f, size.height); addPath(path); lineTo(
            size.width,
            size.height
        ); close()
        }
        drawPath(fill, waveFillColor)
        drawPath(
            path,
            waveColor,
            style = androidx.compose.ui.graphics.drawscope.Stroke(
                1.8.dp.toPx(),
                cap = androidx.compose.ui.graphics.StrokeCap.Round,
                join = androidx.compose.ui.graphics.StrokeJoin.Round
            )
        )
    }
}

@Composable
private fun AnimatedHeartCredit() {
    val loveComposition by rememberLottieComposition(
        LottieCompositionSpec.RawRes(R.raw.love)
    )
    val loveProgress by animateLottieCompositionAsState(
        composition = loveComposition,
        iterations = Int.MAX_VALUE
    )

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp)
    ) {
        Text(
            text = stringResource(R.string.made_with_prefix),
            fontWeight = FontWeight.Medium,
            fontSize = 15.sp,
            color = MaterialTheme.colorScheme.onSurface
        )
        LottieAnimation(
            composition = loveComposition,
            progress = { loveProgress },
            modifier = Modifier.size(28.dp)
        )
        Text(
            text = stringResource(R.string.made_with_suffix),
            fontWeight = FontWeight.Medium,
            fontSize = 15.sp,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
private fun StatusChip(text: String, positive: Boolean) {
    val containerColor = if (positive) {
        MaterialTheme.colorScheme.primaryContainer.copy(alpha = 0.52f)
    } else {
        MaterialTheme.colorScheme.errorContainer.copy(alpha = 0.52f)
    }
    val contentColor = if (positive) {
        MaterialTheme.colorScheme.onPrimaryContainer
    } else {
        MaterialTheme.colorScheme.onErrorContainer
    }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(4.dp),
        modifier = Modifier
            .clip(RoundedCornerShape(9.dp))
            .background(containerColor)
            .padding(horizontal = 8.dp, vertical = 5.dp)
    ) {
        Icon(
            imageVector = if (positive) Icons.Default.Check else Icons.Default.Close,
            contentDescription = null,
            tint = contentColor,
            modifier = Modifier.size(13.dp)
        )
        Text(
            text = text,
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium,
            color = contentColor
        )
        Icon(
            Icons.Default.ChevronRight,
            null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(18.dp)
        )
    }
}

@Composable
private fun TrailingValue(text: String, showChevron: Boolean = false) {
    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(3.dp)
    ) {
        Text(
            text,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        if (showChevron) Icon(
            Icons.Default.ChevronRight,
            null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.size(19.dp)
        )
    }
}

@Composable
private fun SettingsDivider() {
    HorizontalDivider(
        color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.12f),
        modifier = Modifier.padding(horizontal = 14.dp)
    )
}

@Composable
private fun SettingsSection(title: String, content: @Composable () -> Unit) {
    Column(Modifier.fillMaxWidth()) {
        Text(
            title.uppercase(),
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            letterSpacing = 1.sp,
            modifier = Modifier.padding(start = 4.dp, bottom = 5.dp)
        )
        AppCard(Modifier.fillMaxWidth()) { content() }
    }
}

@Composable
private fun SettingsItem(
    icon: ImageVector,
    iconTint: Color,
    title: String,
    subtitle: String,
    onClick: (() -> Unit)? = null,
    trailingContent: @Composable () -> Unit
) {
    Row(
        Modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable { onClick() } else Modifier)
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween) {
        Row(
            Modifier.weight(1f),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                Modifier
                    .size(36.dp)
                    .clip(RoundedCornerShape(10.dp))
                    .background(LocalAccentContainerBrush.current),
                contentAlignment = Alignment.Center
            ) { Icon(icon, null, tint = iconTint, modifier = Modifier.size(20.dp)) }
            Column(verticalArrangement = Arrangement.spacedBy(1.dp)) {
                Text(
                    title,
                    fontWeight = FontWeight.SemiBold,
                    fontSize = 14.sp,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    subtitle,
                    fontSize = 11.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    maxLines = 1
                )
            }
        }
        trailingContent()
    }
}

@Composable
private fun CustomSwitch(
    checked: Boolean,
    enabled: Boolean = true,
    onCheckedChange: (Boolean) -> Unit
) {
    Switch(
        checked = checked,
        enabled = enabled,
        onCheckedChange = onCheckedChange,
        modifier = Modifier.scale(0.72f)
    )
}


@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun CustomColorSelector(
    color: Color,
    style: AccentStyle,
    gradient: AccentGradient,
    customGradientStart: Int,
    customGradientEnd: Int,
    onColorChange: (Int) -> Unit,
    onStyleChange: (AccentStyle) -> Unit,
    onGradientChange: (AccentGradient) -> Unit,
    onCustomGradientChange: (Int, Int) -> Unit
) {
    val presetColors = listOf(
        Color(0xFFE92B2B), Color(0xFFE57373), Color(0xFF42A5F5),
        Color(0xFF26A69A), Color(0xFF66BB6A), Color(0xFF9CCC65),
        Color(0xFFFFB74D), Color(0xFFFF7043), Color(0xFFAB47BC),
        Color(0xFF7E57C2), Color(0xFF5C6BC0), Color(0xFF26C6DA),
        Color(0xFF8BC34A), Color(0xFFFFCA28), Color(0xFFFFA726)
    )
    val gradients = AccentGradient.entries.filter { it != AccentGradient.CUSTOM }
    val hsv = remember(color) {
        FloatArray(3).also { android.graphics.Color.colorToHSV(color.toArgb(), it) }
    }
    var hue by remember(color) { mutableStateOf(hsv[0]) }
    var showCustomGradientEditor by remember { mutableStateOf(false) }
    val startHsv = remember(customGradientStart) {
        FloatArray(3).also { android.graphics.Color.colorToHSV(customGradientStart, it) }
    }
    val endHsv = remember(customGradientEnd) {
        FloatArray(3).also { android.graphics.Color.colorToHSV(customGradientEnd, it) }
    }
    var customStartHue by remember(showCustomGradientEditor, customGradientStart) {
        mutableStateOf(startHsv[0])
    }
    var customEndHue by remember(showCustomGradientEditor, customGradientEnd) {
        mutableStateOf(endHsv[0])
    }

    Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp)) {
        Text(
            stringResource(R.string.custom_color),
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurface
        )
        Text(
            stringResource(R.string.custom_color_desc),
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(top = 2.dp)
        )
        Row(
            Modifier.fillMaxWidth().padding(top = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            AccentStyle.entries.forEach { option ->
                val selected = style == option
                Surface(
                    modifier = Modifier.weight(1f).then(
                        if (selected) Modifier.background(
                            if (option == AccentStyle.GRADIENT) LocalAccentBrush.current
                            else Brush.horizontalGradient(listOf(MaterialTheme.colorScheme.primary, MaterialTheme.colorScheme.primary)),
                            RoundedCornerShape(14.dp)
                        ) else Modifier
                    ).clickable { onStyleChange(option) },
                    shape = RoundedCornerShape(14.dp),
                    color = if (selected) Color.Transparent else MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.45f),
                    border = BorderStroke(
                        1.dp,
                        if (selected) MaterialTheme.colorScheme.primary
                        else MaterialTheme.colorScheme.outline.copy(alpha = 0.3f)
                    )
                ) {
                    Text(
                        text = if (option == AccentStyle.SOLID) stringResource(R.string.solid_color)
                        else stringResource(R.string.gradient),
                        modifier = Modifier.padding(vertical = 11.dp),
                        textAlign = TextAlign.Center,
                        color = if (selected) Color.White else MaterialTheme.colorScheme.onSurface,
                        style = MaterialTheme.typography.labelLarge
                    )
                }
            }
        }

        if (style == AccentStyle.GRADIENT) {
            Text(
                stringResource(R.string.choose_gradient),
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(top = 10.dp, bottom = 2.dp)
            )
            gradients.chunked(4).forEach { rowGradients ->
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    rowGradients.forEach { preset ->
                        val colors = accentGradientColors(preset)
                        val selected = gradient == preset
                        Column(
                            Modifier.weight(1f).clip(RoundedCornerShape(12.dp))
                                .clickable { onGradientChange(preset) }.padding(bottom = 3.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Box(
                                Modifier.fillMaxWidth().height(48.dp).clip(RoundedCornerShape(12.dp))
                                    .background(Brush.horizontalGradient(colors))
                                    .then(if (selected) Modifier.border(2.dp, MaterialTheme.colorScheme.onSurface, RoundedCornerShape(12.dp)) else Modifier),
                                contentAlignment = Alignment.Center
                            ) {
                                if (selected) {
                                    Icon(Icons.Default.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(22.dp))
                                }
                            }
                            Text(
                                preset.name.lowercase().replace('_', ' ').replaceFirstChar { it.uppercase() },
                                color = if (selected) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurfaceVariant,
                                style = MaterialTheme.typography.labelSmall,
                                maxLines = 1,
                                softWrap = false,
                                modifier = Modifier.padding(top = 4.dp)
                            )
                        }
                    }
                    repeat(4 - rowGradients.size) { Spacer(Modifier.weight(1f)) }
                }
            }
            Surface(
                modifier = Modifier.fillMaxWidth().padding(top = 8.dp).clickable { showCustomGradientEditor = true },
                shape = RoundedCornerShape(14.dp),
                color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.35f),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outline.copy(alpha = 0.25f))
            ) {
                Row(
                    Modifier.padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    Box(
                        Modifier.size(width = 64.dp, height = 42.dp).clip(RoundedCornerShape(10.dp))
                            .background(Brush.horizontalGradient(listOf(Color(customGradientStart), Color(customGradientEnd))))
                    )
                    Column(Modifier.weight(1f)) {
                        Text(stringResource(R.string.custom_gradient), style = MaterialTheme.typography.labelLarge, color = MaterialTheme.colorScheme.onSurface)
                        Text(stringResource(R.string.custom_gradient_desc), style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    }
                    Text(stringResource(R.string.edit), color = MaterialTheme.colorScheme.primary, style = MaterialTheme.typography.labelLarge)
                }
            }
            if (showCustomGradientEditor) {
                val startColor = Color.hsv(customStartHue, 0.85f, 0.95f)
                val endColor = Color.hsv(customEndHue, 0.85f, 0.95f)
                AlertDialog(
                    onDismissRequest = { showCustomGradientEditor = false },
                    title = { Text(stringResource(R.string.custom_gradient)) },
                    text = {
                        Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                            Box(
                                Modifier.fillMaxWidth().height(76.dp).clip(RoundedCornerShape(14.dp))
                                    .background(Brush.horizontalGradient(listOf(startColor, endColor)))
                            )
                            Text(stringResource(R.string.start_color), style = MaterialTheme.typography.labelMedium)
                            HueSlider(customStartHue) { customStartHue = it }
                            Text(stringResource(R.string.end_color), style = MaterialTheme.typography.labelMedium)
                            HueSlider(customEndHue) { customEndHue = it }
                        }
                    },
                    confirmButton = {
                        TextButton(onClick = {
                            onCustomGradientChange(startColor.toArgb(), endColor.toArgb())
                            onGradientChange(AccentGradient.CUSTOM)
                            onStyleChange(AccentStyle.GRADIENT)
                            showCustomGradientEditor = false
                        }) { Text(stringResource(R.string.apply_gradient)) }
                    },
                    dismissButton = {
                        TextButton(onClick = { showCustomGradientEditor = false }) {
                            Text(stringResource(R.string.cancel))
                        }
                    }
                )
            }
        } else {
            Row(
                Modifier.fillMaxWidth().horizontalScroll(rememberScrollState()).padding(top = 12.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                presetColors.forEach { preset ->
                    Box(
                        Modifier.size(38.dp).clip(CircleShape).background(preset).clickable {
                            val presetHsv = FloatArray(3)
                            android.graphics.Color.colorToHSV(preset.toArgb(), presetHsv)
                            hue = presetHsv[0]
                            onColorChange(preset.toArgb())
                        },
                        contentAlignment = Alignment.Center
                    ) {
                        if (color.toArgb() == preset.toArgb()) {
                            Icon(Icons.Default.Check, contentDescription = null, tint = Color.White, modifier = Modifier.size(20.dp))
                        }
                    }
                }
            }
            HueSlider(hue) {
                hue = it
                onColorChange(android.graphics.Color.HSVToColor(floatArrayOf(it, 0.78f, 0.92f)))
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HueSlider(value: Float, onValueChange: (Float) -> Unit) {
    val hueColors = remember {
        (0..360 step 30).map { Color.hsv(it.toFloat(), 0.78f, 0.92f) }
    }
    Box(
        Modifier.fillMaxWidth().height(44.dp).padding(top = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Box(
            Modifier.fillMaxWidth().height(12.dp).clip(CircleShape)
                .background(Brush.horizontalGradient(hueColors))
        )
        Slider(
            value = value,
            onValueChange = onValueChange,
            valueRange = 0f..360f,
            colors = SliderDefaults.colors(
                thumbColor = Color.White,
                activeTrackColor = Color.Transparent,
                inactiveTrackColor = Color.Transparent,
                activeTickColor = Color.Transparent,
                inactiveTickColor = Color.Transparent
            ),
            thumb = {
                Box(Modifier.width(4.dp).height(34.dp).background(Color.White, CircleShape))
            },
            modifier = Modifier.fillMaxWidth()
        )
    }
}
