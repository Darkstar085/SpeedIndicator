package com.sipun.netspeedindicator.data.preferences

import android.content.Context
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

import androidx.datastore.preferences.core.edit

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
    fun `dynamicColor default is true`() = runTest {
        val collectJob = launch { preferenceManager.dynamicColor.collect {} }
        testScheduler.runCurrent()
        assertTrue(preferenceManager.dynamicColor.first())
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
