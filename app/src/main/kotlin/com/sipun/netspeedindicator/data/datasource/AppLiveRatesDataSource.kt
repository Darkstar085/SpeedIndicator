package com.sipun.netspeedindicator.data.datasource

import android.content.Context
import android.content.Intent
import android.net.TrafficStats
import android.os.SystemClock
import com.sipun.netspeedindicator.core.state.TrafficStateManager
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Polls per-UID traffic counters (only for launcher-visible apps) while
 * monitoring and live per-app rates are enabled, and publishes deltas as
 * total bytes/second into [TrafficStateManager].
 *
 * Deliberately opt-in: it reads two counters per visible app per poll.
 */
@Singleton
class AppLiveRatesDataSource @Inject constructor(
    @ApplicationContext private val context: Context,
    private val trafficStateManager: TrafficStateManager
) {
    private companion object {
        const val POLL_INTERVAL_MS = 2_000L
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
        val packageManager = context.packageManager
        val launcherIntent = Intent(Intent.ACTION_MAIN)
            .addCategory(Intent.CATEGORY_LAUNCHER)
        val result = HashMap<Int, Long>()
        for (info in packageManager.queryIntentActivities(launcherIntent, 0)) {
            val uid = info.activityInfo?.applicationInfo?.uid ?: continue
            val rx = TrafficStats.getUidRxBytes(uid)
            val tx = TrafficStats.getUidTxBytes(uid)
            if (rx >= 0L && tx >= 0L) {
                result[uid] = (result[uid] ?: 0L) + rx + tx
            }
        }
        return result
    }
}
