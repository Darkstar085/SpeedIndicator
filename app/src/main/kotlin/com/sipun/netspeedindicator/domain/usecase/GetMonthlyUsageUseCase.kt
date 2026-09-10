package com.sipun.netspeedindicator.domain.usecase

import com.sipun.netspeedindicator.domain.model.UsageInfo
import com.sipun.netspeedindicator.domain.repository.UsageRepository
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import javax.inject.Inject

/**
 * Use case to get monthly usage data
 * Handles month-based queries and aggregation
 */
class GetMonthlyUsageUseCase @Inject constructor(
    private val usageRepository: UsageRepository
) {
    /**
     * Get usage for current month
     */
    suspend fun getCurrentMonth(): List<UsageInfo> {
        val yearMonth = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM"))
        return usageRepository.getMonthlyUsage(yearMonth)
    }
    
    /**
     * Get usage for a specific month
     * @param yearMonth Format: yyyy-MM (e.g., "2024-01")
     */
    suspend fun getByMonth(yearMonth: String): List<UsageInfo> {
        return usageRepository.getMonthlyUsage(yearMonth)
    }
    
    /**
     * Build a day-by-day usage calendar, zero-filling any days with no recorded usage.
     *
     * @param monthsBack For 0 or 1, returns a single month (this month / N months ago).
     * For values > 1 (e.g. 3 for "Last 3 Months"), aggregates that many consecutive months
     * ending with the current one into a single date-descending list.
     * The current month is always clamped to "up to today" (no future dates).
     */
    suspend fun getMonthCalendar(monthsBack: Int): List<UsageInfo> {
        val monthOffsets = if (monthsBack <= 1) listOf(monthsBack) else (0 until monthsBack).toList()
        val today = LocalDate.now()
        val dayFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
        val monthFormatter = DateTimeFormatter.ofPattern("yyyy-MM")

        return monthOffsets.flatMap { offset ->
            val targetMonth = YearMonth.now().minusMonths(offset.toLong())
            val dbUsageMap = usageRepository.getMonthlyUsage(targetMonth.format(monthFormatter))
                .associateBy { it.date }

            val lastDayToShow = if (offset == 0) today.dayOfMonth else targetMonth.lengthOfMonth()

            (lastDayToShow downTo 1).map { day ->
                val dateStr = targetMonth.atDay(day).format(dayFormatter)
                dbUsageMap[dateStr] ?: UsageInfo(date = dateStr)
            }
        }
    }

    /**
     * Get total usage for current month
     */
    suspend fun getCurrentMonthTotal(): UsageInfo {
        val monthlyData = getCurrentMonth()
        return aggregateUsage(monthlyData)
    }
    
    /**
     * Aggregate multiple usage records into a single total
     */
    private fun aggregateUsage(usageList: List<UsageInfo>): UsageInfo {
        if (usageList.isEmpty()) {
            val currentMonth = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM"))
            return UsageInfo(date = "$currentMonth-01")
        }
        
        return UsageInfo(
            date = usageList.first().date,
            wifiRxBytes = usageList.sumOf { it.wifiRxBytes },
            wifiTxBytes = usageList.sumOf { it.wifiTxBytes },
            mobileRxBytes = usageList.sumOf { it.mobileRxBytes },
            mobileTxBytes = usageList.sumOf { it.mobileTxBytes }
        )
    }
}
