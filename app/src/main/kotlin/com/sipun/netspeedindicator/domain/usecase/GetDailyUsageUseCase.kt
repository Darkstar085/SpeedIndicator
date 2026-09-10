package com.sipun.netspeedindicator.domain.usecase

import com.sipun.netspeedindicator.domain.model.UsageInfo
import com.sipun.netspeedindicator.domain.repository.UsageRepository
import kotlinx.coroutines.flow.Flow
import java.time.LocalDate
import java.time.format.DateTimeFormatter
import javax.inject.Inject

/**
 * Use case to get daily usage data
 * Handles date formatting and retrieval logic
 */
class GetDailyUsageUseCase @Inject constructor(
    private val usageRepository: UsageRepository
) {
    /**
     * Get usage for today
     */
    suspend fun getToday(): UsageInfo? {
        return usageRepository.getTodayUsage()
    }
    
    /**
     * Observe today's usage in real-time
     */
    fun observeToday(): Flow<UsageInfo?> {
        return usageRepository.observeTodayUsage()
    }
    
    /**
     * Get usage for a specific date
     */
    suspend fun getByDate(date: String): UsageInfo? {
        return usageRepository.getUsageByDate(date)
    }
    
    /**
     * Get usage for last N days
     */
    suspend fun getLastNDays(days: Int): List<UsageInfo> {
        val endDate = LocalDate.now()
        val startDate = endDate.minusDays(days.toLong() - 1)
        
        val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
        return usageRepository.getUsageByDateRange(
            startDate.format(formatter),
            endDate.format(formatter)
        )
    }
}
