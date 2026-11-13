package com.sipun.netspeedindicator.data.datasource

import android.app.usage.NetworkStats
import android.app.usage.NetworkStatsManager
import android.content.Context
import android.net.ConnectivityManager
import android.net.TrafficStats
import android.os.SystemClock
import android.util.Log
import com.sipun.netspeedindicator.core.state.TrafficStateManager
import com.sipun.netspeedindicator.core.util.PermissionUtils
import com.sipun.netspeedindicator.domain.repository.InstalledAppsProvider
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * Polls per-UID network usage once per second while monitoring is active
 * and publishes total bytes/second into [TrafficStateManager].
 *
 * Primary strategy — `NetworkStatsManager`: attributes rx/tx to every UID
 * (background apps, DownloadManager, Google Play services, system) over a
 * small sliding window, which is the same source the App Usage history
 * uses. Requires Usage Access.
 *
 * Fallback — `TrafficStats` per-UID counters for launcher-visible apps,
 * used only when Usage Access is not granted (works without any extra
 * permission, but misses non-launcher consumers and can report
 * UNSUPPORTED on some devices).
 */
@Singleton
class AppLiveRatesDataSource @Inject constructor(
    @ApplicationContext private val context: Context,
    private val installedAppsProvider: InstalledAppsProvider,
    private val trafficStateManager: TrafficStateManager
) {
    private companion object {
        const val POLL_INTERVAL_MS = 1_000L
        const val TAG = "AppLiveRatesDataSource"
    }

    private val networkStatsManager: NetworkStatsManager? by lazy {
        context.getSystemService(Context.NETWORK_STATS_SERVICE) as? NetworkStatsManager
    }

    private var job: Job? = null

    fun start(scope: CoroutineScope) {
        if (job?.isActive == true) return
        job = scope.launch(Dispatchers.Default) {
            var lastTotals: Map<Int, Long> = emptyMap()
            var lastTimestamp = SystemClock.elapsedRealtime()
            var lastWindowEndMs = System.currentTimeMillis()
            while (isActive) {
                delay(POLL_INTERVAL_MS)

                val rates = if (hasNetworkStatsAccess()) {
                    lastTimestamp = SystemClock.elapsedRealtime()
                    val windowEnd = System.currentTimeMillis()
                    val windowStart = lastWindowEndMs
                    lastWindowEndMs = windowEnd
                    readNetworkStatsRates(windowStart, windowEnd)
                } else {
                    lastWindowEndMs = System.currentTimeMillis()
                    val now = SystemClock.elapsedRealtime()
                    val elapsed = (now - lastTimestamp).coerceAtLeast(1L)
                    lastTimestamp = now

                    val totals = readTrafficStatsTotals()
                    val deltas = HashMap<Int, Long>(totals.size)
                    for ((uid, total) in totals) {
                        val last = lastTotals[uid] ?: total
                        var delta = total - last
                        if (delta < 0) delta = total // counter reset
                        deltas[uid] = (delta * 1000L / elapsed).coerceAtLeast(0L)
                    }
                    lastTotals = totals
                    deltas
                }
                trafficStateManager.updateAppLiveRates(rates)
            }
        }
    }

    fun stop() {
        job?.cancel()
        job = null
        trafficStateManager.updateAppLiveRates(emptyMap())
    }

    private fun hasNetworkStatsAccess(): Boolean =
        networkStatsManager != null && PermissionUtils.hasUsageStatsPermission(context)

    /**
     * Rates (bytes/second) for every UID that transferred anything during
     * [startMs, endMs). UIDs with no traffic in the window are omitted.
     */
    @Suppress("DEPRECATION")
    private fun readNetworkStatsRates(startMs: Long, endMs: Long): Map<Int, Long> {
        val totals = HashMap<Int, Long>()
        for (networkType in intArrayOf(ConnectivityManager.TYPE_WIFI, ConnectivityManager.TYPE_MOBILE)) {
            try {
                networkStatsManager?.querySummary(networkType, null, startMs, endMs)?.use { stats ->
                    val bucket = NetworkStats.Bucket()
                    while (stats.hasNextBucket()) {
                        stats.getNextBucket(bucket)
                        if (bucket.uid < 0) continue
                        val rx = bucket.rxBytes.coerceAtLeast(0L)
                        val tx = bucket.txBytes.coerceAtLeast(0L)
                        if (rx + tx > 0L) {
                            totals[bucket.uid] = (totals[bucket.uid] ?: 0L) + rx + tx
                        }
                    }
                }
            } catch (e: Exception) {
                Log.w(TAG, "querySummary failed for network type $networkType", e)
            }
        }
        val windowMs = (endMs - startMs).coerceAtLeast(1L)
        return totals.mapValues { (_, bytes) -> bytes * 1000L / windowMs }
    }

    /** Fallback: cumulative per-UID counters for launcher-visible apps. */
    private fun readTrafficStatsTotals(): Map<Int, Long> {
        val result = HashMap<Int, Long>()
        for (uid in installedAppsProvider.launcherApps().keys) {
            val rx = TrafficStats.getUidRxBytes(uid)
            val tx = TrafficStats.getUidTxBytes(uid)
            if (rx >= 0L && tx >= 0L) {
                result[uid] = (result[uid] ?: 0L) + rx + tx
            }
        }
        return result
    }
}
