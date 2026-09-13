package com.sipun.netspeedindicator.domain.usecase

import com.sipun.netspeedindicator.MainDispatcherRule
import com.sipun.netspeedindicator.domain.model.SpeedInfo
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Rule
import org.junit.Test

class GetCurrentSpeedUseCaseTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    private lateinit var repository: FakeSpeedRepository
    private lateinit var useCase: GetCurrentSpeedUseCase

    @Before
    fun setUp() {
        repository = FakeSpeedRepository()
        useCase = GetCurrentSpeedUseCase(repository)
    }

    @Test
    fun `invoke delegates to repository observeSpeed`() = runTest {
        val expected = SpeedInfo(
            downloadBytesPerSecond = 1024L,
            uploadBytesPerSecond = 512L,
            totalBytesPerSecond = 1536L
        )
        repository.observeSpeedResult = kotlinx.coroutines.flow.flowOf(expected)

        val result = useCase().first()

        assertEquals(expected, result)
    }

    @Test
    fun `invoke returns default SpeedInfo when repository emits default`() = runTest {
        val result = useCase().first()

        assertEquals(0L, result.downloadBytesPerSecond)
        assertEquals(0L, result.uploadBytesPerSecond)
        assertEquals(0L, result.totalBytesPerSecond)
    }
}
