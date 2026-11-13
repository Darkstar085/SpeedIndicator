package com.sipun.netspeedindicator.ui.screens.main.home

import com.sipun.netspeedindicator.domain.model.LiveAppRate

/**
 * Debounces live per-app rates for the dashboard card.
 *
 * Android's per-UID accounting arrives in bursts with sub-second lag, so a
 * raw `filter { rate > 0 }` makes the list strobe between empty and full
 * every poll. Fresh non-empty samples replace the list immediately; an
 * idle stretch keeps the last known list for [holdPolls] consecutive
 * empty polls before clearing to "nothing active".
 */
class LiveAppsHolder(private val holdPolls: Int = DEFAULT_HOLD_POLLS) {

    companion object {
        const val DEFAULT_HOLD_POLLS = 4
    }

    private var current: List<LiveAppRate> = emptyList()
    private var emptyPolls = 0

    /** Record one sample and return the list the card should show. */
    fun record(fresh: List<LiveAppRate>): List<LiveAppRate> {
        if (fresh.isNotEmpty()) {
            current = fresh
            emptyPolls = 0
            return current
        }
        emptyPolls += 1
        if (emptyPolls > holdPolls) {
            current = emptyList()
        }
        return current
    }

    /** Drop the retained list immediately (monitoring stopped / service reset). */
    fun clear(): List<LiveAppRate> {
        current = emptyList()
        emptyPolls = 0
        return current
    }
}
