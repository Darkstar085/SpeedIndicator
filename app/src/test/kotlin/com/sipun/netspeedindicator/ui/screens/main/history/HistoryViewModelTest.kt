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
import org.junit.Rule
import org.junit.Test
import org.junit.Before
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
    fun `loads exactly the previous 6 days plus today`() = runTest {
        val collectJob = launch { viewModel.uiState.collect {} }
        val state = viewModel.uiState.value
        assertEquals(HistoryRange.SEVEN_DAYS, state.selectedRange)
        assertFalse(state.isLoading)
        assertEquals(7, state.dailyUsage.size)
        assertEquals(LocalDate.now(), LocalDate.parse(state.dailyUsage.first().date))
        assertEquals(LocalDate.now().minusDays(6), LocalDate.parse(state.dailyUsage.last().date))
        collectJob.cancel()
    }

    @Test
    fun `loads every day of this month`() = runTest {
        val collectJob = launch { viewModel.uiState.collect {} }
        viewModel.onEvent(HistoryUiEvent.OnSelectRange(HistoryRange.THIS_MONTH))
        val state = viewModel.uiState.value
        assertEquals(HistoryRange.THIS_MONTH, state.selectedRange)
        assertEquals(YearMonth.now().lengthOfMonth(), state.dailyUsage.size)
        assertEquals(YearMonth.now().atDay(1), LocalDate.parse(state.dailyUsage.last().date))
        assertEquals(YearMonth.now().atEndOfMonth(), LocalDate.parse(state.dailyUsage.first().date))
        collectJob.cancel()
    }

    @Test
    fun `loads all days across the selected three calendar months`() = runTest {
        val collectJob = launch { viewModel.uiState.collect {} }
        viewModel.onEvent(HistoryUiEvent.OnSelectRange(HistoryRange.THREE_MONTHS))
        val state = viewModel.uiState.value
        val expectedDays = YearMonth.now().lengthOfMonth() +
            YearMonth.now().minusMonths(1).lengthOfMonth() +
            YearMonth.now().minusMonths(2).lengthOfMonth()
        assertEquals(expectedDays, state.dailyUsage.size)
        collectJob.cancel()
    }

    @Test
    fun `live usage overlays today's entry`() = runTest {
        val collectJob = launch { viewModel.uiState.collect {} }
        val todayStr = LocalDate.now().format(DateTimeFormatter.ISO_LOCAL_DATE)
        trafficStateManager.updateDailyUsage(UsageInfo(date = todayStr, wifiRxBytes = 12345L))
        val today = viewModel.uiState.value.dailyUsage.first { it.date == todayStr }
        assertEquals(12345L, today.wifiRxBytes)
        collectJob.cancel()
    }

    @Test
    fun `empty live usage does not add a blank history row`() = runTest {
        val collectJob = launch { viewModel.uiState.collect {} }
        assertFalse(viewModel.uiState.value.dailyUsage.any { it.date.isBlank() })
        collectJob.cancel()
    }
}
