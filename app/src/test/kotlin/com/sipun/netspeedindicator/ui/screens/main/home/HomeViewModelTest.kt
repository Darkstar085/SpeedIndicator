package com.sipun.netspeedindicator.ui.screens.main.home

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.sipun.netspeedindicator.MainDispatcherRule
import com.sipun.netspeedindicator.data.manager.TrafficStateManager
import com.sipun.netspeedindicator.data.preferences.PreferenceManager
import com.sipun.netspeedindicator.domain.model.SpeedInfo
import com.sipun.netspeedindicator.domain.model.UsageInfo
import com.sipun.netspeedindicator.domain.usecase.FakeUsageRepository
import com.sipun.netspeedindicator.domain.usecase.GetDailyUsageUseCase
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class HomeViewModelTest {
    @get:Rule val mainDispatcherRule = MainDispatcherRule()
    private lateinit var trafficStateManager: TrafficStateManager
    private lateinit var usageRepository: FakeUsageRepository
    private lateinit var viewModel: HomeViewModel

    @Before
    fun setUp() {
        trafficStateManager = TrafficStateManager()
        usageRepository = FakeUsageRepository()
        val context = ApplicationProvider.getApplicationContext<Context>()
        viewModel = HomeViewModel(
            context,
            trafficStateManager,
            PreferenceManager(context),
            GetDailyUsageUseCase(usageRepository)
        )
    }

    @Test
    fun `initial state is not running with no dialog shown`() = runTest {
        val collectJob = launch { viewModel.uiState.collect {} }
        advanceUntilIdle()
        val state = viewModel.uiState.value
        assertFalse(state.isServiceRunning)
        assertFalse(state.showStopDialog)
        collectJob.cancel()
    }

    @Test
    fun `today usage is restored from repository`() = runTest {
        val usage = UsageInfo(date = "2026-09-16", wifiRxBytes = 1234L)
        usageRepository.todayUsage = usage
        val context = ApplicationProvider.getApplicationContext<Context>()
        viewModel = HomeViewModel(
            context,
            trafficStateManager,
            PreferenceManager(context),
            GetDailyUsageUseCase(usageRepository)
        )

        advanceUntilIdle()

        assertEquals(usage, viewModel.uiState.value.todayUsage)
    }

    @Test
    fun `OnStopClick shows dialog when service is running`() = runTest {
        val collectJob = launch { viewModel.uiState.collect {} }
        trafficStateManager.setServiceRunning(true)
        viewModel.onEvent(HomeUiEvent.OnStopClick)
        assertTrue(viewModel.uiState.value.showStopDialog)
        viewModel.onEvent(HomeUiEvent.OnDismissDialog)
        assertFalse(viewModel.uiState.value.showStopDialog)
        collectJob.cancel()
    }

    @Test
    fun `service running and peak speed mirror TrafficStateManager`() = runTest {
        val collectJob = launch { viewModel.uiState.collect {} }
        trafficStateManager.setServiceRunning(true)
        trafficStateManager.updateSpeed(SpeedInfo(downloadBytesPerSecond = 500L, uploadBytesPerSecond = 100L, totalBytesPerSecond = 600L))
        trafficStateManager.updateSpeed(SpeedInfo(downloadBytesPerSecond = 200L, uploadBytesPerSecond = 50L, totalBytesPerSecond = 250L))
        val state = viewModel.uiState.value
        assertTrue(state.isServiceRunning)
        assertEquals(600L, state.peakSpeed)
        assertEquals(250L, state.currentSpeed.totalBytesPerSecond)
        collectJob.cancel()
    }
}
