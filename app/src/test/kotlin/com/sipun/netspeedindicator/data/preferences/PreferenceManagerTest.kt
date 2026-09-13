package com.sipun.netspeedindicator.data.preferences

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner

@RunWith(RobolectricTestRunner::class)
class PreferenceManagerTest {

    private lateinit var preferenceManager: PreferenceManager

    @Before
    fun setUp() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        preferenceManager = PreferenceManager(context)
    }

    @Test
    fun `appTheme default is System (0)`() = runTest {
        assertEquals(0, preferenceManager.appTheme.first())
    }

    @Test
    fun `setAppTheme persists and emits new value`() = runTest {
        val collectJob = launch { preferenceManager.appTheme.collect {} }

        preferenceManager.setAppTheme(1)
        assertEquals(1, preferenceManager.appTheme.first())

        preferenceManager.setAppTheme(2)
        assertEquals(2, preferenceManager.appTheme.first())

        collectJob.cancel()
    }

    @Test
    fun `dynamicColor default is true`() = runTest {
        assertTrue(preferenceManager.dynamicColor.first())
    }

    @Test
    fun `setDynamicColor persists and emits new value`() = runTest {
        val collectJob = launch { preferenceManager.dynamicColor.collect {} }

        preferenceManager.setDynamicColor(false)
        assertFalse(preferenceManager.dynamicColor.first())

        preferenceManager.setDynamicColor(true)
        assertTrue(preferenceManager.dynamicColor.first())

        collectJob.cancel()
    }

    @Test
    fun `lockScreenNotification default is true`() = runTest {
        assertTrue(preferenceManager.lockScreenNotification.first())
    }

    @Test
    fun `setLockScreenNotification persists and emits new value`() = runTest {
        val collectJob = launch { preferenceManager.lockScreenNotification.collect {} }

        preferenceManager.setLockScreenNotification(false)
        assertFalse(preferenceManager.lockScreenNotification.first())

        collectJob.cancel()
    }

    @Test
    fun `showUploadSpeed default is false`() = runTest {
        assertFalse(preferenceManager.showUploadSpeed.first())
    }

    @Test
    fun `setShowUploadSpeed persists and emits new value`() = runTest {
        val collectJob = launch { preferenceManager.showUploadSpeed.collect {} }

        preferenceManager.setShowUploadSpeed(true)
        assertTrue(preferenceManager.showUploadSpeed.first())

        collectJob.cancel()
    }
}
