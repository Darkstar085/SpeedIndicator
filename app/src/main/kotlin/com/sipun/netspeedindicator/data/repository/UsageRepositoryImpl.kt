package com.sipun.netspeedindicator.data.repository

import com.sipun.netspeedindicator.data.datasource.UsageDataSource
import com.sipun.netspeedindicator.data.local.dao.UsageDao
import com.sipun.netspeedindicator.data.mapper.toDomain
import com.sipun.netspeedindicator.data.mapper.toEntity
import com.sipun.netspeedindicator.domain.model.UsageInfo
import com.sipun.netspeedindicator.domain.repository.UsageRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class UsageRepositoryImpl @Inject constructor(
    private val usageDao: UsageDao,
    private val usageDataSource: UsageDataSource
) : UsageRepository {
    private val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")

    override suspend fun saveUsage(usage: UsageInfo) = withContext(Dispatchers.IO) {
        usageDao.insertOrUpdate(usage.toEntity())
    }

    override suspend fun getTodayUsage() = getUsageByDate(LocalDate.now().format(formatter))

    override suspend fun getUsageByDate(date: String): UsageInfo? = withContext(Dispatchers.IO) {
        usageDataSource.getUsageForDate(date)?.also { saveUsage(it) }
            ?: usageDao.getByDate(date)?.toDomain()
    }

    override suspend fun getUsageByDateRange(startDate: String, endDate: String): List<UsageInfo> =
        withContext(Dispatchers.IO) {
            var current = LocalDate.parse(startDate, formatter)
            val end = LocalDate.parse(endDate, formatter)
            while (!current.isAfter(end)) {
                usageDataSource.getUsageForDate(current.format(formatter))?.let { saveUsage(it) }
                current = current.plusDays(1)
            }
            usageDao.getByDateRange(startDate, endDate).map { it.toDomain() }
        }

    override fun observeTodayUsage(): Flow<UsageInfo?> = usageDao
        .observeByDate(LocalDate.now().format(formatter)).map { it?.toDomain() }

    override suspend fun getMonthlyUsage(yearMonth: String): List<UsageInfo> = withContext(Dispatchers.IO) {
        val month = YearMonth.parse(yearMonth)
        var current = month.atDay(1)
        val end = month.atEndOfMonth().coerceAtMost(LocalDate.now())
        while (!current.isAfter(end)) {
            usageDataSource.getUsageForDate(current.format(formatter))?.let { saveUsage(it) }
            current = current.plusDays(1)
        }
        usageDao.getByMonth(yearMonth).map { it.toDomain() }
    }
}
