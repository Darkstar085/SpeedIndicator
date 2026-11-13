package com.sipun.netspeedindicator.data.preferences

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
class PreferenceManagerTest {

    private lateinit var preferenceManager: PreferenceManager

    @Before
    fun setUp() = runTest {
        val context = ApplicationProvider.getApplicationContext<Context>()
        context.appPreferencesDataStore.edit { it.clear() }
        preferenceManager = PreferenceManager(context).apply {
            ioDispatcher = UnconfinedTestDispatcher()
        }
    }

    @Test
    fun `isMonitoringEnabled defaults to true`() {
        assertTrue(preferenceManager.isMonitoringEnabled())
    }

    @Test
    fun `setMonitoringEnabled updates synchronous read`() = runTest {
        preferenceManager.setMonitoringEnabled(false)
        assertFalse(preferenceManager.isMonitoringEnabled())
        preferenceManager.setMonitoringEnabled(true)
        assertTrue(preferenceManager.isMonitoringEnabled())
    }

    @Test
    fun `custom color defaults to blue and persists`() = runTest {
        assertEquals(0xFF42A5F5.toInt(), preferenceManager.customColor.first())
        preferenceManager.setCustomColor(0xFFE92B2B.toInt())
        assertEquals(0xFFE92B2B.toInt(), preferenceManager.customColor.first())
    }

    @Test
    fun `custom color keeps a previously selected accent preset`() = runTest {
        val context = ApplicationProvider.getApplicationContext<Context>()
        context.appPreferencesDataStore.edit { it[intPreferencesKey(PreferenceManager.KEY_ACCENT)] = 3 }
        assertEquals(0xFF7C3AED.toInt(), preferenceManager.customColor.first())
    }


    @Test
    fun `accent gradient settings default and persist`() = runTest {
        assertEquals(AccentStyle.SOLID, preferenceManager.accentStyle.first())
        assertEquals(AccentGradient.SUNSET, preferenceManager.accentGradient.first())
        assertEquals(0xFFE92DE2.toInt(), preferenceManager.customGradientStart.first())
        assertEquals(0xFF4A00E0.toInt(), preferenceManager.customGradientEnd.first())

        preferenceManager.setAccentStyle(AccentStyle.GRADIENT)
        preferenceManager.setAccentGradient(AccentGradient.OCEAN)
        preferenceManager.setCustomGradientColors(0xFF112233.toInt(), 0xFF445566.toInt())
        assertEquals(AccentStyle.GRADIENT, preferenceManager.accentStyle.first())
        assertEquals(AccentGradient.OCEAN, preferenceManager.accentGradient.first())
        assertEquals(0xFF112233.toInt(), preferenceManager.customGradientStart.first())
        assertEquals(0xFF445566.toInt(), preferenceManager.customGradientEnd.first())
    }

    @Test
    fun `onboardingSeen defaults to false`() = runTest {
        assertFalse(preferenceManager.onboardingSeen.first())
    }

    @Test
    fun `setOnboardingSeen persists and emits`() = runTest {
        preferenceManager.setOnboardingSeen(true)
        assertTrue(preferenceManager.onboardingSeen.first())
    }

    @Test
    fun `monitoringEnabled survives manager recreation`() = runTest {
        preferenceManager.setMonitoringEnabled(false)
        // Simulate a cold start: a new manager must observe the persisted flag
        // synchronously, without waiting for the DataStore flow to emit.
        val context = ApplicationProvider.getApplicationContext<Context>()
        assertFalse(PreferenceManager(context).isMonitoringEnabled())
    }

    @Test
    fun `appTheme default is System (0)`() = runTest {
        val collectJob = launch { preferenceManager.appTheme.collect {} }
        testScheduler.runCurrent()
        assertEquals(0, preferenceManager.appTheme.first())
        collectJob.cancel()
    }

    @Test
    fun `setAppTheme persists and emits new value`() = runTest {
        val collectJob = launch { preferenceManager.appTheme.collect {} }
        testScheduler.runCurrent()

        preferenceManager.setAppTheme(1)
        testScheduler.runCurrent()
        assertEquals(1, preferenceManager.appTheme.first())

        preferenceManager.setAppTheme(2)
        testScheduler.runCurrent()
        assertEquals(2, preferenceManager.appTheme.first())

        collectJob.cancel()
    }

    @Test
    fun `dynamicColor default is false`() = runTest {
        val collectJob = launch { preferenceManager.dynamicColor.collect {} }
        testScheduler.runCurrent()
        assertFalse(preferenceManager.dynamicColor.first())
        collectJob.cancel()
    }

    @Test
    fun `setDynamicColor persists and emits new value`() = runTest {
        val collectJob = launch { preferenceManager.dynamicColor.collect {} }
        testScheduler.runCurrent()

        preferenceManager.setDynamicColor(false)
        testScheduler.runCurrent()
        assertFalse(preferenceManager.dynamicColor.first())

        preferenceManager.setDynamicColor(true)
        testScheduler.runCurrent()
        assertTrue(preferenceManager.dynamicColor.first())

        collectJob.cancel()
    }

    @Test
    fun `lockScreenNotification default is true`() = runTest {
        val collectJob = launch { preferenceManager.lockScreenNotification.collect {} }
        testScheduler.runCurrent()
        assertTrue(preferenceManager.lockScreenNotification.first())
        collectJob.cancel()
    }

    @Test
    fun `setLockScreenNotification persists and emits new value`() = runTest {
        val collectJob = launch { preferenceManager.lockScreenNotification.collect {} }
        testScheduler.runCurrent()

        preferenceManager.setLockScreenNotification(false)
        testScheduler.runCurrent()
        assertFalse(preferenceManager.lockScreenNotification.first())

        collectJob.cancel()
    }

    @Test
    fun `showUploadSpeed default is false`() = runTest {
        val collectJob = launch { preferenceManager.showUploadSpeed.collect {} }
        testScheduler.runCurrent()
        assertFalse(preferenceManager.showUploadSpeed.first())
        collectJob.cancel()
    }

    @Test
    fun `setShowUploadSpeed persists and emits new value`() = runTest {
        val collectJob = launch { preferenceManager.showUploadSpeed.collect {} }
        testScheduler.runCurrent()

        preferenceManager.setShowUploadSpeed(true)
        testScheduler.runCurrent()
        assertTrue(preferenceManager.showUploadSpeed.first())

        collectJob.cancel()
    }
}
