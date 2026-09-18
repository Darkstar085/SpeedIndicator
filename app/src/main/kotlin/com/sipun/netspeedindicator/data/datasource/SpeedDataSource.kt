package com.sipun.netspeedindicator.data.datasource

import android.net.TrafficStats
import android.os.SystemClock
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject
import javax.inject.Singleton

data class TrafficSample(
    val downloadBytes: Long,
    val uploadBytes: Long,
    val downloadBytesPerSecond: Long,
    val uploadBytesPerSecond: Long,
    val elapsedMillis: Long
)

/**
 * Data source for real-time speed monitoring.
 * Uses Android TrafficStats to measure both byte deltas and rates.
 */
@Singleton
class SpeedDataSource @Inject constructor() {

    fun observeSpeed(intervalMs: Long = 1000L): Flow<TrafficSample> = flow {
        var lastRxBytes = TrafficStats.getTotalRxBytes()
        var lastTxBytes = TrafficStats.getTotalTxBytes()
        var lastTimestamp = SystemClock.elapsedRealtime()

        while (true) {
            delay(intervalMs)

            val currentRxBytes = TrafficStats.getTotalRxBytes()
            val currentTxBytes = TrafficStats.getTotalTxBytes()
            val currentTimestamp = SystemClock.elapsedRealtime()

            var rxDelta = currentRxBytes - lastRxBytes
            var txDelta = currentTxBytes - lastTxBytes
            val elapsedMillis = (currentTimestamp - lastTimestamp).coerceAtLeast(1L)

            // Handle counter reset (for example after reboot or counter rollover).
            if (rxDelta < 0) rxDelta = currentRxBytes.coerceAtLeast(0L)
            if (txDelta < 0) txDelta = currentTxBytes.coerceAtLeast(0L)

            val downloadSpeed = (rxDelta * 1000L / elapsedMillis).coerceAtLeast(0L)
            val uploadSpeed = (txDelta * 1000L / elapsedMillis).coerceAtLeast(0L)

            lastRxBytes = currentRxBytes
            lastTxBytes = currentTxBytes
            lastTimestamp = currentTimestamp

            emit(
                TrafficSample(
                    downloadBytes = rxDelta,
                    uploadBytes = txDelta,
                    downloadBytesPerSecond = downloadSpeed,
                    uploadBytesPerSecond = uploadSpeed,
                    elapsedMillis = elapsedMillis
                )
            )
        }
    }

    fun getCurrentTotalBytes(): Pair<Long, Long> {
        return Pair(
            TrafficStats.getTotalRxBytes(),
            TrafficStats.getTotalTxBytes()
        )
    }
}
