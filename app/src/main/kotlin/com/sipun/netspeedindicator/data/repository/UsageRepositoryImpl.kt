package com.sipun.netspeedindicator.data.repository

import com.sipun.netspeedindicator.data.datasource.UsageDataSource
import com.sipun.netspeedindicator.data.local.dao.UsageDao
import com.sipun.netspeedindicator.data.mapper.toDomain
import com.sipun.netspeedindicator.data.mapper.toEntity
import com.sipun.netspeedindicator.domain.model.UsageInfo
import com.sipun.netspeedindicator.domain.repository.UsageRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
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
        getUsageByDateInternal(today, refresh = true)
    }

    override suspend fun getUsageByDate(date: String): UsageInfo? = withContext(Dispatchers.IO) {
        val today = LocalDate.now().format(dateFormatter)
        getUsageByDateInternal(date, refresh = date == today)
    }

    private suspend fun getUsageByDateInternal(date: String, refresh: Boolean): UsageInfo? {
        if (!refresh) {
            usageDao.getByDate(date)?.let { return it.toDomain() }
        }

        return fetchAndCache(date) ?: usageDao.getByDate(date)?.toDomain()
    }

    private suspend fun fetchAndCache(date: String): UsageInfo? {
        val usage = usageDataSource.getUsageForDate(date) ?: return null
        usageDao.insertOrUpdate(usage.toEntity())
        return usage
    }

    override suspend fun getUsageByDateRange(startDate: String, endDate: String): List<UsageInfo> =
        withContext(Dispatchers.IO) {
            val cached = usageDao.getByDateRange(startDate, endDate)
                .associate { it.date to it.toDomain() }
                .toMutableMap()
            val start = LocalDate.parse(startDate, dateFormatter)
            val end = LocalDate.parse(endDate, dateFormatter)
            val today = LocalDate.now()

            var current = start
            while (!current.isAfter(end)) {
                val date = current.format(dateFormatter)
                if (current == today || !cached.containsKey(date)) {
                    fetchAndCache(date)?.let { cached[date] = it }
                }
                current = current.plusDays(1)
            }

            cached.values.sortedBy { it.date }
        }

    @OptIn(kotlinx.coroutines.ExperimentalCoroutinesApi::class)
    override fun observeTodayUsage(): Flow<UsageInfo?> =
        flow {
            while (true) {
                emit(LocalDate.now().format(dateFormatter))
                delay(60_000L)
            }
        }
            .distinctUntilChanged()
            .flatMapLatest { date -> usageDao.observeByDate(date) }
            .map { it?.toDomain() }

    override suspend fun getMonthlyUsage(yearMonth: String): List<UsageInfo> =
        withContext(Dispatchers.IO) {
            val yearMonthObj = java.time.YearMonth.parse(yearMonth)
            val today = LocalDate.now()
            val startDate = yearMonthObj.atDay(1)
            val endDate = yearMonthObj.atEndOfMonth().coerceAtMost(today)
            val cached = usageDao.getByMonth(yearMonth)
                .associate { it.date to it.toDomain() }
                .toMutableMap()

            var current = startDate
            while (!current.isAfter(endDate)) {
                val date = current.format(dateFormatter)
                if (current == today || !cached.containsKey(date)) {
                    fetchAndCache(date)?.let { cached[date] = it }
                }
                current = current.plusDays(1)
            }

            cached.values.sortedBy { it.date }
        }
}
