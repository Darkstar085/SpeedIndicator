package com.sipun.netspeedindicator.ui.screens.main.home

import com.sipun.netspeedindicator.domain.model.LiveAppRate

/**
 * Debounces live per-app rates for the dashboard card.
 *
 * Android's per-UID accounting arrives in bursts with sub-second lag, and
 * `TrafficStateManager.appLiveRates` only emits when the map content
 * changes — so a raw `filter { rate > 0 }` strobes the list, and silent
 * idle stretches would otherwise leave the last known rows forever. Fresh
 * non-empty samples replace the list immediately; idle samples keep the
 * last known rows for [holdTicks] consecutive idle ticks (the dashboard
 * re-evaluates once per second) before clearing.
 */
class LiveAppsHolder(private val holdTicks: Int = DEFAULT_HOLD_TICKS) {

    companion object {
        const val DEFAULT_HOLD_TICKS = 5
    }

    private var current: List<LiveAppRate> = emptyList()
    private var idleTicks = 0

    /** Record one tick and return the list the card should show. */
    fun record(fresh: List<LiveAppRate>): List<LiveAppRate> {
        if (fresh.isNotEmpty()) {
            current = fresh
            idleTicks = 0
            return current
        }
        idleTicks += 1
        if (idleTicks > holdTicks) {
            current = emptyList()
        }
        return current
    }

    /** Drop the retained list immediately (monitoring stopped / service reset). */
    fun clear(): List<LiveAppRate> {
        current = emptyList()
        idleTicks = 0
        return current
    }
}
