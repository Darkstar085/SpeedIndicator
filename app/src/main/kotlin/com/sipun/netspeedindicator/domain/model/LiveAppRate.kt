package com.sipun.netspeedindicator.domain.model

/**
 * A launcher-visible app using the network right now, with its live total
 * throughput in bytes per second.
 */
data class LiveAppRate(
    val uid: Int,
    val packageName: String,
    val appName: String,
    val bytesPerSecond: Long
)
