package com.sipun.netspeedindicator.domain.usecase

import com.sipun.netspeedindicator.domain.model.UsageInfo
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import java.time.LocalDate
import java.time.format.DateTimeFormatter

class GetDailyUsageUseCaseTest {

    private lateinit var repository: FakeUsageRepository
    private lateinit var useCase: GetDailyUsageUseCase

    @Before
    fun setUp() {
        repository = FakeUsageRepository()
        useCase = GetDailyUsageUseCase(repository)
    }

    @Test
    fun `getLastNDays requests an inclusive range ending today`() = runTest {
        val formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd")
        val expectedEnd = LocalDate.now().format(formatter)
        val expectedStart = LocalDate.now().minusDays(6).format(formatter)
        val expected = listOf(UsageInfo(date = expectedEnd))
        repository.dateRangeResult = expected

        val result = useCase.getLastNDays(days = 7)

        assertEquals(expectedStart to expectedEnd, repository.lastRequestedDateRange)
        assertEquals(expected, result)
    }

    @Test
    fun `getByDate delegates to the repository`() = runTest {
        assertEquals(null, useCase.getByDate("2026-01-01"))
    }
}
