package com.sipun.netspeedindicator.domain.usecase

import com.sipun.netspeedindicator.domain.model.UsageInfo
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test

class SaveUsageUseCaseTest {

    private lateinit var repository: FakeUsageRepository
    private lateinit var useCase: SaveUsageUseCase

    @Before
    fun setUp() {
        repository = FakeUsageRepository()
        useCase = SaveUsageUseCase(repository)
    }

    @Test
    fun `invoke delegates to repository saveUsage`() = runTest {
        val usage = UsageInfo(
            date = "2024-01-15",
            wifiRxBytes = 1000L,
            wifiTxBytes = 500L,
            mobileRxBytes = 2000L,
            mobileTxBytes = 750L
        )

        useCase(usage)

        assertEquals(1, repository.savedUsages.size)
        assertEquals(usage, repository.savedUsages.first())
    }

    @Test
    fun `invoke can be called multiple times`() = runTest {
        val usage1 = UsageInfo(date = "2024-01-15", wifiRxBytes = 100L)
        val usage2 = UsageInfo(date = "2024-01-16", wifiRxBytes = 200L)

        useCase(usage1)
        useCase(usage2)

        assertEquals(2, repository.savedUsages.size)
        assertEquals(usage1, repository.savedUsages[0])
        assertEquals(usage2, repository.savedUsages[1])
    }
}
