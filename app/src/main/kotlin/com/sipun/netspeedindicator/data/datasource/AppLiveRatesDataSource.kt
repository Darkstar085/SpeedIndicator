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
import com.sipun.netspeedindicator.domain.model.LiveRatesDiagnostics
import com.sipun.netspeedindicator.domain.model.LiveRatesStrategy
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
 * Primary strategy — `TrafficStats` per-UID counters over ALL installed
 * and common system UIDs: these are the same netd qtaguid counters the
 * total-speed path sums, so per-app rates stay consistent with the device
 * total.
 *
 * Secondary — `NetworkStatsManager` cumulative counters since boot (every
 * UID that transferred anything). Requires Usage Access; kept as fallback
 * for kernels where per-UID TrafficStats counters are unsupported.
 *
 * Last resort — `TrafficStats` launcher-visible apps only, when neither
 * works (no permission at all; misses non-launcher consumers).
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
        const val UID_COUNTERS_PROBE_MS = 5 * 60 * 1000L
        const val MODE_UID_COUNTERS = 1
        const val MODE_NETWORK_STATS = 2
        const val MODE_LAUNCHER = 3

        val SYSTEM_UIDS: Set<Int> = (1000..1099).toSet() + setOf(0, 2000, 2007, 2008, 2010, 2392)
    }

    private val networkStatsManager: NetworkStatsManager? by lazy {
        context.getSystemService(Context.NETWORK_STATS_SERVICE) as? NetworkStatsManager
    }

    @Volatile
    private var uidCountersSupported: Boolean? = null

    @Volatile
    private var uidCountersCheckedAt = 0L

    @Volatile
    private var activeMode = MODE_LAUNCHER

    private var job: Job? = null

    fun start(scope: CoroutineScope) {
        if (job?.isActive == true) return
        job = scope.launch(Dispatchers.Default) {
            var lastTotals: Map<Int, Long> = emptyMap()
            var lastTimestamp = SystemClock.elapsedRealtime()
            var lastSampleEndMs = 0L
            var mode = 0 // last strategy used; counter bases differ, re-baseline on change
            while (isActive) {
                delay(POLL_INTERVAL_MS)

                val allUids = allTrafficUids()
                val newMode = when {
                    hasNetworkStatsAccess() -> MODE_NETWORK_STATS
                    uidCountersSupported(allUids) -> MODE_UID_COUNTERS
                    else -> MODE_LAUNCHER
                }
                if (newMode != mode) {
                    // Counter bases differ between strategies; re-baseline on switch.
                    lastTotals = emptyMap()
                    lastSampleEndMs = 0L
                    lastTimestamp = SystemClock.elapsedRealtime()
                    mode = newMode
                }
                activeMode = mode

                val rates = if (hasNetworkStatsAccess() && mode == MODE_NETWORK_STATS) {
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

                    val uids = if (mode == MODE_LAUNCHER) {
                        installedAppsProvider.launcherApps().keys
                    } else {
                        allUids
                    }
                    val totals = readTrafficStatsTotals(uids)
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

    /** Snapshot for the in-app diagnostics popup (long-press the LIVE NOW header). */
    fun diagnostics(): LiveRatesDiagnostics {
        val uids = allTrafficUids()
        var good = 0
        var bad = 0
        for (uid in uids) {
            val rx = TrafficStats.getUidRxBytes(uid)
            val tx = TrafficStats.getUidTxBytes(uid)
            if (rx >= 0L && tx >= 0L) good++ else bad++
        }
        return LiveRatesDiagnostics(
            strategy = when (activeMode) {
                MODE_UID_COUNTERS -> LiveRatesStrategy.UID_COUNTERS
                MODE_NETWORK_STATS -> LiveRatesStrategy.NETWORK_STATS
                else -> LiveRatesStrategy.LAUNCHER_ONLY
            },
            usageAccess = hasNetworkStatsAccess(),
            uidCountersReadable = good,
            uidCountersUnsupported = bad
        )
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

    private fun allTrafficUids(): Set<Int> = installedAppsProvider.allApps().keys + SYSTEM_UIDS

    /**
     * Probe whether per-UID counters are readable on this device, cached for
     * [UID_COUNTERS_PROBE_MS]. When unsupported (every counter reads -1) the
     * caller falls back to NetworkStatsManager.
     */
    private fun uidCountersSupported(uids: Set<Int>): Boolean {
        val now = System.currentTimeMillis()
        uidCountersSupported?.let { cached ->
            if (now - uidCountersCheckedAt < UID_COUNTERS_PROBE_MS) return cached
        }
        val supported = readTrafficStatsTotals(uids).isNotEmpty()
        uidCountersSupported = supported
        uidCountersCheckedAt = now
        return supported
    }

    /** Per-UID counters for the given UID set (cumulative since boot). */
    private fun readTrafficStatsTotals(uids: Set<Int>): Map<Int, Long> {
        val result = HashMap<Int, Long>(uids.size)
        for (uid in uids) {
            val rx = TrafficStats.getUidRxBytes(uid)
            val tx = TrafficStats.getUidTxBytes(uid)
            if (rx >= 0L && tx >= 0L) {
                result[uid] = (result[uid] ?: 0L) + rx + tx
            }
        }
        return result
    }
}
