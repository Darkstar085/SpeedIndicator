package com.sipun.netspeedindicator.data.datasource

import android.net.TrafficStats
import android.os.SystemClock
import com.sipun.netspeedindicator.core.state.TrafficStateManager
import com.sipun.netspeedindicator.domain.repository.InstalledAppsProvider
import javax.inject.Inject
import javax.inject.Singleton
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

/**
 * Polls per-UID traffic counters (only for launcher-visible apps) while
 * monitoring is active, and publishes deltas as total bytes/second into
 * [TrafficStateManager].
 *
 * Runs once per second so every consumer — the dashboard's live per-app
 * card and the App Usage screen's LIVE badges — sees fresh rates. The
 * launcher app list itself is cached via [InstalledAppsProvider], so a
 * poll costs two [TrafficStats] counter reads per visible app.
 */
@Singleton
class AppLiveRatesDataSource @Inject constructor(
    private val installedAppsProvider: InstalledAppsProvider,
    private val trafficStateManager: TrafficStateManager
) {
    private companion object {
        const val POLL_INTERVAL_MS = 1_000L
    }

    private var job: Job? = null

    fun start(scope: CoroutineScope) {
        if (job?.isActive == true) return
        job = scope.launch(Dispatchers.Default) {
            var lastTotals: Map<Int, Long> = emptyMap()
            var lastTimestamp = SystemClock.elapsedRealtime()
            while (isActive) {
                delay(POLL_INTERVAL_MS)
                val now = SystemClock.elapsedRealtime()
                val elapsed = (now - lastTimestamp).coerceAtLeast(1L)
                lastTimestamp = now

                val totals = readTotals()
                if (totals.isEmpty()) continue
                val deltas = HashMap<Int, Long>(totals.size)
                for ((uid, total) in totals) {
                    val last = lastTotals[uid] ?: total
                    var delta = total - last
                    if (delta < 0) delta = total // counter reset
                    deltas[uid] = (delta * 1000L / elapsed).coerceAtLeast(0L)
                }
                lastTotals = totals
                trafficStateManager.updateAppLiveRates(deltas)
            }
        }
    }

    fun stop() {
        job?.cancel()
        job = null
        trafficStateManager.updateAppLiveRates(emptyMap())
    }

    private fun readTotals(): Map<Int, Long> {
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
