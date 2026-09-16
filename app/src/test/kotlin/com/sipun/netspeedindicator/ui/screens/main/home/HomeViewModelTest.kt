package com.sipun.netspeedindicator.ui.screens.main.home

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.sipun.netspeedindicator.MainDispatcherRule
import com.sipun.netspeedindicator.data.manager.TrafficStateManager
import com.sipun.netspeedindicator.domain.model.SpeedInfo
import com.sipun.netspeedindicator.domain.usecase.FakeUsageRepository
import com.sipun.netspeedindicator.domain.usecase.GetDailyUsageUseCase
import kotlinx.coroutines.launch
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
    private lateinit var viewModel: HomeViewModel
    @Before fun setUp() {
        trafficStateManager = TrafficStateManager()
        val context = ApplicationProvider.getApplicationContext<Context>()
        viewModel = HomeViewModel(context, trafficStateManager, createPreferences(context), GetDailyUsageUseCase(FakeUsageRepository()))
    }
    @Test fun `initial state is not running with no dialog shown`() = runTest { val collectJob = launch { viewModel.uiState.collect {} }; val state = viewModel.uiState.value; assertFalse(state.isServiceRunning); assertFalse(state.showStopDialog); assertFalse(state.shouldFinishActivity); collectJob.cancel() }
    @Test fun `OnStopClick shows the dialog and OnDismissDialog hides it`() = runTest { val collectJob = launch { viewModel.uiState.collect {} }; viewModel.onEvent(HomeUiEvent.OnStopClick); assertTrue(viewModel.uiState.value.showStopDialog); viewModel.onEvent(HomeUiEvent.OnDismissDialog); assertFalse(viewModel.uiState.value.showStopDialog); collectJob.cancel() }
    @Test fun `OnConfirmStop hides the dialog and requests an activity finish`() = runTest { val collectJob = launch { viewModel.uiState.collect {} }; viewModel.onEvent(HomeUiEvent.OnStopClick); viewModel.onEvent(HomeUiEvent.OnConfirmStop); val state = viewModel.uiState.value; assertFalse(state.showStopDialog); assertTrue(state.shouldFinishActivity); collectJob.cancel() }
    @Test fun `OnActivityFinished clears the finish flag`() = runTest { val collectJob = launch { viewModel.uiState.collect {} }; viewModel.onEvent(HomeUiEvent.OnConfirmStop); viewModel.onEvent(HomeUiEvent.OnActivityFinished); assertFalse(viewModel.uiState.value.shouldFinishActivity); collectJob.cancel() }
    @Test fun `service running and peak speed mirror TrafficStateManager`() = runTest { val collectJob = launch { viewModel.uiState.collect {} }; trafficStateManager.setServiceRunning(true); trafficStateManager.updateSpeed(SpeedInfo(downloadBytesPerSecond = 500L, uploadBytesPerSecond = 100L, totalBytesPerSecond = 600L)); trafficStateManager.updateSpeed(SpeedInfo(downloadBytesPerSecond = 200L, uploadBytesPerSecond = 50L, totalBytesPerSecond = 250L)); val state = viewModel.uiState.value; assertTrue(state.isServiceRunning); assertEquals(600L, state.peakSpeed); assertEquals(250L, state.currentSpeed.totalBytesPerSecond); collectJob.cancel() }

    private fun createPreferences(context: Context): com.sipun.netspeedindicator.data.preferences.PreferenceManager =
        com.sipun.netspeedindicator.data.preferences.PreferenceManager(context)
}
