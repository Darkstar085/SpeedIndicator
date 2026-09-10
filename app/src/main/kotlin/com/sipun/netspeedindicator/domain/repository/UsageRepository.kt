package com.sipun.netspeedindicator.domain.repository

import com.sipun.netspeedindicator.domain.model.UsageInfo
import kotlinx.coroutines.flow.Flow

interface UsageRepository {
    suspend fun saveUsage(usage: UsageInfo)
    suspend fun getTodayUsage(): UsageInfo?
    suspend fun getUsageByDate(date: String): UsageInfo?
    suspend fun getUsageByDateRange(startDate: String, endDate: String): List<UsageInfo>
    fun observeTodayUsage(): Flow<UsageInfo?>
    suspend fun getMonthlyUsage(yearMonth: String): List<UsageInfo>
}
