package com.sipun.netspeedindicator.ui.screens.main.home

import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.asPaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccessTime
import androidx.compose.material.icons.filled.DataUsage
import androidx.compose.material.icons.filled.Download
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.SignalCellularAlt
import androidx.compose.material.icons.filled.Speed
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Upload
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.core.graphics.drawable.toBitmap
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.sipun.netspeedindicator.R
import com.sipun.netspeedindicator.core.util.FormatUtils
import com.sipun.netspeedindicator.domain.model.LiveAppRate
import com.sipun.netspeedindicator.domain.model.LiveRatesDiagnostics
import com.sipun.netspeedindicator.domain.model.LiveRatesStrategy
import com.sipun.netspeedindicator.domain.model.SpeedInfo
import com.sipun.netspeedindicator.domain.model.UsageInfo
import com.sipun.netspeedindicator.ui.components.AppTopBar
import com.sipun.netspeedindicator.ui.components.StopMonitoringDialog
import com.sipun.netspeedindicator.ui.navigation.MainRoute
import com.sipun.netspeedindicator.ui.theme.AppDialogTheme
import com.sipun.netspeedindicator.ui.theme.NetSpeedIndicatorTheme
import com.sipun.netspeedindicator.ui.theme.OutfitFontFamily
import com.sipun.netspeedindicator.ui.theme.dimens
import com.sipun.netspeedindicator.ui.util.AutoHideBottomBarEffect
import com.sipun.netspeedindicator.ui.util.LocalNavController
import kotlinx.coroutines.delay

@Composable
fun HomeScreen(viewModel: HomeViewModel = hiltViewModel()) {
    val uiState by viewModel.uiState.collectAsState()
    HomeScreenContent(
        uiState = uiState,
        onEvent = viewModel::onEvent,
        diagnosticsProvider = viewModel::liveRatesDiagnostics
    )
}

@Composable
private fun HomeScreenContent(
    uiState: HomeUiState,
    onEvent: (HomeUiEvent) -> Unit = {},
    diagnosticsProvider: () -> LiveRatesDiagnostics = {
        LiveRatesDiagnostics(LiveRatesStrategy.NETWORK_STATS, true, 0, 0)
    }
) {
    if (uiState.showStopDialog) {
        StopMonitoringDialog(
            onDismiss = { onEvent(HomeUiEvent.OnDismissDialog) },
            onConfirm = { onEvent(HomeUiEvent.OnConfirmStop) })
    }
    val scrollState = rememberScrollState()
    AutoHideBottomBarEffect(scrollState)

    val isDark = MaterialTheme.colorScheme.surface.red < 0.5f
    val ambientGlow = Brush.radialGradient(
        colors = listOf(
            MaterialTheme.colorScheme.primary.copy(alpha = if (isDark) 0.12f else 0.07f),
            Color.Transparent
        ),
        center = androidx.compose.ui.geometry.Offset(800f, 600f),
        radius = 1100f
    )
    Scaffold(
        contentWindowInsets = WindowInsets(0),
        topBar = {
            AppTopBar(
                title = stringResource(R.string.dashboard),
                subTitle = stringResource(R.string.real_time_monitor),
                showTrailingIcon = true,
                trailingIcon = Icons.Default.PowerSettingsNew,
                onTrailingIconClick = { onEvent(HomeUiEvent.OnStopClick) })
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        val navBarBottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding()
        BoxWithConstraints(
            modifier = Modifier
                .fillMaxSize()
                .background(ambientGlow)
                .padding(top = paddingValues.calculateTopPadding(), bottom = 54.dp + navBarBottom)
        ) {
            val density = LocalDensity.current
            val contentScale = (maxHeight / 720.dp).coerceIn(0.84f, 1f)
            CompositionLocalProvider(
                LocalDensity provides Density(
                    density = density.density * contentScale,
                    fontScale = density.fontScale
                )
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .verticalScroll(scrollState)
                        .padding(horizontal = dimens.horizontalPadding, vertical = 12.dp),
                    verticalArrangement = Arrangement.spacedBy(22.dp)
                ) {
                    CurrentSpeedCard(
                        uiState.currentSpeed,
                        uiState.peakSpeed,
                        uiState.sessionDurationSeconds
                    )
                    ActiveAppsCard(
                        isServiceRunning = uiState.isServiceRunning,
                        apps = uiState.activeApps,
                        onStartMonitoring = { onEvent(HomeUiEvent.OnStopClick) },
                        diagnosticsProvider = diagnosticsProvider
                    )
                    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                        Row(
                            Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                stringResource(R.string.today_s_usage),
                                fontFamily = OutfitFontFamily,
                                fontWeight = FontWeight.SemiBold,
                                fontSize = 20.sp,
                                color = MaterialTheme.colorScheme.onBackground
                            )
                            ResetUsageLabel()
                        }
                        TotalUsageCard(uiState.todayUsage)
                    }
                }
            }
        }
    }
}

