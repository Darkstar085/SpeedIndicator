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
 * Primary strategy — `NetworkStatsManager` cumulative counters since boot:
 * attributes rx/tx to every UID (background apps, DownloadManager, Google
 * Play services, system). Android flushes per-UID accounting asynchronously,
 * so a narrow time window sees only a fraction of a running transfer;
 * diffing since-boot counters is byte-accurate. Requires Usage Access.
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
            var lastSampleEndMs = 0L
            var mode = 0 // 1 = NetworkStats cumulative, 2 = TrafficStats counters
            while (isActive) {
                delay(POLL_INTERVAL_MS)

                val newMode = if (hasNetworkStatsAccess()) 1 else 2
                if (newMode != mode) {
                    // Counter bases differ between strategies; re-baseline on switch.
                    lastTotals = emptyMap()
                    lastSampleEndMs = 0L
                    lastTimestamp = SystemClock.elapsedRealtime()
                    mode = newMode
                }

                val rates = if (hasNetworkStatsAccess()) {
                    lastTimestamp = SystemClock.elapsedRealtime()
                    val nowMs = System.currentTimeMillis()
                    val totals = readNetworkStatsTotals()
                    val elapsed = if (lastSampleEndMs == 0L) {
                        POLL_INTERVAL_MS
                    } else {
                        (nowMs - lastSampleEndMs).coerceAtLeast(1L)
                    }
                    lastSampleEndMs = nowMs
                    deltas(totals, lastTotals, elapsed).also { lastTotals = totals }
                } else {
                    lastSampleEndMs = 0L
                    val now = SystemClock.elapsedRealtime()
                    val elapsed = (now - lastTimestamp).coerceAtLeast(1L)
                    lastTimestamp = now

                    val totals = readTrafficStatsTotals()
                    deltas(totals, lastTotals, elapsed).also { lastTotals = totals }
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
     * Rates = counter deltas between samples, normalized to bytes/second.
     * Never substitutes the whole cumulative counter for a negative delta —
     * Android can legitimately lower per-UID counters (netd retags/prunes
     * records, a query fails transiently), which would otherwise report
     * the since-boot total as one second of traffic.
     */
    private fun deltas(
        totals: Map<Int, Long>,
        last: Map<Int, Long>,
        elapsedMs: Long
    ): Map<Int, Long> {
        val out = HashMap<Int, Long>(totals.size)
        for ((uid, total) in totals) {
            val lastValue = last[uid] ?: continue // no baseline yet
            if (total <= lastValue) continue // no growth or source dropped
            val delta = total - lastValue
            out[uid] = (delta * 1000L / elapsedMs).coerceAtLeast(0L)
        }
        return out
    }

    /**
     * Cumulative since-boot byte counters per UID, Wi-Fi and mobile
     * combined. Diffed between samples to get rates; the source counters
     * are monotonic, so no traffic is lost even though accounting is
     * flushed asynchronously (rates lag by a fraction of a second).
     */
    @Suppress("DEPRECATION")
    private fun readNetworkStatsTotals(): Map<Int, Long> {
        val totals = HashMap<Int, Long>()
        val now = System.currentTimeMillis()
        for (networkType in intArrayOf(ConnectivityManager.TYPE_WIFI, ConnectivityManager.TYPE_MOBILE)) {
            try {
                networkStatsManager?.querySummary(networkType, null, 0L, now)?.use { stats ->
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
        return totals
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
