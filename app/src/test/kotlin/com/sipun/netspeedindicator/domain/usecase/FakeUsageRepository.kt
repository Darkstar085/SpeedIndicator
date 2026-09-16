package com.sipun.netspeedindicator.domain.usecase

import com.sipun.netspeedindicator.domain.model.UsageInfo
import com.sipun.netspeedindicator.domain.repository.UsageRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flowOf

/** Hand-written test double so use-case tests don't need a mocking library. */
class FakeUsageRepository : UsageRepository {

    val savedUsages = mutableListOf<UsageInfo>()
    var todayUsage: UsageInfo? = null
    var monthlyUsageByMonth: Map<String, List<UsageInfo>> = emptyMap()
    var dateRangeResult: List<UsageInfo> = emptyList()
    var lastRequestedDateRange: Pair<String, String>? = null

    override suspend fun saveUsage(usage: UsageInfo) {
        savedUsages.add(usage)
    }

    override suspend fun getTodayUsage(): UsageInfo? = todayUsage

    override suspend fun getUsageByDate(date: String): UsageInfo? = null

    override suspend fun getUsageByDateRange(startDate: String, endDate: String): List<UsageInfo> {
        lastRequestedDateRange = startDate to endDate
        return dateRangeResult
    }

    override fun observeTodayUsage(): Flow<UsageInfo?> = flowOf(null)

    override suspend fun getMonthlyUsage(yearMonth: String): List<UsageInfo> =
        monthlyUsageByMonth[yearMonth] ?: emptyList()
}