@Composable
private fun ResetUsageLabel() {
    val remainingMillis = remember { mutableLongStateOf(0L) }

    LaunchedEffect(Unit) {
        while (true) {
            val now = java.time.ZonedDateTime.now()
            val nextReset = now.toLocalDate()
                .plusDays(1)
                .atStartOfDay(now.zone)
            remainingMillis.longValue = java.time.Duration.between(now, nextReset)
                .toMillis()
                .coerceAtLeast(0L)
            delay(1000L)
        }
    }

    val totalSeconds = (remainingMillis.longValue / 1000L).toInt()
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    val seconds = totalSeconds % 60
    val countdown = String.format(java.util.Locale.US, "%02d:%02d:%02d", hours, minutes, seconds)

    val isDark = MaterialTheme.colorScheme.surface.red < 0.5f
    val resetFill = if (isDark) {
        Brush.verticalGradient(
            listOf(
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.28f),
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.12f)
            )
        )
    } else {
        Brush.verticalGradient(
            listOf(
                MaterialTheme.colorScheme.surface,
                MaterialTheme.colorScheme.surface.copy(alpha = 0.85f)
            )
        )
    }
    val resetBorder = if (isDark) {
        Brush.verticalGradient(
            listOf(
                MaterialTheme.colorScheme.onSurface.copy(alpha = 0.25f),
                MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.08f)
            )
        )
    } else {
        Brush.verticalGradient(
            listOf(
                Color.White.copy(alpha = 0.90f),
                MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)
            )
        )
    }

    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(18.dp))
            .background(resetFill)
            .border(1.dp, resetBorder, RoundedCornerShape(18.dp))
            .padding(horizontal = 10.dp, vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(5.dp)
    ) {
        Icon(
            Icons.Default.AccessTime,
            null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(15.dp)
        )
        Text(
            stringResource(R.string.reset_countdown, countdown),
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun CurrentSpeedCard(
    currentSpeed: SpeedInfo,
    peakSpeed: Long,
    sessionDurationSeconds: Long
) {
    val isDark = MaterialTheme.colorScheme.surface.red < 0.5f
    val cardFill = if (isDark) {
        Brush.verticalGradient(
            listOf(
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.18f),
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.08f)
            )
        )
    } else {
        Brush.verticalGradient(
            listOf(
                MaterialTheme.colorScheme.surface,
                MaterialTheme.colorScheme.surface.copy(alpha = 0.85f)
            )
        )
    }
    val glassBorder = if (isDark) {
        Brush.verticalGradient(
            listOf(
                MaterialTheme.colorScheme.onSurface.copy(alpha = 0.28f),
                MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.08f)
            )
        )
    } else {
        Brush.verticalGradient(
            listOf(
                Color.White.copy(alpha = 0.90f),
                MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)
            )
        )
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .background(cardFill)
            .border(1.dp, glassBorder, RoundedCornerShape(22.dp))
            .padding(horizontal = 18.dp, vertical = 18.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(Modifier.fillMaxWidth(), Arrangement.Start, Alignment.Top) {
            Column(verticalArrangement = Arrangement.spacedBy(0.5.dp)) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    BlinkingDot()
                    Text(
                        stringResource(R.string.live_session),
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary,
                        letterSpacing = 1.sp
                    )
                }
                Text(
                    stringResource(R.string.live_session_desc),
                    fontSize = 10.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier
                        .padding(start = 16.dp)
                        .offset(y = (-3).dp)
                )
            }
        }
        SpeedDisplay(currentSpeed.totalBytesPerSecond)
        Row(Modifier.fillMaxWidth(), Arrangement.spacedBy(9.dp)) {
            SpeedStatCard(
                Modifier.weight(1f),
                stringResource(R.string.download),
                FormatUtils.formatSpeed(currentSpeed.downloadBytesPerSecond),
                Icons.Default.Download
            )
            SpeedStatCard(
                Modifier.weight(1f),
                stringResource(R.string.upload),
                FormatUtils.formatSpeed(currentSpeed.uploadBytesPerSecond),
                Icons.Default.Upload
            )
        }
        Spacer(Modifier.height(8.dp))
        Row(Modifier.fillMaxWidth(), Arrangement.spacedBy(9.dp)) {
            SpeedStatCard(
                Modifier.weight(1f),
                stringResource(R.string.peak),
                FormatUtils.formatSpeed(peakSpeed),
                Icons.Default.Speed
            )
            SpeedStatCard(
                Modifier.weight(1f),
                stringResource(R.string.time),
                FormatUtils.formatDuration(sessionDurationSeconds),
                Icons.Default.Timer
            )
        }
    }
}

