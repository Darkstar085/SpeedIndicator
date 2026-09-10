package com.sipun.netspeedindicator.data.datasource

import android.net.TrafficStats
import android.os.SystemClock
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Data source for real-time device-wide network speed monitoring.
 * Uses Android TrafficStats API to track total network bytes.
 */
@Singleton
class SpeedDataSource @Inject constructor() {

    fun observeSpeed(intervalMs: Long = 1000L): Flow<Pair<Long, Long>> = flow {
        var lastRxBytes = TrafficStats.getTotalRxBytes()
        var lastTxBytes = TrafficStats.getTotalTxBytes()
        var lastTimestamp = SystemClock.elapsedRealtime()

        while (true) {
            delay(intervalMs)

            val currentRxBytes = TrafficStats.getTotalRxBytes()
            val currentTxBytes = TrafficStats.getTotalTxBytes()
            val currentTimestamp = SystemClock.elapsedRealtime()

            if (currentRxBytes < 0L || currentTxBytes < 0L) {
                emit(0L to 0L)
                lastRxBytes = currentRxBytes
                lastTxBytes = currentTxBytes
                lastTimestamp = currentTimestamp
                continue
            }

            val rxDelta = (currentRxBytes - lastRxBytes).takeIf { it >= 0L } ?: 0L
            val txDelta = (currentTxBytes - lastTxBytes).takeIf { it >= 0L } ?: 0L
            val timeDelta = currentTimestamp - lastTimestamp

            val downloadSpeed = bytesPerSecond(rxDelta, timeDelta)
            val uploadSpeed = bytesPerSecond(txDelta, timeDelta)

            lastRxBytes = currentRxBytes
            lastTxBytes = currentTxBytes
            lastTimestamp = currentTimestamp

            emit(downloadSpeed to uploadSpeed)
        }
    }

    private fun bytesPerSecond(deltaBytes: Long, elapsedMs: Long): Long {
        if (deltaBytes <= 0L || elapsedMs <= 0L) return 0L
        return (deltaBytes.toDouble() * 1000.0 / elapsedMs)
            .toLong()
            .coerceAtLeast(0L)
    }

    /**
     * Get current total device-wide bytes transferred.
     * @return Pair of (rxBytes, txBytes)
     */
    fun getCurrentTotalBytes(): Pair<Long, Long> =
        TrafficStats.getTotalRxBytes() to TrafficStats.getTotalTxBytes()
}
