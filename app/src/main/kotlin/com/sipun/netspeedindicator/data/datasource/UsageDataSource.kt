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

/**
 * Data source for accurate usage tracking
 * Uses NetworkStatsManager for detailed network statistics
 * Requires PACKAGE_USAGE_STATS permission
 */
@Singleton
class UsageDataSource @Inject constructor(
    @ApplicationContext private val context: Context
) {

    companion object {
        private const val TAG = "UsageDataSource"
    }


    private val networkStatsManager: NetworkStatsManager? by lazy {
        context.getSystemService(Context.NETWORK_STATS_SERVICE) as? NetworkStatsManager
    }
    
    private val connectivityManager: ConnectivityManager by lazy {
        context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
    }

    /**
     * Get usage for a specific date
     * @param date Format: yyyy-MM-dd
     * @return UsageModel or null if permission missing/error
     */
    fun getUsageForDate(date: String): UsageInfo? {
        val localDate = LocalDate.parse(date)
        val startOfDay = localDate.atStartOfDay(ZoneId.systemDefault()).toInstant()
        val endOfDay = localDate.plusDays(1).atStartOfDay(ZoneId.systemDefault()).toInstant()
        
        return getUsageForPeriod(date, startOfDay.toEpochMilli(), endOfDay.toEpochMilli())
    }
    
    /**
     * Get usage for a time period
     */
    private fun getUsageForPeriod(dateStr: String, startMillis: Long, endMillis: Long): UsageInfo? {
        val statsManager = networkStatsManager
        
        if (statsManager == null) {
            return null
        }
        
        try {
            val wifiSummary = statsManager.querySummaryForDevice(
                ConnectivityManager.TYPE_WIFI,
                null,
                startMillis,
                endMillis
            )
            
            val wifiRx = wifiSummary.rxBytes
            val wifiTx = wifiSummary.txBytes
            
            val mobileSummary = statsManager.querySummaryForDevice(
                ConnectivityManager.TYPE_MOBILE,
                null,
                startMillis,
                endMillis
            )
            
            val mobileRx = mobileSummary.rxBytes
            val mobileTx = mobileSummary.txBytes
            
            return UsageInfo(
                date = dateStr,
                wifiRxBytes = wifiRx,
                wifiTxBytes = wifiTx,
                mobileRxBytes = mobileRx,
                mobileTxBytes = mobileTx
            )
            
        } catch (e: Exception) {
            // Permission not granted or other error
            Log.e(TAG, "Failed to query usage for $dateStr", e)
            return null
        }
    }
    
    /**
     * Check if currently connected to WiFi
     */
    fun isWifiConnected(): Boolean {
        val network = connectivityManager.activeNetwork ?: return false
        val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
        return capabilities.hasTransport(NetworkCapabilities.TRANSPORT_WIFI)
    }
    
    /**
     * Check if currently connected to mobile data
     */
    fun isMobileConnected(): Boolean {
        val network = connectivityManager.activeNetwork ?: return false
        val capabilities = connectivityManager.getNetworkCapabilities(network) ?: return false
        return capabilities.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR)
    }
}
