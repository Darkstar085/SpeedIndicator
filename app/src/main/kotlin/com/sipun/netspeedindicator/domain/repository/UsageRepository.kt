package com.sipun.netspeedindicator.domain.repository

import com.sipun.netspeedindicator.domain.model.UsageInfo
import kotlinx.coroutines.flow.Flow

/**
 * Repository interface for usage data operations
 * Domain layer - defines contract without implementation details
 */
interface UsageRepository {
    /**
     * Save or update usage data for a specific date
     */
    suspend fun saveUsage(usage: UsageInfo)
    
    /**
     * Get usage data for today
     */
    suspend fun getTodayUsage(): UsageInfo?
    
    /**
     * Get usage data for a specific date
     */
    suspend fun getUsageByDate(date: String): UsageInfo?
    
    /**
     * Get usage data for a date range
     */
    suspend fun getUsageByDateRange(startDate: String, endDate: String): List<UsageInfo>
    
    /**
     * Observe today's usage in real-time
     */
    fun observeTodayUsage(): Flow<UsageInfo?>
    
    /**
     * Get current month's total usage
     */
    suspend fun getMonthlyUsage(yearMonth: String): List<UsageInfo>
}