@Composable
private fun SpeedDisplay(speed: Long) {
    Column(
        Modifier
            .fillMaxWidth()
            .padding(top = 10.dp, bottom = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(
            stringResource(R.string.total_speed),
            fontSize = 11.sp,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(buildAnnotatedString {
            withStyle(
                SpanStyle(
                    fontSize = 48.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onBackground
                )
            ) { append(FormatUtils.formatSpeedValue(speed)) }
            append(" ")
            withStyle(
                SpanStyle(
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            ) { append(FormatUtils.formatSpeedUnit(speed)) }
        }, fontFamily = OutfitFontFamily, modifier = Modifier.padding(top = 2.dp))
        SpeedWave(speed)
    }
}

@Composable
private fun SpeedWave(speed: Long) {
    val maxSpeed = 100L * 1024L * 1024L
    val normalizedSpeed = if (speed <= 0L) {
        0f
    } else {
        (
                kotlin.math.log10(speed.toDouble() + 1.0) /
                        kotlin.math.log10(maxSpeed.toDouble() + 1.0)
                ).toFloat().coerceIn(0f, 1f)
    }

    val waveProgress = kotlin.math.sqrt(normalizedSpeed)
    val waveColor = MaterialTheme.colorScheme.primary
    val retainedWaveProgress = remember { mutableFloatStateOf(0f) }

    LaunchedEffect(speed) {
        if (speed > 0L) {
            retainedWaveProgress.floatValue = waveProgress
        }
    }

    val waveVisibility by animateFloatAsState(
        targetValue = if (speed > 0L) 1f else 0f,
        animationSpec = tween(
            durationMillis = 900,
            easing = FastOutSlowInEasing
        ),
        label = "speedWaveVisibility"
    )

    val transition = rememberInfiniteTransition(label = "speedWave")
    val phase by transition.animateFloat(
        0f,
        (2f * kotlin.math.PI).toFloat(),
        infiniteRepeatable(
            tween(
                durationMillis = (5600f - normalizedSpeed * 3800f)
                    .toInt()
                    .coerceAtLeast(1800),
                easing = LinearEasing
            ),
            RepeatMode.Restart
        ),
        label = "speedWavePhase"
    )

    Canvas(
        Modifier
            .fillMaxWidth()
            .height(52.dp)
            .padding(horizontal = 4.dp)
    ) {
        val baselineY = size.height * 0.72f
        val amplitude = 22.dp.toPx() * retainedWaveProgress.floatValue
        val points = 120
        val step = size.width / points

        // The wave grows from the left when speed starts and collapses
        // from the right when speed stops. The remaining portion stays flat.
        val waveEnd = waveVisibility.coerceIn(0f, 1f)
        val transitionWidth = 0.18f

        fun waveEnvelope(progress: Float): Float {
            if (waveVisibility >= 1f) return 1f

            val distanceFromFront = (waveEnd - progress) / transitionWidth
            val t = distanceFromFront.coerceIn(0f, 1f)
            return t * t * (3f - 2f * t)
        }

        val flatStart = waveEnd * size.width
        drawLine(
            color = waveColor.copy(alpha = 0.30f),
            start = androidx.compose.ui.geometry.Offset(flatStart, baselineY),
            end = androidx.compose.ui.geometry.Offset(size.width, baselineY),
            strokeWidth = 1.5.dp.toPx()
        )

        val path = androidx.compose.ui.graphics.Path()
        val edge = androidx.compose.ui.graphics.Path()
        var lastX = 0f
        var lastY = baselineY

        for (i in 0..points) {
            val progress = i / points.toFloat()

            if (progress > waveEnd) {
                break
            }

            val x = i * step
            val primary = kotlin.math.sin(
                progress * 3.2f * kotlin.math.PI.toFloat() + phase
            )
            val secondary = kotlin.math.sin(
                progress * 6.4f * kotlin.math.PI.toFloat() - phase * 0.55f
            ) * 0.18f
            val envelope = waveEnvelope(progress)
            val y = baselineY - (primary + secondary) * amplitude * envelope

            if (i == 0) {
                path.moveTo(x, y)
                edge.moveTo(x, y)
            } else {
                path.lineTo(x, y)
                edge.lineTo(x, y)
            }

            lastX = x
            lastY = y
        }

        path.lineTo(lastX, size.height)
        path.lineTo(0f, size.height)
        path.close()

        val fillAlpha = 0.30f

        if (waveEnd > 0f) {
            drawPath(
                path = path,
                brush = Brush.verticalGradient(
                    colors = listOf(
                        waveColor.copy(alpha = fillAlpha),
                        waveColor.copy(alpha = fillAlpha * 0.28f)
                    ),
                    startY = 0f,
                    endY = size.height
                )
            )
        }

        if (waveEnd > 0f) {
            drawPath(
                path = edge,
                color = waveColor.copy(alpha = 0.62f),
                style = androidx.compose.ui.graphics.drawscope.Stroke(1.5.dp.toPx())
            )
        }
    }
}

@Composable
private fun SpeedStatCard(modifier: Modifier, label: String, value: String, icon: ImageVector) {
    val isDark = MaterialTheme.colorScheme.surface.red < 0.5f
    val fill = if (isDark) {
        Brush.verticalGradient(
            listOf(
                MaterialTheme.colorScheme.surface.copy(alpha = 0.22f),
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.10f)
            )
        )
    } else {
        Brush.verticalGradient(
            listOf(
                MaterialTheme.colorScheme.surface,
                MaterialTheme.colorScheme.surface.copy(alpha = 0.85f)
            )
        )
    }
    val border = if (isDark) {
        Brush.verticalGradient(
            listOf(
                MaterialTheme.colorScheme.onSurface.copy(alpha = 0.22f),
                MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.08f)
            )
        )
    } else {
        Brush.verticalGradient(
            listOf(
                Color.White.copy(alpha = 0.90f),
                MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)
            )
        )
    }
    Row(
        modifier
            .clip(RoundedCornerShape(13.dp))
            .background(fill)
            .border(1.dp, border, RoundedCornerShape(13.dp))
            .padding(horizontal = 10.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Icon(
            icon,
            null,
            tint = MaterialTheme.colorScheme.primary.copy(alpha = .9f),
            modifier = Modifier.size(18.dp)
        )
        Column {
            Text(label, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(
                value,
                fontSize = 13.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}

@Composable
private fun BlinkingDot() {
    val transition = rememberInfiniteTransition(label = "live")
    val alpha by transition.animateFloat(
        .45f,
        1f,
        infiniteRepeatable(tween(900, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "liveAlpha"
    )
    val scale by transition.animateFloat(
        .78f,
        1f,
        infiniteRepeatable(tween(900, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "liveScale"
    )
    Box(
        Modifier
            .size(8.dp)
            .scale(scale)
            .clip(CircleShape)
            .background(MaterialTheme.colorScheme.primary)
            .alpha(alpha)
    )
}

@Composable
private fun TotalUsageCard(todayUsage: UsageInfo) {
    val navController = LocalNavController.current
    val totalBytes = todayUsage.totalBytes
    val downloadBytes = todayUsage.totalDownloadBytes
    val uploadBytes = todayUsage.totalUploadBytes
    val mobileBytes = todayUsage.mobileTotalBytes
    val wifiBytes = todayUsage.wifiTotalBytes
    val downloadUploadTotal = (downloadBytes + uploadBytes).coerceAtLeast(1L)
    val mobileWifiTotal = (mobileBytes + wifiBytes).coerceAtLeast(1L)
    val downloadPercentage = downloadBytes.toFloat() / downloadUploadTotal
    val uploadPercentage = uploadBytes.toFloat() / downloadUploadTotal
    val mobilePercentage = mobileBytes.toFloat() / mobileWifiTotal
    val wifiPercentage = wifiBytes.toFloat() / mobileWifiTotal

    val isDark = MaterialTheme.colorScheme.surface.red < 0.5f
    val cardFill = if (isDark) {
        Brush.verticalGradient(
            listOf(
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.16f),
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.06f)
            )
        )
    } else {
        Brush.verticalGradient(
            listOf(
                MaterialTheme.colorScheme.surface,
                MaterialTheme.colorScheme.surface.copy(alpha = 0.85f)
            )
        )
    }
    val glassBorder = if (isDark) {
        Brush.verticalGradient(
            listOf(
                MaterialTheme.colorScheme.onSurface.copy(alpha = 0.28f),
                MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.08f)
            )
        )
    } else {
        Brush.verticalGradient(
            listOf(
                Color.White.copy(alpha = 0.90f),
                MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)
            )
        )
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(20.dp))
            .background(cardFill)
            .border(1.dp, glassBorder, RoundedCornerShape(20.dp))
            .clickable {
                navController.navigate(MainRoute.AppDataUsage) {
                    popUpTo(navController.graph.startDestinationId) { saveState = true }
                    launchSingleTop = true
                    restoreState = true
                }
            }
            .padding(6.dp),
        verticalArrangement = Arrangement.spacedBy(6.dp)
    ) {
        UsageSummaryCard(totalBytes)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            UsageBreakdownCard(
                Modifier.weight(1f),
                stringResource(R.string.download),
                downloadBytes,
                downloadPercentage,
                Icons.Default.Download
            )
            UsageBreakdownCard(
                Modifier.weight(1f),
                stringResource(R.string.upload),
                uploadBytes,
                uploadPercentage,
                Icons.Default.Upload
            )
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            UsageBreakdownCard(
                Modifier.weight(1f),
                stringResource(R.string.mobile),
                mobileBytes,
                mobilePercentage,
                Icons.Default.SignalCellularAlt
            )
            UsageBreakdownCard(
                Modifier.weight(1f),
                stringResource(R.string.wifi),
                wifiBytes,
                wifiPercentage,
                Icons.Default.Wifi
            )
        }
    }
}

@Composable
private fun UsageSummaryCard(totalBytes: Long) {
    val isDark = MaterialTheme.colorScheme.surface.red < 0.5f
    val fill = if (isDark) {
        Brush.verticalGradient(
            listOf(
                MaterialTheme.colorScheme.surface.copy(alpha = 0.20f),
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.10f)
            )
        )
    } else {
        Brush.verticalGradient(
            listOf(
                MaterialTheme.colorScheme.surface,
                MaterialTheme.colorScheme.surface.copy(alpha = 0.85f)
            )
        )
    }
    val border = if (isDark) {
        Brush.verticalGradient(
            listOf(
                MaterialTheme.colorScheme.onSurface.copy(alpha = 0.22f),
                MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.08f)
            )
        )
    } else {
        Brush.verticalGradient(
            listOf(
                Color.White.copy(alpha = 0.90f),
                MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)
            )
        )
    }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(fill)
            .border(1.dp, border, RoundedCornerShape(18.dp))
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(9.dp)
    ) {
        Icon(
            Icons.Default.DataUsage,
            null,
            tint = MaterialTheme.colorScheme.primary,
            modifier = Modifier.size(22.dp)
        )
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(
                stringResource(R.string.total_usage),
                fontSize = 12.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.primary
            )
            Text(buildAnnotatedString {
                withStyle(
                    SpanStyle(
                        fontSize = 32.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                ) { append(FormatUtils.formatBytesValue(totalBytes)) }
                append(" ")
                withStyle(
                    SpanStyle(
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.primary
                    )
                ) { append(FormatUtils.formatBytesUnit(totalBytes)) }
            }, fontFamily = OutfitFontFamily)
        }
    }
}

