package com.sipun.netspeedindicator.domain.usecase

import com.sipun.netspeedindicator.domain.model.UsageInfo
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter

class GetMonthlyUsageUseCaseTest {

    private lateinit var repository: FakeUsageRepository
    private lateinit var useCase: GetMonthlyUsageUseCase

    private val dayFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
    private val monthFormatter = DateTimeFormatter.ofPattern("yyyy-MM")

    @Before
    fun setUp() {
        repository = FakeUsageRepository()
        useCase = GetMonthlyUsageUseCase(repository)
    }

    @Test
    fun `getMonthCalendar for current month zero-fills up to today only`() = runTest {
        val calendar = useCase.getMonthCalendar(monthsBack = 0)

        assertEquals(LocalDate.now().dayOfMonth, calendar.size)
        assertEquals(LocalDate.now().format(dayFormatter), calendar.first().date)
        assertTrue(calendar.all { it.totalBytes == 0L })
    }

    @Test
    fun `getMonthCalendar fills known usage and zero-fills the rest of a past month`() = runTest {
        val targetMonth = YearMonth.now().minusMonths(1)
        val knownDate = targetMonth.atDay(1).format(dayFormatter)
        repository.monthlyUsageByMonth = mapOf(
            targetMonth.format(monthFormatter) to listOf(
                UsageInfo(date = knownDate, wifiRxBytes = 1000L)
            )
        )

        val calendar = useCase.getMonthCalendar(monthsBack = 1)

        assertEquals(targetMonth.lengthOfMonth(), calendar.size)
        assertEquals(1000L, calendar.first { it.date == knownDate }.totalBytes)
        assertTrue(calendar.filter { it.date != knownDate }.all { it.totalBytes == 0L })
    }

    @Test
    fun `getMonthCalendar for 3 months returns a single date-descending list`() = runTest {
        val calendar = useCase.getMonthCalendar(monthsBack = 3)

        val expectedSize = LocalDate.now().dayOfMonth +
            YearMonth.now().minusMonths(1).lengthOfMonth() +
            YearMonth.now().minusMonths(2).lengthOfMonth()
        assertEquals(expectedSize, calendar.size)

        val dates = calendar.map { LocalDate.parse(it.date) }
        for (i in 0 until dates.lastIndex) {
            assertTrue(
                "Expected ${dates[i]} to be after ${dates[i + 1]}",
                dates[i].isAfter(dates[i + 1])
            )
        }
    }

    @Test
    fun `getCurrentMonthTotal aggregates all records for the month`() = runTest {
        val currentMonth = YearMonth.now().format(monthFormatter)
        repository.monthlyUsageByMonth = mapOf(
            currentMonth to listOf(
                UsageInfo(date = "$currentMonth-01", wifiRxBytes = 100L, mobileTxBytes = 50L),
                UsageInfo(date = "$currentMonth-02", wifiRxBytes = 200L, mobileTxBytes = 25L)
            )
        )

        val total = useCase.getCurrentMonthTotal()

        assertEquals(300L, total.wifiRxBytes)
        assertEquals(75L, total.mobileTxBytes)
    }

    @Test
    fun `getCurrentMonthTotal returns zeroed usage when there are no records`() = runTest {
        val total = useCase.getCurrentMonthTotal()

        assertEquals(0L, total.totalBytes)
    }
}
