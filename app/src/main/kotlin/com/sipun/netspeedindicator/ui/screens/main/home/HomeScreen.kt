package com.sipun.netspeedindicator.ui.screens.main.home

import android.app.Activity
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
import kotlinx.coroutines.delay
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.sipun.netspeedindicator.R
import com.sipun.netspeedindicator.core.util.FormatUtils
import com.sipun.netspeedindicator.domain.model.SpeedInfo
import com.sipun.netspeedindicator.domain.model.UsageInfo
import com.sipun.netspeedindicator.ui.components.AppTopBar
import com.sipun.netspeedindicator.ui.components.StopMonitoringDialog
import com.sipun.netspeedindicator.ui.theme.NetSpeedIndicatorTheme
import com.sipun.netspeedindicator.ui.theme.OutfitFontFamily
import com.sipun.netspeedindicator.ui.theme.dimens

@Composable
fun HomeScreen(viewModel: HomeViewModel = hiltViewModel()) {
    val uiState by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    LaunchedEffect(uiState.shouldFinishActivity) {
        if (uiState.shouldFinishActivity) {
            (context as? Activity)?.finishAffinity()
            viewModel.onEvent(HomeUiEvent.OnActivityFinished)
        }
    }
    HomeScreenContent(uiState = uiState, onEvent = viewModel::onEvent)
}

@Composable
private fun HomeScreenContent(uiState: HomeUiState, onEvent: (HomeUiEvent) -> Unit = {}) {
    if (uiState.showStopDialog) {
        StopMonitoringDialog(onDismiss = { onEvent(HomeUiEvent.OnDismissDialog) }, onConfirm = { onEvent(HomeUiEvent.OnConfirmStop) })
    }
    Scaffold(
        topBar = { AppTopBar(title = stringResource(R.string.dashboard), subTitle = stringResource(R.string.real_time_monitor), showTrailingIcon = true, trailingIcon = Icons.Default.PowerSettingsNew, onTrailingIconClick = { onEvent(HomeUiEvent.OnStopClick) }) },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = paddingValues.calculateTopPadding())
                .padding(horizontal = dimens.horizontalPadding)
                .padding(bottom = 98.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            CurrentSpeedCard(uiState.currentSpeed, uiState.peakSpeed, uiState.sessionDurationSeconds)
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text(stringResource(R.string.today_s_usage), fontFamily = OutfitFontFamily, fontWeight = FontWeight.SemiBold, fontSize = 20.sp, color = MaterialTheme.colorScheme.onBackground)
                    ResetUsageLabel()
                }
                TotalUsageCard(uiState.todayUsage)
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

    Row(
        modifier = Modifier
            .clip(RoundedCornerShape(18.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.28f))
            .border(
                1.dp,
                MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.22f),
                RoundedCornerShape(18.dp)
            )
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
private fun CurrentSpeedCard(currentSpeed: SpeedInfo, peakSpeed: Long, sessionDurationSeconds: Long) {
    Column(
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(22.dp)).background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.075f)).border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.42f), RoundedCornerShape(22.dp)).padding(horizontal = 18.dp, vertical = 15.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(Modifier.fillMaxWidth(), Arrangement.Start, Alignment.Top) {
            Column(verticalArrangement = Arrangement.spacedBy(0.5.dp)) {
                Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    BlinkingDot()
                    Text(stringResource(R.string.live_session), fontSize = 12.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary, letterSpacing = 1.sp)
                }
                Text(stringResource(R.string.live_session_desc), fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.padding(start = 16.dp).offset(y = (-3).dp))
            }
        }
        SpeedDisplay(currentSpeed.totalBytesPerSecond)
        Row(Modifier.fillMaxWidth(), Arrangement.spacedBy(9.dp)) {
            SpeedStatCard(Modifier.weight(1f), stringResource(R.string.download), FormatUtils.formatSpeed(currentSpeed.downloadBytesPerSecond), Icons.Default.Download)
            SpeedStatCard(Modifier.weight(1f), stringResource(R.string.upload), FormatUtils.formatSpeed(currentSpeed.uploadBytesPerSecond), Icons.Default.Upload)
        }
        Spacer(Modifier.height(8.dp))
        Row(Modifier.fillMaxWidth(), Arrangement.spacedBy(9.dp)) {
            SpeedStatCard(Modifier.weight(1f), stringResource(R.string.peak), FormatUtils.formatSpeed(peakSpeed), Icons.Default.Speed)
            SpeedStatCard(Modifier.weight(1f), stringResource(R.string.time), FormatUtils.formatDuration(sessionDurationSeconds), Icons.Default.Timer)
        }
    }
}

