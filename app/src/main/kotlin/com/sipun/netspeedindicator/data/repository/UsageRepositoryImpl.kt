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
import java.time.format.DateTimeFormatter
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Implementation of UsageRepository.
 * NetworkStatsManager is the authoritative source for dates that are not yet
 * cached in Room; the current day is refreshed because it is still changing.
 */
@Singleton
class UsageRepositoryImpl @Inject constructor(
    private val usageDao: UsageDao,
    private val usageDataSource: UsageDataSource
) : UsageRepository {

    private val dateFormatter = DateTimeFormatter.ISO_LOCAL_DATE

    override suspend fun saveUsage(usage: UsageInfo) = withContext(Dispatchers.IO) {
        usageDao.insertOrUpdate(usage.toEntity())
    }

    override suspend fun getTodayUsage(): UsageInfo? = withContext(Dispatchers.IO) {
        val today = LocalDate.now().format(dateFormatter)
        usageDataSource.getUsageForDate(today)?.also { saveUsage(it) }
            ?: usageDao.getByDate(today)?.toDomain()
    }

    override suspend fun getUsageByDate(date: String): UsageInfo? = withContext(Dispatchers.IO) {
        val today = LocalDate.now().format(dateFormatter)
        if (date == today) {
            usageDataSource.getUsageForDate(date)?.also { saveUsage(it) }
                ?: usageDao.getByDate(date)?.toDomain()
        } else {
            usageDao.getByDate(date)?.toDomain()
                ?: usageDataSource.getUsageForDate(date)?.also { saveUsage(it) }
        }
    }

    override suspend fun getUsageByDateRange(startDate: String, endDate: String): List<UsageInfo> =
        withContext(Dispatchers.IO) {
            val cached = usageDao.getByDateRange(startDate, endDate)
                .associateBy { it.date }
                .toMutableMap()
            val start = LocalDate.parse(startDate, dateFormatter)
            val end = LocalDate.parse(endDate, dateFormatter)
            val today = LocalDate.now()

            var current = start
            while (!current.isAfter(end)) {
                val date = current.format(dateFormatter)
                val shouldRefresh = current == today
                if (shouldRefresh || !cached.containsKey(date)) {
                    usageDataSource.getUsageForDate(date)?.let {
                        saveUsage(it)
                        cached[date] = it.toEntity()
                    }
                }
                current = current.plusDays(1)
            }

            usageDao.getByDateRange(startDate, endDate).map { it.toDomain() }
        }

    override fun observeTodayUsage(): Flow<UsageInfo?> {
        val today = LocalDate.now().format(dateFormatter)
        return usageDao.observeByDate(today).map { it?.toDomain() }
    }

    override suspend fun getMonthlyUsage(yearMonth: String): List<UsageInfo> =
        withContext(Dispatchers.IO) {
            val yearMonthObj = java.time.YearMonth.parse(yearMonth)
            val startDate = yearMonthObj.atDay(1)
            val endDate = yearMonthObj.atEndOfMonth().coerceAtMost(LocalDate.now())
            val cached = usageDao.getByMonth(yearMonth).associateBy { it.date }.toMutableMap()

            var current = startDate
            while (!current.isAfter(endDate)) {
                val date = current.format(dateFormatter)
                if (current == LocalDate.now() || !cached.containsKey(date)) {
                    usageDataSource.getUsageForDate(date)?.let {
                        saveUsage(it)
                        cached[date] = it.toEntity()
                    }
                }
                current = current.plusDays(1)
            }

            usageDao.getByMonth(yearMonth).map { it.toDomain() }
        }
}
