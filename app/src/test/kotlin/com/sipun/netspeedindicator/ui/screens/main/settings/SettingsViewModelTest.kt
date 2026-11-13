package com.sipun.netspeedindicator.ui.screens.main.settings

import android.app.AppOpsManager
import android.content.Context
import android.os.Process
import androidx.datastore.preferences.core.edit
import androidx.test.core.app.ApplicationProvider
import com.sipun.netspeedindicator.MainDispatcherRule
import com.sipun.netspeedindicator.data.preferences.AccentGradient
import com.sipun.netspeedindicator.data.preferences.AccentStyle
import com.sipun.netspeedindicator.data.preferences.PreferenceManager
import com.sipun.netspeedindicator.data.preferences.appPreferencesDataStore
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class SettingsViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()
    private lateinit var preferenceManager: PreferenceManager
    private lateinit var viewModel: SettingsViewModel

    @Before
    fun setUp() = runTest {
        val context = ApplicationProvider.getApplicationContext<Context>()
        context.appPreferencesDataStore.edit { it.clear() }
        // Deterministic usage-stats state: Robolectric's AppOps shadow allows the op
        // by default, which would flip the initial hasUsagePermission assertion.
        val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as AppOpsManager
        Shadows.shadowOf(appOps).setMode(
            AppOpsManager.OPSTR_GET_USAGE_STATS,
            Process.myUid(),
            context.packageName,
            AppOpsManager.MODE_IGNORED
        )
        preferenceManager = PreferenceManager(context).apply {
            ioDispatcher = UnconfinedTestDispatcher()
        }
        viewModel = SettingsViewModel(context, preferenceManager)
    }

    @Test
    fun `initial state matches PreferenceManager defaults`() = runTest {
        val collectJob = launch { viewModel.uiState.collect {} }
        testScheduler.runCurrent()
        val state = viewModel.uiState.value
        assertEquals(0, state.appTheme)
        assertFalse(state.dynamicColor)
        assertTrue(state.lockScreenNotification)
        assertFalse(state.showUploadSpeed)
        assertEquals(0xFF42A5F5.toInt(), state.customColor)
        assertEquals(AccentStyle.SOLID, state.accentStyle)
        assertEquals(AccentGradient.SUNSET, state.accentGradient)
        assertEquals(0xFFE92DE2.toInt(), state.customGradientStart)
        assertEquals(0xFF4A00E0.toInt(), state.customGradientEnd)
        assertFalse(state.hasUsagePermission)
        assertFalse(state.isAutoStartAvailable)
        collectJob.cancel()
    }

    @Test
    fun `OnThemeSelected persists standard and pure black themes`() = runTest {
        val collectJob = launch { viewModel.uiState.collect {} }
        testScheduler.runCurrent()
        viewModel.onEvent(SettingsUiEvent.OnThemeSelected(1))
        testScheduler.runCurrent()
        assertEquals(1, viewModel.uiState.first { it.appTheme == 1 }.appTheme)

        viewModel.onEvent(SettingsUiEvent.OnThemeSelected(3))
        testScheduler.runCurrent()
        val pureBlack = viewModel.uiState.first { it.pureBlackTheme && it.appTheme == 2 }
        assertEquals(2, pureBlack.appTheme)

        viewModel.onEvent(SettingsUiEvent.OnThemeSelected(0))
        testScheduler.runCurrent()
        val systemTheme = viewModel.uiState.first { it.appTheme == 0 && !it.pureBlackTheme }
        assertEquals(0, systemTheme.appTheme)
        collectJob.cancel()
    }

    @Test
    fun `toggle events persist through PreferenceManager and flow back into state`() = runTest {
        val collectJob = launch { viewModel.uiState.collect {} }
        testScheduler.runCurrent()

        viewModel.onEvent(SettingsUiEvent.OnCustomColorChanged(0xFFE92B2B.toInt()))
        testScheduler.runCurrent()
        assertEquals(0xFFE92B2B.toInt(), viewModel.uiState.first { it.customColor == 0xFFE92B2B.toInt() }.customColor)

        viewModel.onEvent(SettingsUiEvent.OnAccentStyleChanged(AccentStyle.GRADIENT))
        viewModel.onEvent(SettingsUiEvent.OnAccentGradientChanged(AccentGradient.OCEAN))
        viewModel.onEvent(SettingsUiEvent.OnCustomGradientChanged(0xFF112233.toInt(), 0xFF445566.toInt()))
        testScheduler.runCurrent()
        val selectedGradient = viewModel.uiState.first { it.accentStyle == AccentStyle.GRADIENT }
        assertEquals(AccentGradient.OCEAN, selectedGradient.accentGradient)
        assertEquals(0xFF112233.toInt(), selectedGradient.customGradientStart)
        assertEquals(0xFF445566.toInt(), selectedGradient.customGradientEnd)

        viewModel.onEvent(SettingsUiEvent.OnDynamicColorChanged(true))
        testScheduler.runCurrent()
        val dynamicEnabled = viewModel.uiState.first { it.dynamicColor }
        assertEquals(0xFFE92B2B.toInt(), dynamicEnabled.customColor)
        assertEquals(AccentStyle.GRADIENT, dynamicEnabled.accentStyle)
        assertEquals(AccentGradient.OCEAN, dynamicEnabled.accentGradient)

        viewModel.onEvent(SettingsUiEvent.OnDynamicColorChanged(false))
        testScheduler.runCurrent()
        val dynamicDisabled = viewModel.uiState.first { !it.dynamicColor }
        assertEquals(0xFFE92B2B.toInt(), dynamicDisabled.customColor)
        assertEquals(AccentStyle.GRADIENT, dynamicDisabled.accentStyle)

        viewModel.onEvent(SettingsUiEvent.OnLockScreenNotificationChanged(false))
        testScheduler.runCurrent()
        assertFalse(viewModel.uiState.first { !it.lockScreenNotification }.lockScreenNotification)

        viewModel.onEvent(SettingsUiEvent.OnNotificationBarChanged(true))
        testScheduler.runCurrent()
        assertTrue(viewModel.uiState.first { it.showUploadSpeed }.showUploadSpeed)
        collectJob.cancel()
    }
}