@Composable
private fun SpeedDisplay(speed: Long) {
    Column(Modifier.fillMaxWidth().padding(top = 6.dp, bottom = 4.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Text(stringResource(R.string.total_speed), fontSize = 11.sp, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(buildAnnotatedString {
            withStyle(SpanStyle(fontSize = 48.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground)) { append(FormatUtils.formatSpeedValue(speed)) }
            append(" ")
            withStyle(SpanStyle(fontSize = 18.sp, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurfaceVariant)) { append(FormatUtils.formatSpeedUnit(speed)) }
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
            .height(44.dp)
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
    Row(modifier.clip(RoundedCornerShape(13.dp)).background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = .10f)).border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = .20f), RoundedCornerShape(13.dp)).padding(horizontal = 10.dp, vertical = 8.dp), verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
        Icon(icon, null, tint = MaterialTheme.colorScheme.primary.copy(alpha = .9f), modifier = Modifier.size(18.dp))
        Column {
            Text(label, fontSize = 10.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(value, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurface)
        }
    }
}

@Composable
private fun BlinkingDot() {
    val transition = rememberInfiniteTransition(label = "live")
    val alpha by transition.animateFloat(.45f, 1f, infiniteRepeatable(tween(900, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "liveAlpha")
    val scale by transition.animateFloat(.78f, 1f, infiniteRepeatable(tween(900, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "liveScale")
    Box(Modifier.size(8.dp).scale(scale).clip(CircleShape).background(MaterialTheme.colorScheme.primary).alpha(alpha))
}

@Composable
private fun TotalUsageCard(todayUsage: UsageInfo) {
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
    Column(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(20.dp)).border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = .28f), RoundedCornerShape(20.dp)).padding(8.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
        UsageSummaryCard(totalBytes)
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            UsageBreakdownCard(Modifier.weight(1f), stringResource(R.string.download), downloadBytes, downloadPercentage, Icons.Default.Download)
            UsageBreakdownCard(Modifier.weight(1f), stringResource(R.string.upload), uploadBytes, uploadPercentage, Icons.Default.Upload)
        }
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(8.dp)) {
            UsageBreakdownCard(Modifier.weight(1f), stringResource(R.string.mobile), mobileBytes, mobilePercentage, Icons.Default.SignalCellularAlt)
            UsageBreakdownCard(Modifier.weight(1f), stringResource(R.string.wifi), wifiBytes, wifiPercentage, Icons.Default.Wifi)
        }
    }
}

@Composable
private fun UsageSummaryCard(totalBytes: Long) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = .10f))
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = .24f), RoundedCornerShape(18.dp))
            .padding(horizontal = 16.dp, vertical = 11.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(9.dp)
    ) {
        Icon(Icons.Default.DataUsage, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(21.dp))
        Column(verticalArrangement = Arrangement.spacedBy(2.dp)) {
            Text(stringResource(R.string.total_usage), fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.primary)
            Text(buildAnnotatedString {
                withStyle(SpanStyle(fontSize = 34.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground)) { append(FormatUtils.formatBytesValue(totalBytes)) }
                append(" ")
                withStyle(SpanStyle(fontSize = 17.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)) { append(FormatUtils.formatBytesUnit(totalBytes)) }
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
    val fillColor = MaterialTheme.colorScheme.primary.copy(alpha = 0.28f)
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
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = .10f))
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = .24f), shape)
    ) {
        Canvas(Modifier.matchParentSize()) {
            val fillWidth = size.width * animatedPercentage
            if (fillWidth > 0f) {
                drawRect(color = fillColor, topLeft = androidx.compose.ui.geometry.Offset.Zero, size = androidx.compose.ui.geometry.Size(fillWidth, size.height))
                val drift = kotlin.math.sin(phase) * size.width * 0.18f
                val sheenWidth = (size.width * 0.45f).coerceAtLeast(1f)
                drawRect(
                    brush = Brush.linearGradient(
                        colors = listOf(Color.Transparent, fillMotionColor.copy(alpha = 0.10f), Color.Transparent),
                        start = androidx.compose.ui.geometry.Offset(x = -sheenWidth + drift, y = 0f),
                        end = androidx.compose.ui.geometry.Offset(x = sheenWidth + drift, y = size.height)
                    ),
                    topLeft = androidx.compose.ui.geometry.Offset.Zero,
                    size = androidx.compose.ui.geometry.Size(fillWidth, size.height)
                )
            }
        }

        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 11.dp, vertical = 9.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(19.dp))
            Column(
                modifier = Modifier.padding(start = 7.dp).weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                Text(title, fontSize = 11.sp, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(FormatUtils.formatBytesValue(usage), fontSize = 17.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                    Spacer(Modifier.width(3.dp))
                    Text(FormatUtils.formatBytesUnit(usage), fontSize = 10.sp, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            Text("${(percentage * 100).formatPercentage()}%", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.primary)
        }
    }
}

private fun Float.formatPercentage(): String = String.format(java.util.Locale.US, "%.1f", this)

@Preview
@Composable
fun HomeScreenPreview() { NetSpeedIndicatorTheme { HomeScreenContent(uiState = HomeUiState(), onEvent = {}) } }
