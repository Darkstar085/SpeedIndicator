package com.sipun.netspeedindicator.ui.screens.main.home

import android.app.Activity
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
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
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.StrokeJoin
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.tooling.preview.Preview
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
                .padding(bottom = 8.dp),
            verticalArrangement = Arrangement.SpaceBetween
        ) {
            CurrentSpeedCard(uiState.currentSpeed, uiState.peakSpeed, uiState.sessionDurationSeconds)
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                    Text(stringResource(R.string.today_s_usage), fontFamily = OutfitFontFamily, fontWeight = FontWeight.SemiBold, fontSize = 20.sp, color = MaterialTheme.colorScheme.onBackground)
                    Text(stringResource(R.string.reset_12_00_am), fontSize = 12.sp, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
                TotalUsageCard(uiState.todayUsage)
            }
        }
    }
}

@Composable
private fun CurrentSpeedCard(currentSpeed: SpeedInfo, peakSpeed: Long, sessionDurationSeconds: Long) {
    Column(
        modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(22.dp)).background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.075f)).border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.42f), RoundedCornerShape(22.dp)).padding(horizontal = 18.dp, vertical = 11.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Row(Modifier.fillMaxWidth(), Arrangement.SpaceBetween, Alignment.CenterVertically) {
            Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                BlinkingDot()
                Text(stringResource(R.string.live_session), fontSize = 11.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary, letterSpacing = 1.sp)
            }
            SignalBars()
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
    Column(Modifier.fillMaxWidth().padding(top = 16.dp, bottom = 10.dp), horizontalAlignment = Alignment.CenterHorizontally) {
        Text(stringResource(R.string.total_speed), fontSize = 11.sp, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Text(buildAnnotatedString {
            withStyle(SpanStyle(fontSize = 48.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground)) { append(FormatUtils.formatSpeedValue(speed)) }
            append(" ")
            withStyle(SpanStyle(fontSize = 18.sp, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurfaceVariant)) { append(FormatUtils.formatSpeedUnit(speed)) }
        }, fontFamily = OutfitFontFamily, modifier = Modifier.padding(top = 2.dp))
        SpeedWave()
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
        val path = androidx.compose.ui.graphics.Path()
        for (i in 0 until 160) {
            val x = i * step
            val y = centerY - kotlin.math.sin(i / 159f * 2.15f * 2f * kotlin.math.PI.toFloat() + phase) * amplitude
            if (i == 0) path.moveTo(x, y) else path.lineTo(x, y)
        }
        val fill = androidx.compose.ui.graphics.Path().apply { moveTo(0f, size.height); addPath(path); lineTo(size.width, size.height); close() }
        drawPath(fill, waveFillColor)
        drawPath(path, waveColor, style = androidx.compose.ui.graphics.drawscope.Stroke(1.8.dp.toPx(), cap = StrokeCap.Round, join = StrokeJoin.Round))
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
private fun SignalBars() {
    Row(horizontalArrangement = Arrangement.spacedBy(3.dp), verticalAlignment = Alignment.Bottom, modifier = Modifier.height(21.dp).alpha(.55f)) {
        listOf(.35f, .55f, .8f, .62f, .75f).forEach { h -> Box(Modifier.width(4.dp).height(21.dp * h).clip(CircleShape).background(MaterialTheme.colorScheme.primary)) }
    }
}

@Composable
private fun BlinkingDot() {
    val transition = rememberInfiniteTransition(label = "live")
    val alpha by transition.animateFloat(1f, .3f, infiniteRepeatable(tween(1000, easing = FastOutSlowInEasing), RepeatMode.Reverse), label = "liveAlpha")
    Box(Modifier.size(8.dp).clip(CircleShape).background(MaterialTheme.colorScheme.primary).alpha(alpha))
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
    Column(modifier = Modifier.fillMaxWidth().clip(RoundedCornerShape(18.dp)).background(MaterialTheme.colorScheme.primary.copy(alpha = .055f)).border(1.dp, MaterialTheme.colorScheme.primary.copy(alpha = .28f), RoundedCornerShape(18.dp)).padding(horizontal = 16.dp, vertical = 11.dp), verticalArrangement = Arrangement.spacedBy(2.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(9.dp)) {
            Icon(Icons.Default.DataUsage, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(21.dp))
            Text(stringResource(R.string.total_usage), fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.primary)
        }
        Text(buildAnnotatedString {
            withStyle(SpanStyle(fontSize = 34.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onBackground)) { append(FormatUtils.formatBytesValue(totalBytes)) }
            append(" ")
            withStyle(SpanStyle(fontSize = 17.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)) { append(FormatUtils.formatBytesUnit(totalBytes)) }
        }, fontFamily = OutfitFontFamily)
    }
}

@Composable
private fun UsageBreakdownCard(modifier: Modifier, title: String, usage: Long, percentage: Float, icon: ImageVector) {
    Column(modifier = modifier.clip(RoundedCornerShape(15.dp)).background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = .10f)).border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = .24f), RoundedCornerShape(15.dp)).padding(horizontal = 11.dp, vertical = 9.dp), verticalArrangement = Arrangement.spacedBy(6.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(7.dp)) {
            Icon(icon, null, tint = MaterialTheme.colorScheme.primary, modifier = Modifier.size(19.dp))
            Text(title, fontSize = 11.sp, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Bottom, horizontalArrangement = Arrangement.SpaceBetween) {
            Row(verticalAlignment = Alignment.Bottom) {
                Text(FormatUtils.formatBytesValue(usage), fontSize = 17.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
                Spacer(Modifier.width(3.dp))
                Text(FormatUtils.formatBytesUnit(usage), fontSize = 10.sp, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
            Text("${(percentage * 100).formatPercentage()}%", fontSize = 11.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.primary)
        }
    }
}

private fun Float.formatPercentage(): String = String.format(java.util.Locale.US, "%.1f", this)

@Preview
@Composable
fun HomeScreenPreview() { NetSpeedIndicatorTheme { HomeScreenContent(uiState = HomeUiState(), onEvent = {}) } }
