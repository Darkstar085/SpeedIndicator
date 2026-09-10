package com.sipun.netspeedindicator.presentation.history

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.SignalCellularAlt
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material.icons.outlined.DonutLarge
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.sipun.netspeedindicator.R
import com.sipun.netspeedindicator.core.util.FormatUtils
import com.sipun.netspeedindicator.domain.model.UsageInfo
import com.sipun.netspeedindicator.presentation.components.AppTopBar
import com.sipun.netspeedindicator.presentation.theme.dimens
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HistoryScreen(viewModel: HistoryViewModel = hiltViewModel()) {
    val uiState by viewModel.uiState.collectAsState()
    HistoryScreenContent(uiState = uiState, onEvent = viewModel::onEvent)
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun HistoryScreenContent(
    uiState: HistoryUiState,
    onEvent: (HistoryUiEvent) -> Unit = {}
) {
    val dailyUsage = uiState.dailyUsage
    val selectedMonthIndex = uiState.selectedMonthIndex

    val targetMonthPrefix = remember(selectedMonthIndex) {
        YearMonth.now().minusMonths(selectedMonthIndex.toLong())
            .format(DateTimeFormatter.ofPattern("yyyy-MM"))
    }

    val (monthUsage, dateRange) = remember(dailyUsage, targetMonthPrefix, selectedMonthIndex) {
        val monthUsages = dailyUsage.filter { it.date.startsWith(targetMonthPrefix) }
        val total = UsageInfo(
            date = "Total",
            wifiRxBytes = monthUsages.sumOf { it.wifiRxBytes },
            wifiTxBytes = monthUsages.sumOf { it.wifiTxBytes },
            mobileRxBytes = monthUsages.sumOf { it.mobileRxBytes },
            mobileTxBytes = monthUsages.sumOf { it.mobileTxBytes }
        )
        val range = try {
            val yearMonth = YearMonth.parse(targetMonthPrefix)
            val start = yearMonth.atDay(1)
            val end = if (selectedMonthIndex == 0) LocalDate.now() else yearMonth.atEndOfMonth()
            val formatter = DateTimeFormatter.ofPattern("MMM d", Locale.US)
            "${start.format(formatter)} - ${end.format(formatter)}"
        } catch (_: Exception) {
            ""
        }
        total to range
    }

    Scaffold(
        topBar = {
            AppTopBar(
                title = stringResource(R.string.history),
                subTitle = stringResource(R.string.data_usage_logs),
                showTrailingIcon = false
            )
        },
        containerColor = MaterialTheme.colorScheme.background
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = paddingValues.calculateTopPadding())
                .padding(horizontal = dimens.horizontalPadding),
            verticalArrangement = Arrangement.spacedBy(13.dp)
        ) {
            Spacer(modifier = Modifier.height(2.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.09f))
                    .padding(3.dp),
                horizontalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                SegmentedButton(stringResource(R.string.this_month), selectedMonthIndex == 0, Modifier.weight(1f)) { onEvent(HistoryUiEvent.OnSelectMonth(0)) }
                SegmentedButton(stringResource(R.string.last_month), selectedMonthIndex == 1, Modifier.weight(1f)) { onEvent(HistoryUiEvent.OnSelectMonth(1)) }
                SegmentedButton(stringResource(R.string.last_3_months), selectedMonthIndex == 3, Modifier.weight(1f)) { onEvent(HistoryUiEvent.OnSelectMonth(3)) }
            }

            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    .clip(RoundedCornerShape(20.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.045f))
                    .border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.28f), RoundedCornerShape(20.dp))
            ) {
                Column(modifier = Modifier.fillMaxSize()) {
                    TableHeader()
                    if (uiState.isLoading) {
                        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                            CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                        }
                    } else {
                        LazyColumn(
                            modifier = Modifier.fillMaxSize(),
                            contentPadding = androidx.compose.foundation.layout.PaddingValues(bottom = 5.dp)
                        ) {
                            items(dailyUsage) { usage -> UsageRow(usage) }
                        }
                    }
                }
            }

            MonthSummaryCard(usage = monthUsage, dateRange = dateRange)
            Spacer(modifier = Modifier.height(12.dp))
        }
    }
}

@Composable
private fun MonthSummaryCard(usage: UsageInfo, dateRange: String) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(18.dp))
            .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.055f))
            .border(1.dp, MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.28f), RoundedCornerShape(18.dp))
            .padding(horizontal = 18.dp, vertical = 14.dp),
        verticalArrangement = Arrangement.spacedBy(9.dp)
    ) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(stringResource(R.string.month_summary), fontSize = 12.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurfaceVariant, letterSpacing = 0.7.sp)
            Text(dateRange, color = MaterialTheme.colorScheme.primary, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
        }

        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.Bottom,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            val (totalValue, totalUnit) = formatDataParts(usage.totalBytes)
            Column {
                Row(verticalAlignment = Alignment.Bottom) {
                    Text(totalValue, fontSize = 30.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface, lineHeight = 32.sp)
                    Spacer(modifier = Modifier.width(4.dp))
                    Text(totalUnit, fontSize = 13.sp, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.primary, modifier = Modifier.padding(bottom = 2.dp))
                }
                Text(stringResource(R.string.total_usage), fontSize = 10.sp, fontWeight = FontWeight.Medium, color = MaterialTheme.colorScheme.onSurfaceVariant, letterSpacing = 0.6.sp)
            }

            Row(horizontalArrangement = Arrangement.spacedBy(16.dp)) {
                SummaryMetric(Icons.Default.SignalCellularAlt, stringResource(R.string.mobile), usage.mobileTotalBytes)
                SummaryMetric(Icons.Default.Wifi, stringResource(R.string.wifi), usage.wifiTotalBytes)
            }
        }
    }
}

