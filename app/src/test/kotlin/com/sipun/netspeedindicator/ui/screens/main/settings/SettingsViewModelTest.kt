package com.sipun.netspeedindicator.ui.screens.main.settings

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.sipun.netspeedindicator.MainDispatcherRule
import com.sipun.netspeedindicator.data.preferences.PreferenceManager
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
class SettingsViewModelTest {
    @get:Rule val mainDispatcherRule = MainDispatcherRule()
    private lateinit var preferenceManager: PreferenceManager
    private lateinit var viewModel: SettingsViewModel
    @Before fun setUp() { val context = ApplicationProvider.getApplicationContext<Context>(); preferenceManager = PreferenceManager(context); viewModel = SettingsViewModel(context, preferenceManager) }
    @Test fun `initial state matches PreferenceManager defaults`() = runTest { val collectJob = launch { viewModel.uiState.collect {} }; val state = viewModel.uiState.value; assertEquals(0, state.appTheme); assertTrue(state.dynamicColor); assertTrue(state.lockScreenNotification); assertFalse(state.showUploadSpeed); assertFalse(state.hasUsagePermission); assertFalse(state.isAutoStartAvailable); collectJob.cancel() }
    @Test fun `OnThemeCycle advances through System, Light, Dark and wraps around`() = runTest { val collectJob = launch { viewModel.uiState.collect {} }; viewModel.onEvent(SettingsUiEvent.OnThemeCycle); assertEquals(1, viewModel.uiState.value.appTheme); viewModel.onEvent(SettingsUiEvent.OnThemeCycle); assertEquals(2, viewModel.uiState.value.appTheme); viewModel.onEvent(SettingsUiEvent.OnThemeCycle); assertEquals(0, viewModel.uiState.value.appTheme); collectJob.cancel() }
    @Test fun `toggle events persist through PreferenceManager and flow back into state`() = runTest { val collectJob = launch { viewModel.uiState.collect {} }; viewModel.onEvent(SettingsUiEvent.OnDynamicColorChanged(false)); assertFalse(viewModel.uiState.value.dynamicColor); viewModel.onEvent(SettingsUiEvent.OnLockScreenNotificationChanged(false)); assertFalse(viewModel.uiState.value.lockScreenNotification); viewModel.onEvent(SettingsUiEvent.OnNotificationBarChanged(true)); assertTrue(viewModel.uiState.value.showUploadSpeed); collectJob.cancel() }
}
