package com.sipun.netspeedindicator.domain.model

/** Which live-rates strategy the poller currently uses on this device. */
enum class LiveRatesStrategy {
    /** Same qtaguid counters as the device total; may miss apps entirely. */
    UID_COUNTERS,

    /** NetworkStatsManager cumulative counters (requires Usage Access). */
    NETWORK_STATS,

    /** TrafficStats per-UID counters for launcher-visible apps only. */
    LAUNCHER_ONLY
}

/** On-device snapshot of the live-rates pipeline for diagnostics. */
data class LiveRatesDiagnostics(
    val strategy: LiveRatesStrategy,
    val usageAccess: Boolean,
    val uidCountersReadable: Int,
    val uidCountersUnsupported: Int
)
