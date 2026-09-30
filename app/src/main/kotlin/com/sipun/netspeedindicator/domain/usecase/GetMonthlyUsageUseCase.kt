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
    suspend fun getCurrentMonth(): List<UsageInfo> {
        val yearMonth = LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM"))
        return usageRepository.getMonthlyUsage(yearMonth)
    }

    suspend fun getByMonth(yearMonth: String): List<UsageInfo> {
        return usageRepository.getMonthlyUsage(yearMonth)
    }

    /**
     * Build a day-by-day usage calendar for an exact date range.
     * Missing days are zero-filled and the result is newest first.
     */
    suspend fun getDateRangeCalendar(startDate: LocalDate, endDate: LocalDate): List<UsageInfo> {
        require(!endDate.isBefore(startDate)) { "End date must not be before start date" }

        val dayFormatter = DateTimeFormatter.ISO_LOCAL_DATE
        val monthFormatter = DateTimeFormatter.ofPattern("yyyy-MM")
        val usageByDate = mutableMapOf<String, UsageInfo>()

        var month = YearMonth.from(startDate)
        val lastMonth = YearMonth.from(endDate)
        while (!month.isAfter(lastMonth)) {
            usageRepository.getMonthlyUsage(month.format(monthFormatter))
                .forEach { usageByDate[it.date] = it }
            month = month.plusMonths(1)
        }

        return generateSequence(endDate) { date ->
            if (date.isAfter(startDate)) date.minusDays(1) else null
        }.map { date ->
            val dateString = date.format(dayFormatter)
            usageByDate[dateString] ?: UsageInfo(date = dateString)
        }.toList()
    }

    suspend fun getMonthCalendar(monthsBack: Int): List<UsageInfo> {
        val monthOffsets = if (monthsBack <= 1) listOf(monthsBack) else (0 until monthsBack).toList()
        val today = LocalDate.now()
        val dayFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
        val monthFormatter = DateTimeFormatter.ofPattern("yyyy-MM")

        return monthOffsets.flatMap { offset ->
            val targetMonth = YearMonth.now().minusMonths(offset.toLong())
            val dbUsageMap = usageRepository.getMonthlyUsage(targetMonth.format(monthFormatter))
                .associateBy { it.date }

            val lastDayToShow = targetMonth.lengthOfMonth()

            (lastDayToShow downTo 1).map { day ->
                val dateStr = targetMonth.atDay(day).format(dayFormatter)
                if (offset == 0 && targetMonth.atDay(day).isAfter(today)) {
                    UsageInfo(date = dateStr)
                } else {
                    dbUsageMap[dateStr] ?: UsageInfo(date = dateStr)
                }
            }
        }
    }

    suspend fun getCurrentMonthTotal(): UsageInfo {
        val monthlyData = getCurrentMonth()
        return aggregateUsage(monthlyData)
    }

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
