package com.sipun.netspeedindicator.ui.screens.main.home

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.sipun.netspeedindicator.MainDispatcherRule
import com.sipun.netspeedindicator.core.state.TrafficStateManager
import com.sipun.netspeedindicator.data.preferences.PreferenceManager
import com.sipun.netspeedindicator.domain.model.InstalledAppInfo
import com.sipun.netspeedindicator.domain.model.SpeedInfo
import com.sipun.netspeedindicator.domain.model.UsageInfo
import com.sipun.netspeedindicator.domain.usecase.FakeInstalledAppsProvider
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
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()
    private lateinit var trafficStateManager: TrafficStateManager
    private lateinit var usageRepository: FakeUsageRepository
    private lateinit var installedAppsProvider: FakeInstalledAppsProvider
    private lateinit var viewModel: HomeViewModel

    @Before
    fun setUp() {
        trafficStateManager = TrafficStateManager()
        usageRepository = FakeUsageRepository()
        installedAppsProvider = FakeInstalledAppsProvider()
        val context = ApplicationProvider.getApplicationContext<Context>()
        viewModel = HomeViewModel(
            context,
            trafficStateManager,
            PreferenceManager(context),
            GetDailyUsageUseCase(usageRepository),
            installedAppsProvider
        )
    }

    @Test
    fun `initial state is not running with no dialog shown`() = runTest {
        val collectJob = launch { viewModel.uiState.collect {} }
        testScheduler.runCurrent()
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
            GetDailyUsageUseCase(usageRepository),
            installedAppsProvider
        )
        val collectJob = launch { viewModel.uiState.collect {} }
        testScheduler.runCurrent()

        assertEquals(usage, viewModel.uiState.value.todayUsage)
        collectJob.cancel()
    }

    @Test
    fun `OnStopClick shows dialog when service is running`() = runTest {
        val collectJob = launch { viewModel.uiState.collect {} }
        testScheduler.runCurrent()
        trafficStateManager.setServiceRunning(true)
        testScheduler.runCurrent()
        viewModel.onEvent(HomeUiEvent.OnStopClick)
        testScheduler.runCurrent()
        assertTrue(viewModel.uiState.value.showStopDialog)
        viewModel.onEvent(HomeUiEvent.OnDismissDialog)
        testScheduler.runCurrent()
        assertFalse(viewModel.uiState.value.showStopDialog)
        collectJob.cancel()
    }

    @Test
    fun `active apps are sorted by rate, capped at four and counted in full`() = runTest {
        val collectJob = launch { viewModel.uiState.collect {} }
        installedAppsProvider.apps = mapOf(
            101 to InstalledAppInfo("com.alpha", "Alpha"),
            102 to InstalledAppInfo("com.beta", "Beta"),
            103 to InstalledAppInfo("com.gamma", "Gamma"),
            104 to InstalledAppInfo("com.delta", "Delta"),
            105 to InstalledAppInfo("com.epsilon", "Epsilon"),
            106 to InstalledAppInfo("com.zeta", "Zeta")
        )
        trafficStateManager.setServiceRunning(true)
        trafficStateManager.updateAppLiveRates(
            mapOf(
                101 to 100L,
                102 to 900L,
                103 to 0L, // idle app, must be filtered out
                104 to 400L,
                105 to 300L,
                106 to 200L
            )
        )
        testScheduler.runCurrent()

        val state = viewModel.uiState.value
        assertEquals(listOf(102, 104, 105, 106), state.activeApps.map { it.uid })
        assertEquals(listOf("Beta", "Delta", "Epsilon", "Zeta"), state.activeApps.map { it.appName })
        assertEquals(5, state.activeAppCount)
        collectJob.cancel()
    }

    @Test
    fun `active apps ignore uids that cannot be resolved to installed app info`() = runTest {
        val collectJob = launch { viewModel.uiState.collect {} }
        installedAppsProvider.apps = mapOf(1001 to InstalledAppInfo("com.known", "Known"))
        trafficStateManager.setServiceRunning(true)
        trafficStateManager.updateAppLiveRates(mapOf(1001 to 500L, 9999 to 800L))
        testScheduler.runCurrent()

        val state = viewModel.uiState.value
        assertEquals(listOf(1001), state.activeApps.map { it.uid })
        assertEquals(1, state.activeAppCount)
        collectJob.cancel()
    }

    @Test
    fun `stopping the service clears active apps`() = runTest {
        val collectJob = launch { viewModel.uiState.collect {} }
        installedAppsProvider.apps = mapOf(101 to InstalledAppInfo("com.alpha", "Alpha"))
        trafficStateManager.setServiceRunning(true)
        trafficStateManager.updateAppLiveRates(mapOf(101 to 100L))
        testScheduler.runCurrent()
        assertEquals(1, viewModel.uiState.value.activeAppCount)

        trafficStateManager.setServiceRunning(false)
        testScheduler.runCurrent()
        assertEquals(0, viewModel.uiState.value.activeAppCount)
        assertTrue(viewModel.uiState.value.activeApps.isEmpty())
        collectJob.cancel()
    }

    @Test
    fun `service running and peak speed mirror TrafficStateManager`() = runTest {
        val collectJob = launch { viewModel.uiState.collect {} }
        testScheduler.runCurrent()
        trafficStateManager.setServiceRunning(true)
        trafficStateManager.updateSpeed(
            SpeedInfo(
                downloadBytesPerSecond = 500L,
                uploadBytesPerSecond = 100L,
                totalBytesPerSecond = 600L
            )
        )
        trafficStateManager.updateSpeed(
            SpeedInfo(
                downloadBytesPerSecond = 200L,
                uploadBytesPerSecond = 50L,
                totalBytesPerSecond = 250L
            )
        )
        testScheduler.runCurrent()
        val state = viewModel.uiState.value
        assertTrue(state.isServiceRunning)
        assertEquals(600L, state.peakSpeed)
        assertEquals(250L, state.currentSpeed.totalBytesPerSecond)
        collectJob.cancel()
    }
}
