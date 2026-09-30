package com.sipun.netspeedindicator.ui.screens.main.appusage

import android.content.Intent
import android.provider.Settings
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Sort
import androidx.compose.material.icons.filled.BarChart
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.Button
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.core.graphics.drawable.toBitmap
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.sipun.netspeedindicator.R
import com.sipun.netspeedindicator.core.util.FormatUtils
import com.sipun.netspeedindicator.ui.components.AppCard
import com.sipun.netspeedindicator.ui.components.AppTopBar
import com.sipun.netspeedindicator.ui.theme.dimens
import com.sipun.netspeedindicator.domain.model.AppDataUsage

private val WifiColor = Color(0xFF29B6F6)
private val MobileColor = Color(0xFFEC6FA9)

private enum class AppUsageSort(val labelRes: Int) {
    HIGHEST_USAGE(R.string.sort_highest_usage),
    LOWEST_USAGE(R.string.sort_lowest_usage),
    NAME_A_TO_Z(R.string.sort_name_a_to_z),
    NAME_Z_TO_A(R.string.sort_name_z_to_a)
}


@Composable
fun AppDataUsageScreen(viewModel: AppDataUsageViewModel = hiltViewModel()) {
    val state by viewModel.uiState.collectAsState()
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    var searchQuery by remember { mutableStateOf("") }
    var showSearchDialog by remember { mutableStateOf(false) }
    var showSortMenu by remember { mutableStateOf(false) }
    var sortOption by remember { mutableStateOf(AppUsageSort.HIGHEST_USAGE) }

    val filteredApps = remember(state.apps, searchQuery, sortOption) {
        val query = searchQuery.trim()
        val matchingApps = if (query.isEmpty()) {
            state.apps
        } else {
            state.apps.filter {
                it.appName.contains(query, ignoreCase = true) ||
                    it.packageName.contains(query, ignoreCase = true)
            }
        }

        when (sortOption) {
            AppUsageSort.HIGHEST_USAGE -> matchingApps.sortedByDescending { it.totalBytes }
            AppUsageSort.LOWEST_USAGE -> matchingApps.sortedBy { it.totalBytes }
            AppUsageSort.NAME_A_TO_Z -> matchingApps.sortedBy { it.appName.lowercase() }
            AppUsageSort.NAME_Z_TO_A -> matchingApps.sortedByDescending { it.appName.lowercase() }
        }
    }

    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) viewModel.refresh()
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    if (showSearchDialog) {
        AlertDialog(
            onDismissRequest = { showSearchDialog = false },
            title = { Text(stringResource(R.string.search_apps)) },
            text = {
                OutlinedTextField(
                    value = searchQuery,
                    onValueChange = { searchQuery = it },
                    modifier = Modifier.fillMaxWidth(),
                    singleLine = true,
                    leadingIcon = {
                        Icon(Icons.Default.Search, contentDescription = null)
                    },
                    placeholder = { Text(stringResource(R.string.app_name_or_package)) },
                    shape = RoundedCornerShape(16.dp)
                )
            },
            confirmButton = {
                TextButton(onClick = { showSearchDialog = false }) {
                    Text(stringResource(R.string.done))
                }
            },
            dismissButton = {
                if (searchQuery.isNotBlank()) {
                    TextButton(onClick = { searchQuery = "" }) {
                        Text(stringResource(R.string.clear))
                    }
                }
            }
        )
    }

    LazyColumn(
        modifier = Modifier.fillMaxSize(),
        verticalArrangement = Arrangement.spacedBy(12.dp),
        contentPadding = PaddingValues(bottom = 98.dp)
    ) {
        item {
            AppTopBar(
                title = stringResource(R.string.app_data_usage_title),
                subTitle = stringResource(R.string.app_data_usage_desc),
                trailingContent = {
                    IconButton(onClick = { showSearchDialog = true }) {
                        Icon(
                            Icons.Default.Search,
                            contentDescription = stringResource(R.string.search_apps)
                        )
                    }
                    IconButton(onClick = viewModel::refresh) {
                        Icon(
                            Icons.Default.Refresh,
                            contentDescription = "Refresh"
                        )
                    }
                }
            )
        }

        item {
            Row(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = dimens.horizontalPadding)
                    .clip(RoundedCornerShape(14.dp))
                    .background(MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.09f))
                    .padding(3.dp),
                horizontalArrangement = Arrangement.spacedBy(2.dp)
            ) {
                AppUsagePeriod.entries.forEach { period ->
                    AppUsageSegmentedButton(
                        text = period.label,
                        isSelected = state.period == period,
                        modifier = Modifier.weight(1f),
                        onClick = { viewModel.setPeriod(period) }
                    )
                }
            }
        }

        if (!state.hasUsageAccess) {
            item { UsageAccessCard(context, Modifier.padding(horizontal = dimens.horizontalPadding)) }
        } else {
            item { UsageSummaryCard(state, Modifier.padding(horizontal = dimens.horizontalPadding)) }

            if (state.isLoading) {
                item {
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .padding(horizontal = dimens.horizontalPadding, vertical = 48.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        CircularProgressIndicator()
                    }
                }
            } else if (filteredApps.isEmpty()) {
                item {
                    EmptyUsageCard(
                        message = if (searchQuery.isBlank()) {
                            stringResource(R.string.no_app_usage)
                        } else {
                            stringResource(R.string.no_apps_match, searchQuery)
                        },
                        modifier = Modifier.padding(horizontal = dimens.horizontalPadding)
                    )
                }
            } else {
                item {
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .padding(horizontal = dimens.horizontalPadding),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(
                            stringResource(R.string.apps_count, filteredApps.size),
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.SemiBold,
                            modifier = Modifier.weight(1f)
                        )
                        Box {
                            IconButton(onClick = { showSortMenu = true }) {
                                Icon(
                                    Icons.AutoMirrored.Filled.Sort,
                                    contentDescription = stringResource(R.string.sort_apps),
                                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                                )
                            }
                            DropdownMenu(
                                expanded = showSortMenu,
                                onDismissRequest = { showSortMenu = false }
                            ) {
                                AppUsageSort.entries.forEach { option ->
                                    DropdownMenuItem(
                                        text = { Text(stringResource(option.labelRes)) },
                                        onClick = {
                                            sortOption = option
                                            showSortMenu = false
                                        }
                                    )
                                }
                            }
                        }
                    }
                }

                items(
                    items = filteredApps,
                    key = { app -> app.uid.toString() + ":" + app.packageName }
                ) { app ->
                    AppUsageRow(app, Modifier.padding(horizontal = dimens.horizontalPadding))
                }
            }
        }
    }
}



