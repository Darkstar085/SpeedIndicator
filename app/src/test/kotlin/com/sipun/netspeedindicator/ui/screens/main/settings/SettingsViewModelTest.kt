package com.sipun.netspeedindicator.ui.screens.main.settings

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.sipun.netspeedindicator.MainDispatcherRule
import com.sipun.netspeedindicator.data.preferences.PreferenceManager
import kotlinx.coroutines.launch
import kotlinx.coroutines.flow.first
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
    @Test fun `OnThemeSelected persists standard and pure black themes`() = runTest { val collectJob = launch { viewModel.uiState.collect {} }; viewModel.onEvent(SettingsUiEvent.OnThemeSelected(1)); assertEquals(1, viewModel.uiState.first { it.appTheme == 1 }.appTheme); viewModel.onEvent(SettingsUiEvent.OnThemeSelected(3)); val pureBlack = viewModel.uiState.first { it.pureBlackTheme && it.appTheme == 2 }; assertEquals(2, pureBlack.appTheme); viewModel.onEvent(SettingsUiEvent.OnThemeSelected(0)); val systemTheme = viewModel.uiState.first { it.appTheme == 0 && !it.pureBlackTheme }; assertEquals(0, systemTheme.appTheme); collectJob.cancel() }
    @Test fun `toggle events persist through PreferenceManager and flow back into state`() = runTest { val collectJob = launch { viewModel.uiState.collect {} }; viewModel.onEvent(SettingsUiEvent.OnDynamicColorChanged(false)); assertFalse(viewModel.uiState.first { !it.dynamicColor }.dynamicColor); viewModel.onEvent(SettingsUiEvent.OnLockScreenNotificationChanged(false)); assertFalse(viewModel.uiState.first { !it.lockScreenNotification }.lockScreenNotification); viewModel.onEvent(SettingsUiEvent.OnNotificationBarChanged(true)); assertTrue(viewModel.uiState.first { it.showUploadSpeed }.showUploadSpeed); collectJob.cancel() }
}