@Composable
private fun SummaryMetric(icon: androidx.compose.ui.graphics.vector.ImageVector, label: String, bytes: Long) {
    val (value, unit) = formatDataParts(bytes)
    Column(horizontalAlignment = Alignment.End) {
        Row(verticalAlignment = Alignment.CenterVertically, horizontalArrangement = Arrangement.spacedBy(4.dp)) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary.copy(alpha = 0.85f), modifier = Modifier.size(14.dp))
            Text(label, fontSize = 10.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
        Text("$value $unit", fontSize = 14.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.onSurface)
    }
}

@Composable
fun SegmentedButton(text: String, isSelected: Boolean, modifier: Modifier = Modifier, onClick: () -> Unit) {
    Box(
        modifier = modifier
            .clip(RoundedCornerShape(10.dp))
            .background(if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surface.copy(alpha = 0.10f))
            .clickable { onClick() }
            .padding(vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(text, fontSize = 12.sp, fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium, color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
fun TableHeader() {
    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 13.dp, horizontal = 18.dp), verticalAlignment = Alignment.CenterVertically) {
        HeaderCell("DATE", modifier = Modifier.weight(1f))
        HeaderCell("MOBILE", Icons.Default.SignalCellularAlt, Modifier.weight(1f))
        HeaderCell("WIFI", Icons.Default.Wifi, Modifier.weight(1f))
        HeaderCell("TOTAL", Icons.Outlined.DonutLarge, Modifier.weight(1f))
    }
}

@Composable
private fun HeaderCell(text: String, icon: androidx.compose.ui.graphics.vector.ImageVector? = null, modifier: Modifier = Modifier) {
    Row(modifier = modifier, horizontalArrangement = if (icon == null) Arrangement.Start else Arrangement.End, verticalAlignment = Alignment.CenterVertically) {
        if (icon != null) {
            Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.onSurfaceVariant, modifier = Modifier.size(13.dp))
            Spacer(modifier = Modifier.width(5.dp))
        }
        Text(text, fontSize = 10.sp, fontWeight = FontWeight.SemiBold, color = MaterialTheme.colorScheme.onSurfaceVariant, letterSpacing = 0.8.sp)
    }
}

@Composable
fun UsageRow(usage: UsageInfo) {
    val dateParts = formatDateParts(usage.date)
    val dayOfWeek = formatDayOfWeek(usage.date)

    Row(modifier = Modifier.fillMaxWidth().padding(horizontal = 18.dp, vertical = 12.dp), verticalAlignment = Alignment.CenterVertically) {
        Column(modifier = Modifier.weight(1f)) {
            Text(dayOfWeek, color = MaterialTheme.colorScheme.onSurface, fontSize = 14.sp, fontWeight = FontWeight.Medium)
            Text("${dateParts.first} ${dateParts.second}".trim(), color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 10.sp)
        }

        val (mobValue, mobUnit) = formatDataParts(usage.mobileTotalBytes)
        Text("$mobValue $mobUnit", color = MaterialTheme.colorScheme.onSurface, fontSize = 13.sp, textAlign = TextAlign.End, modifier = Modifier.weight(1f))

        val (wifiValue, wifiUnit) = formatDataParts(usage.wifiTotalBytes)
        Text("$wifiValue $wifiUnit", color = MaterialTheme.colorScheme.onSurface, fontSize = 13.sp, textAlign = TextAlign.End, modifier = Modifier.weight(1f))

        val (totalValue, totalUnit) = formatDataParts(usage.totalBytes)
        Text("$totalValue $totalUnit", color = MaterialTheme.colorScheme.primary, fontSize = 13.sp, fontWeight = FontWeight.SemiBold, textAlign = TextAlign.End, modifier = Modifier.weight(1f))
    }

    HorizontalDivider(modifier = Modifier.padding(horizontal = 18.dp), thickness = 1.dp, color = MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.16f))
}

private fun formatDayOfWeek(dateString: String): String {
    if (dateString == "This Month" || dateString == "Today" || dateString == "Yesterday") return dateString
    return try {
        val date = LocalDate.parse(dateString)
        val today = LocalDate.now()
        when (date) {
            today -> "Today"
            today.minusDays(1) -> "Yesterday"
            else -> date.format(DateTimeFormatter.ofPattern("EEE", Locale.US))
        }
    } catch (_: Exception) {
        dateString
    }
}

private fun formatDateParts(dateString: String): Pair<String, String> {
    if (dateString == "This Month" || dateString == "Today" || dateString == "Yesterday") return Pair("", dateString)
    return try {
        val date = LocalDate.parse(dateString)
        val month = date.format(DateTimeFormatter.ofPattern("MMM", Locale.US))
        val day = date.format(DateTimeFormatter.ofPattern("dd"))
        Pair(month, day)
    } catch (_: Exception) {
        Pair("", dateString)
    }
}

private fun formatDataParts(bytes: Long): Pair<String, String> = FormatUtils.formatBytesValue(bytes) to FormatUtils.formatBytesUnit(bytes)