@Composable
private fun AppUsageSegmentedButton(
    text: String,
    isSelected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Box(
        modifier
            .clip(RoundedCornerShape(10.dp))
            .background(
                if (isSelected) {
                    MaterialTheme.colorScheme.primary
                } else {
                    MaterialTheme.colorScheme.surface.copy(alpha = 0.10f)
                }
            )
            .clickable { onClick() }
            .padding(vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text,
            fontSize = 12.sp,
            fontWeight = if (isSelected) FontWeight.SemiBold else FontWeight.Medium,
            color = if (isSelected) {
                MaterialTheme.colorScheme.onPrimary
            } else {
                MaterialTheme.colorScheme.onSurfaceVariant
            }
        )
    }
}

@Composable
private fun UsageAccessCard(context: android.content.Context, modifier: Modifier = Modifier) {
    AppCard(modifier.fillMaxWidth()) {
        Column(Modifier.padding(20.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Icon(
                Icons.Default.BarChart,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
            Text(
                stringResource(R.string.usage_access_required),
                style = MaterialTheme.typography.titleMedium,
                fontWeight = FontWeight.SemiBold
            )
            Text(
                stringResource(R.string.app_usage_access_desc),
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Button(onClick = {
                context.startActivity(Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS))
            }) {
                Icon(Icons.Default.Settings, contentDescription = null)
                Spacer(Modifier.size(8.dp))
                Text(stringResource(R.string.grant_access))
            }
        }
    }
}

@Composable
private fun UsageSummaryCard(state: AppDataUsageUiState, modifier: Modifier = Modifier) {
    AppCard(modifier.fillMaxWidth()) {
        Column(
            Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.Bottom) {
                Column(Modifier.weight(1f)) {
                    Text(
                        stringResource(R.string.total_data),
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                    Text(
                        FormatUtils.formatBytes(state.totalBytes),
                        style = MaterialTheme.typography.headlineMedium,
                        fontWeight = FontWeight.Bold
                    )
                }
                Text(
                    state.period.label,
                    style = MaterialTheme.typography.labelMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            SplitUsageBar(state.wifiBytes, state.mobileBytes)

            Row(
                Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                NetworkSummaryItem(
                    label = "Wi-Fi",
                    bytes = state.wifiBytes,
                    color = WifiColor,
                    modifier = Modifier.weight(1f),
                    alignEnd = false
                )

                Box(
                    Modifier
                        .width(1.dp)
                        .height(42.dp)
                        .background(
                            MaterialTheme.colorScheme.outlineVariant.copy(alpha = 0.55f)
                        )
                )

                NetworkSummaryItem(
                    label = "Mobile",
                    bytes = state.mobileBytes,
                    color = MobileColor,
                    modifier = Modifier.weight(1f),
                    alignEnd = true
                )
            }
        }
    }
}

@Composable
private fun NetworkSummaryItem(
    label: String,
    bytes: Long,
    color: Color,
    modifier: Modifier,
    alignEnd: Boolean
) {
    Column(
        modifier
            .padding(horizontal = 12.dp),
        horizontalAlignment = if (alignEnd) Alignment.End else Alignment.Start
    ) {
        Row(
            Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = if (alignEnd) Arrangement.End else Arrangement.Start
        ) {
            Box(Modifier.size(8.dp).background(color, CircleShape))
            Spacer(Modifier.size(7.dp))
            Text(
                label,
                style = MaterialTheme.typography.labelMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
        Text(
            FormatUtils.formatBytes(bytes),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold,
            textAlign = if (alignEnd) TextAlign.End else TextAlign.Start
        )
    }
}

@Composable
private fun SplitUsageBar(
    wifiBytes: Long,
    mobileBytes: Long,
    modifier: Modifier = Modifier
) {
    val wifiWeight = wifiBytes.toFloat().coerceAtLeast(0.001f)
    val mobileWeight = mobileBytes.toFloat().coerceAtLeast(0.001f)

    Row(
        modifier.fillMaxWidth().height(6.dp)
    ) {
        Box(
            Modifier
                .weight(wifiWeight)
                .height(6.dp)
                .background(WifiColor, RoundedCornerShape(4.dp))
        )
        Box(
            Modifier
                .weight(mobileWeight)
                .height(6.dp)
                .background(MobileColor, RoundedCornerShape(4.dp))
        )
    }
}

@Composable
private fun EmptyUsageCard(message: String, modifier: Modifier = Modifier) {
    AppCard(modifier.fillMaxWidth()) {
        Column(
            Modifier.fillMaxWidth().padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Icon(
                Icons.Default.BarChart,
                contentDescription = null,
                modifier = Modifier.size(32.dp),
                tint = MaterialTheme.colorScheme.onSurfaceVariant
            )
            Text(
                message,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}

@Composable
private fun AppUsageRow(app: AppDataUsage, modifier: Modifier = Modifier) {
    val context = LocalContext.current
    val density = LocalDensity.current
    val iconSizePx = with(density) { 52.dp.roundToPx() }
    val icon = remember(app.packageName, iconSizePx) {
        runCatching {
            context.packageManager.getApplicationIcon(app.packageName)
                .toBitmap(width = iconSizePx, height = iconSizePx)
                .asImageBitmap()
        }.getOrNull()
    }

    AppCard(modifier.fillMaxWidth()) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = 12.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (icon != null) {
                Image(
                    icon,
                    contentDescription = app.appName,
                    Modifier.size(52.dp)
                )
            } else {
                Spacer(Modifier.size(52.dp))
            }

            Spacer(Modifier.size(8.dp))

            Column(
                Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(5.dp)
            ) {
                Row(
                    Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        app.appName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.weight(1f)
                    )

                    Text(
                        FormatUtils.formatBytes(app.totalBytes),
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.Bold
                    )
                }

                SplitUsageBar(
                    wifiBytes = app.wifiBytes,
                    mobileBytes = app.mobileBytes
                )

                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    NetworkValue("Wi-Fi", app.wifiBytes, WifiColor)
                    NetworkValue("Mobile", app.mobileBytes, MobileColor)
                }
            }
        }
    }
}

@Composable
private fun NetworkValue(label: String, bytes: Long, color: Color) {
    Row(verticalAlignment = Alignment.CenterVertically) {
        Box(Modifier.size(7.dp).background(color, CircleShape))
        Spacer(Modifier.size(5.dp))
        Text(
            label + " " + FormatUtils.formatBytes(bytes),
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