@Composable
private fun UsageBreakdownCard(
    modifier: Modifier,
    title: String,
    usage: Long,
    percentage: Float,
    icon: ImageVector
) {
    val shape = RoundedCornerShape(15.dp)
    val targetPercentage = percentage.coerceIn(0f, 1f)
    val animatedPercentage by animateFloatAsState(
        targetValue = targetPercentage,
        animationSpec = tween(700, easing = FastOutSlowInEasing),
        label = "usageFill"
    )
    val fillMotionColor = MaterialTheme.colorScheme.primary
    val transition = rememberInfiniteTransition(label = "usageFillMotion")
    val phase by transition.animateFloat(
        0f,
        (2f * kotlin.math.PI).toFloat(),
        infiniteRepeatable(tween(2200, easing = LinearEasing), RepeatMode.Restart),
        label = "usageFillPhase"
    )

    Box(
        modifier = modifier
            .clip(shape)
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = .18f))
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = .24f), shape)
    ) {
        Canvas(Modifier.matchParentSize()) {
            val fillWidth = size.width * animatedPercentage
            if (fillWidth > 0f) {
                val fillBrush = Brush.horizontalGradient(
                    colors = listOf(
                        fillMotionColor.copy(alpha = 0.22f),
                        fillMotionColor.copy(alpha = 0.14f)
                    ),
                    startX = 0f,
                    endX = fillWidth
                )
                drawRect(
                    brush = fillBrush,
                    topLeft = androidx.compose.ui.geometry.Offset.Zero,
                    size = androidx.compose.ui.geometry.Size(fillWidth, size.height)
                )
                val drift = kotlin.math.sin(phase) * size.width * 0.18f
                val sheenWidth = (size.width * 0.45f).coerceAtLeast(1f)
                drawRect(
                    brush = Brush.linearGradient(
                        colors = listOf(
                            Color.Transparent,
                            fillMotionColor.copy(alpha = 0.12f),
                            Color.Transparent
                        ),
                        start = androidx.compose.ui.geometry.Offset(
                            x = -sheenWidth + drift,
                            y = 0f
                        ),
                        end = androidx.compose.ui.geometry.Offset(
                            x = sheenWidth + drift,
                            y = size.height
                        )
                    ),
                    topLeft = androidx.compose.ui.geometry.Offset.Zero,
                    size = androidx.compose.ui.geometry.Size(fillWidth, size.height)
                )
            }
        }

        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 11.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                icon,
                null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(19.dp)
            )
            Column(
                modifier = Modifier
                    .padding(start = 7.dp)
                    .weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(
                    title,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(
                        FormatUtils.formatBytesValue(usage),
                        fontSize = 17.sp,
                        fontWeight = FontWeight.Bold,
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Spacer(Modifier.width(3.dp))
                    Text(
                        FormatUtils.formatBytesUnit(usage),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Medium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            }
            Text(
                "${(percentage * 100).formatPercentage()}%",
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}

private const val MAX_VISIBLE_ACTIVE_APPS = 4

@Composable
private fun ActiveAppsCard(
    isServiceRunning: Boolean,
    apps: List<LiveAppRate>,
    onStartMonitoring: () -> Unit,
    diagnosticsProvider: () -> LiveRatesDiagnostics
) {
    val navController = LocalNavController.current
    val showAllDialog = remember { mutableStateOf(false) }
    val showDiagnostics = remember { mutableStateOf(false) }
    val diagnostics = remember { mutableStateOf<LiveRatesDiagnostics?>(null) }
    val isDark = MaterialTheme.colorScheme.surface.red < 0.5f
    val cardFill = if (isDark) {
        Brush.verticalGradient(
            listOf(
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.18f),
                MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.08f)
            )
        )
    } else {
        Brush.verticalGradient(
            listOf(
                MaterialTheme.colorScheme.surface,
                MaterialTheme.colorScheme.surface.copy(alpha = 0.85f)
            )
        )
    }
    val glassBorder = if (isDark) {
        Brush.verticalGradient(
            listOf(
                MaterialTheme.colorScheme.onSurface.copy(alpha = 0.28f),
                MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.08f)
            )
        )
    } else {
        Brush.verticalGradient(
            listOf(
                Color.White.copy(alpha = 0.90f),
                MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.35f)
            )
        )
    }

    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(22.dp))
            .background(cardFill)
            .border(1.dp, glassBorder, RoundedCornerShape(22.dp))
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(10.dp))
                .combinedClickable(
                    interactionSource = remember {
                        androidx.compose.foundation.interaction.MutableInteractionSource()
                    },
                    indication = null,
                    onClick = {},
                    onLongClick = {
                        diagnostics.value = diagnosticsProvider()
                        showDiagnostics.value = true
                    }
                ),
            Arrangement.SpaceBetween,
            Alignment.CenterVertically
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(7.dp)
            ) {
                BlinkingDot()
                Text(
                    stringResource(R.string.live_now),
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            if (isServiceRunning && apps.isNotEmpty()) {
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(50))
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.16f))
                        .padding(horizontal = 8.dp, vertical = 3.dp)
                ) {
                    Text(
                        stringResource(R.string.active_apps_pill, apps.size),
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.8.sp,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }

        when {
            !isServiceRunning -> MonitoringOffRow(onStartMonitoring)
            apps.isEmpty() -> NoActiveAppsRow()
            else -> {
                val shown = apps.take(MAX_VISIBLE_ACTIVE_APPS)
                val maxRate = shown.maxOf { it.bytesPerSecond }.coerceAtLeast(1L)
                shown.forEach { app ->
                    ActiveAppRow(app = app, maxRate = maxRate)
                }
                if (apps.size > shown.size) {
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(10.dp))
                            .clickable { showAllDialog.value = true }
                            .padding(horizontal = 4.dp, vertical = 4.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(
                            stringResource(R.string.plus_more_apps, apps.size - shown.size),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Medium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Text(
                            stringResource(R.string.show_all),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 0.4.sp,
                            color = MaterialTheme.colorScheme.primary
                        )
                    }
                }
                if (showAllDialog.value) {
                    ActiveAppsDialog(apps = apps, onDismiss = { showAllDialog.value = false })
                }
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(10.dp))
                        .clickable {
                            navController.navigate(MainRoute.AppDataUsage) {
                                popUpTo(navController.graph.startDestinationId) {
                                    saveState = true
                                }
                                launchSingleTop = true
                                restoreState = true
                            }
                        }
                        .padding(vertical = 2.dp),
                    horizontalArrangement = Arrangement.End
                ) {
                    Text(
                        stringResource(R.string.view_all_app_usage),
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        letterSpacing = 0.4.sp,
                        color = MaterialTheme.colorScheme.primary
                    )
                }
            }
        }
        if (showDiagnostics.value && diagnostics.value != null) {
            LiveRatesDiagnosticsDialog(
                diagnostics = diagnostics.value!!,
                onDismiss = { showDiagnostics.value = false }
            )
        }
    }
}

