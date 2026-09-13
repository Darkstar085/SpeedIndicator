package com.sipun.netspeedindicator.data.datasource

import android.app.usage.NetworkStatsManager
import android.content.Context
import android.net.ConnectivityManager
import android.util.Log
import com.sipun.netspeedindicator.domain.model.UsageInfo
import dagger.hilt.android.qualifiers.ApplicationContext
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Data source for historical usage tracking.
 * Uses NetworkStatsManager transport-specific device summaries so Wi-Fi and
 * mobile totals are independent of the currently active network.
 * Requires PACKAGE_USAGE_STATS permission.
 */
@Singleton
class UsageDataSource @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object { private const val TAG = "UsageDataSource" }

    private val networkStatsManager: NetworkStatsManager? by lazy {
        context.getSystemService(Context.NETWORK_STATS_SERVICE) as? NetworkStatsManager
    }

    fun getUsageForDate(date: String): UsageInfo? {
        val localDate = LocalDate.parse(date)
        val startOfDay = localDate.atStartOfDay(ZoneId.systemDefault()).toInstant()
        val endOfDay = localDate.plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant()
        return getUsageForPeriod(date, startOfDay.toEpochMilli(), endOfDay.toEpochMilli())
    }

    private fun getUsageForPeriod(dateStr: String, startMillis: Long, endMillis: Long): UsageInfo? {
        val statsManager = networkStatsManager ?: return null
        return try {
            val wifiSummary = statsManager.querySummaryForDevice(ConnectivityManager.TYPE_WIFI, null, startMillis, endMillis)
            val mobileSummary = statsManager.querySummaryForDevice(ConnectivityManager.TYPE_MOBILE, null, startMillis, endMillis)
            UsageInfo(
                date = dateStr,
                wifiRxBytes = wifiSummary.rxBytes,
                wifiTxBytes = wifiSummary.txBytes,
                mobileRxBytes = mobileSummary.rxBytes,
                mobileTxBytes = mobileSummary.txBytes
            )
        } catch (e: SecurityException) {
            Log.w(TAG, "Network usage permission is unavailable for $dateStr", e)
            null
        } catch (e: Exception) {
            Log.e(TAG, "Failed to query usage for $dateStr", e)
            null
        }
    }
}
