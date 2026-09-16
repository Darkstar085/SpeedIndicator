package com.sipun.netspeedindicator.ui.screens.main.history

import com.sipun.netspeedindicator.MainDispatcherRule
import com.sipun.netspeedindicator.data.manager.TrafficStateManager
import com.sipun.netspeedindicator.domain.model.UsageInfo
import com.sipun.netspeedindicator.domain.usecase.FakeUsageRepository
import com.sipun.netspeedindicator.domain.usecase.GetMonthlyUsageUseCase
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import java.time.LocalDate
import java.time.YearMonth
import java.time.format.DateTimeFormatter

@RunWith(RobolectricTestRunner::class)
class HistoryViewModelTest {
    @get:Rule val mainDispatcherRule = MainDispatcherRule()
    private lateinit var repository: FakeUsageRepository
    private lateinit var trafficStateManager: TrafficStateManager
    private lateinit var viewModel: HistoryViewModel

    @Before
    fun setUp() {
        repository = FakeUsageRepository()
        trafficStateManager = TrafficStateManager()
        viewModel = HistoryViewModel(GetMonthlyUsageUseCase(repository), trafficStateManager)
    }

    @Test
    fun `loads the current month on init`() = runTest {
        val collectJob = launch { viewModel.uiState.collect {} }
        val state = viewModel.uiState.value
        assertEquals(0, state.selectedMonthIndex)
        assertFalse(state.isLoading)
        assertEquals(LocalDate.now().dayOfMonth, state.dailyUsage.size)
        collectJob.cancel()
    }

    @Test
    fun `OnSelectMonth switches to the requested month`() = runTest {
        val collectJob = launch { viewModel.uiState.collect {} }
        viewModel.onEvent(HistoryUiEvent.OnSelectMonth(1))
        val state = viewModel.uiState.value
        assertEquals(1, state.selectedMonthIndex)
        assertEquals(YearMonth.now().minusMonths(1).lengthOfMonth(), state.dailyUsage.size)
        collectJob.cancel()
    }

    @Test
    fun `live usage overlays today's entry while viewing the current month`() = runTest {
        val collectJob = launch { viewModel.uiState.collect {} }
        val todayStr = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE)
        trafficStateManager.updateDailyUsage(UsageInfo(date = todayStr, wifiRxBytes = 12345L))
        val today = viewModel.uiState.value.dailyUsage.first { it.date == todayStr }
        assertEquals(12345L, today.wifiRxBytes)
        collectJob.cancel()
    }

    @Test
    fun `live usage keeps history sorted newest first`() = runTest {
        val collectJob = launch { viewModel.uiState.collect {} }
        val today = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE)
        trafficStateManager.updateDailyUsage(UsageInfo(date = today, wifiRxBytes = 12345L))
        assertEquals(today, viewModel.uiState.value.dailyUsage.first().date)
        collectJob.cancel()
    }

    @Test
    fun `empty live usage does not add a blank history row`() = runTest {
        val collectJob = launch { viewModel.uiState.collect {} }
        assertFalse(viewModel.uiState.value.dailyUsage.any { it.date.isBlank() })
        collectJob.cancel()
    }

    @Test
    fun `live usage does not overlay when viewing a different month`() = runTest {
        val collectJob = launch { viewModel.uiState.collect {} }
        viewModel.onEvent(HistoryUiEvent.OnSelectMonth(1))
        val todayStr = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE)
        trafficStateManager.updateDailyUsage(UsageInfo(date = todayStr, wifiRxBytes = 999L))
        assertFalse(viewModel.uiState.value.dailyUsage.any { it.date == todayStr })
        collectJob.cancel()
    }
}
