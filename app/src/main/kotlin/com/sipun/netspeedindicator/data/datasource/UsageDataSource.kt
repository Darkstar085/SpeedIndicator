package com.sipun.netspeedindicator.data.datasource

import android.app.usage.NetworkStatsManager
import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.util.Log
import com.sipun.netspeedindicator.domain.model.UsageInfo
import dagger.hilt.android.qualifiers.ApplicationContext
import java.time.LocalDate
import java.time.ZoneId
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UsageDataSource @Inject constructor(
    @ApplicationContext private val context: Context
) {
    companion object { private const val TAG = "UsageDataSource" }
    private val networkStatsManager by lazy {
        context.getSystemService(Context.NETWORK_STATS_SERVICE) as? NetworkStatsManager
    }
    private val connectivityManager by lazy {
        context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    }

    fun getUsageForDate(date: String): UsageInfo? {
        val localDate = LocalDate.parse(date)
        val start = localDate.atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
        val end = localDate.plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant().toEpochMilli()
        return getUsageForPeriod(date, start, end)
    }

    private fun getUsageForPeriod(date: String, start: Long, end: Long): UsageInfo? {
        val stats = networkStatsManager ?: return null
        return try {
            val wifi = stats.querySummaryForDevice(ConnectivityManager.TYPE_WIFI, null, start, end)
            val mobile = stats.querySummaryForDevice(ConnectivityManager.TYPE_MOBILE, null, start, end)
            UsageInfo(date, wifi.rxBytes, wifi.txBytes, mobile.rxBytes, mobile.txBytes)
        } catch (e: Exception) {
            Log.e(TAG, "Failed to query usage for $date", e)
            null
        }
    }

    fun isWifiConnected(): Boolean {
        val network = connectivityManager.activeNetwork ?: return false
        val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
        return capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)
    }

    fun isMobileConnected(): Boolean {
        val network = connectivityManager.activeNetwork ?: return false
        val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
        return capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)
    }
}