@Composable
private fun ActiveAppsDialog(apps: List<LiveAppRate>, onDismiss: () -> Unit) {
    val maxRate = apps.maxOf { it.bytesPerSecond }.coerceAtLeast(1L)
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        AppDialogTheme {
            Surface(
                modifier = Modifier
                    .fillMaxWidth(0.92f)
                    .heightIn(max = 560.dp)
                    .wrapContentHeight(),
                shape = RoundedCornerShape(28.dp),
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 0.dp
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 18.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            stringResource(R.string.active_apps_dialog_title),
                            fontFamily = OutfitFontFamily,
                            fontWeight = FontWeight.Bold,
                            fontSize = 17.sp,
                            color = MaterialTheme.colorScheme.onBackground,
                            modifier = Modifier.weight(1f)
                        )
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(50))
                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.16f))
                                .padding(horizontal = 8.dp, vertical = 3.dp)
                        ) {
                            Text(
                                stringResource(R.string.active_apps_pill, apps.size),
                                fontSize = 9.sp,
                                fontWeight = FontWeight.Bold,
                                letterSpacing = 0.8.sp,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                    LazyColumn(
                        modifier = Modifier.weight(1f, fill = false),
                        verticalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        items(
                            items = apps,
                            key = { app -> app.uid }
                        ) { app ->
                            ActiveAppRow(app = app, maxRate = maxRate)
                        }
                    }
                    Spacer(Modifier.height(8.dp))
                    Row(Modifier.fillMaxWidth(), Arrangement.End) {
                        TextButton(onClick = onDismiss) {
                            Text(
                                stringResource(R.string.close),
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun LiveRatesDiagnosticsDialog(
    diagnostics: LiveRatesDiagnostics,
    onDismiss: () -> Unit
) {
    val strategy = when (diagnostics.strategy) {
        LiveRatesStrategy.NETWORK_STATS ->
            stringResource(R.string.live_strategy_network_stats)

        LiveRatesStrategy.UID_COUNTERS ->
            stringResource(R.string.live_strategy_uid_counters)

        LiveRatesStrategy.LAUNCHER_ONLY ->
            stringResource(R.string.live_strategy_launcher)
    }
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        AppDialogTheme {
            Surface(
                modifier = Modifier
                    .fillMaxWidth(0.88f)
                    .wrapContentHeight(),
                shape = RoundedCornerShape(28.dp),
                color = MaterialTheme.colorScheme.surface,
                tonalElevation = 0.dp
            ) {
                Column(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 20.dp, vertical = 18.dp)
                ) {
                    Text(
                        stringResource(R.string.live_diagnostics_title),
                        fontFamily = OutfitFontFamily,
                        fontWeight = FontWeight.Bold,
                        fontSize = 17.sp,
                        color = MaterialTheme.colorScheme.onBackground
                    )
                    Spacer(Modifier.height(14.dp))
                    DiagnosticsRow(
                        stringResource(R.string.live_diagnostics_strategy),
                        strategy
                    )
                    DiagnosticsRow(
                        stringResource(R.string.live_diagnostics_usage_access),
                        stringResource(
                            if (diagnostics.usageAccess) {
                                R.string.live_diagnostics_granted
                            } else {
                                R.string.live_diagnostics_not_granted
                            }
                        )
                    )
                    DiagnosticsRow(
                        stringResource(R.string.live_diagnostics_uid_counters),
                        stringResource(
                            R.string.live_diagnostics_counters_value,
                            diagnostics.uidCountersReadable,
                            diagnostics.uidCountersUnsupported
                        )
                    )
                    Spacer(Modifier.height(8.dp))
                    Row(Modifier.fillMaxWidth(), Arrangement.End) {
                        TextButton(onClick = onDismiss) {
                            Text(
                                stringResource(R.string.close),
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun DiagnosticsRow(label: String, value: String) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 6.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Text(
            label,
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(end = 12.dp)
        )
        Text(
            value,
            fontSize = 12.sp,
            fontWeight = FontWeight.SemiBold,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis,
            color = MaterialTheme.colorScheme.onSurface
        )
    }
}

@Composable
private fun MonitoringOffRow(onStart: () -> Unit) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(16.dp))
            .clickable(onClick = onStart)
            .padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Box(
            modifier = Modifier
                .size(36.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.14f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                Icons.Default.PowerSettingsNew,
                null,
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(18.dp)
            )
        }
        Text(
            stringResource(R.string.monitoring_off_live_apps),
            modifier = Modifier.weight(1f),
            fontSize = 11.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Text(
            stringResource(R.string.start_monitoring),
            fontSize = 11.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 0.8.sp,
            color = MaterialTheme.colorScheme.primary
        )
    }
}

@Composable
private fun NoActiveAppsRow() {
    Row(
        modifier = Modifier.padding(vertical = 4.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Icon(
            Icons.Default.Speed,
            null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f),
            modifier = Modifier.size(18.dp)
        )
        Text(
            stringResource(R.string.no_apps_active),
            fontSize = 11.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

@Composable
private fun ActiveAppRow(app: LiveAppRate, maxRate: Long) {
    val context = LocalContext.current
    val density = LocalDensity.current
    val iconSizePx = with(density) { 38.dp.roundToPx() }
    val icon = remember(app.packageName, iconSizePx) {
        runCatching {
            context.packageManager.getApplicationIcon(app.packageName)
                .toBitmap(width = iconSizePx, height = iconSizePx)
                .asImageBitmap()
        }.getOrNull()
    }
    val fraction = (app.bytesPerSecond.toFloat() / maxRate.toFloat()).coerceIn(0.04f, 1f)
    val animatedFraction by animateFloatAsState(
        targetValue = fraction,
        animationSpec = tween(650, easing = FastOutSlowInEasing),
        label = "activeAppRate"
    )

    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        if (icon != null) {
            Image(
                icon,
                app.appName,
                Modifier
                    .size(38.dp)
                    .clip(RoundedCornerShape(9.dp))
            )
        } else {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .clip(RoundedCornerShape(9.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    app.appName.firstOrNull()?.uppercaseChar()?.toString().orEmpty(),
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        Column(
            Modifier.weight(1f),
            verticalArrangement = Arrangement.spacedBy(3.dp)
        ) {
            Row(
                Modifier.fillMaxWidth(),
                Arrangement.SpaceBetween,
                Alignment.CenterVertically
            ) {
                Text(
                    app.appName,
                    modifier = Modifier
                        .weight(1f)
                        .padding(end = 8.dp),
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Text(
                    FormatUtils.formatSpeed(app.bytesPerSecond),
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.primary
                )
            }
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(3.dp)
                    .clip(CircleShape)
                    .background(MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.18f))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth(animatedFraction)
                        .height(3.dp)
                        .clip(CircleShape)
                        .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.85f))
                )
            }
        }
    }
}

private fun Float.formatPercentage(): String = String.format(java.util.Locale.US, "%.1f", this)

@Preview
@Composable
fun HomeScreenPreview() {
    NetSpeedIndicatorTheme {
        HomeScreenContent(
            uiState = HomeUiState(
                isServiceRunning = true,
                activeApps = listOf(
                    LiveAppRate(1001, "com.android.chrome", "Chrome", 2_400_000L),
                    LiveAppRate(1002, "com.example.video", "Streamly", 950_000L),
                    LiveAppRate(1003, "com.example.chat", "Messages", 12_000L),
                    LiveAppRate(1004, "com.example.music", "Music", 6_000L),
                    LiveAppRate(1005, "com.example.mail", "Mail", 3_000L),
                    LiveAppRate(1006, "com.example.news", "News", 1_500L)
                )
            ),
            onEvent = {}
        )
    }
}
