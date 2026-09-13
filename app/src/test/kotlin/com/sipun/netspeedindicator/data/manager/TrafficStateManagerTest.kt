package com.sipun.netspeedindicator.data.manager

import com.sipun.netspeedindicator.MainDispatcherRule
import com.sipun.netspeedindicator.domain.model.SpeedInfo
import com.sipun.netspeedindicator.domain.model.UsageInfo
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
class TrafficStateManagerTest {
    @get:Rule val mainDispatcherRule = MainDispatcherRule()
    private lateinit var manager: TrafficStateManager

    @Before fun setUp() { manager = TrafficStateManager() }

    @Test fun `initial state has default values`() = runTest {
        assertEquals(0L, manager.speed.value.downloadBytesPerSecond)
        assertEquals(0L, manager.speed.value.uploadBytesPerSecond)
        assertEquals(0L, manager.speed.value.totalBytesPerSecond)
        assertEquals(0L, manager.dailyUsage.value.wifiRxBytes)
        assertEquals(0L, manager.dailyUsage.value.wifiTxBytes)
        assertEquals(0L, manager.dailyUsage.value.mobileRxBytes)
        assertEquals(0L, manager.dailyUsage.value.mobileTxBytes)
        assertFalse(manager.isServiceRunning.value)
        assertEquals(0L, manager.peakSpeedBytesPerSecond.value)
        assertEquals(0L, manager.monitoringStartElapsedRealtime.value)
    }

    @Test fun `updateSpeed updates speed and tracks peak`() = runTest {
        manager.updateSpeed(SpeedInfo(totalBytesPerSecond = 100L))
        manager.updateSpeed(SpeedInfo(totalBytesPerSecond = 50L))
        assertEquals(50L, manager.speed.value.totalBytesPerSecond)
        assertEquals(100L, manager.peakSpeedBytesPerSecond.value)
        manager.updateSpeed(SpeedInfo(totalBytesPerSecond = 200L))
        assertEquals(200L, manager.speed.value.totalBytesPerSecond)
        assertEquals(200L, manager.peakSpeedBytesPerSecond.value)
    }

    @Test fun `updateDailyUsage updates daily usage`() = runTest {
        val usage = UsageInfo(date = "2024-01-01", wifiRxBytes = 1000L)
        manager.updateDailyUsage(usage)
        assertEquals(usage, manager.dailyUsage.value)
    }

    @Test fun `starting service resets peak and records start time`() = runTest {
        manager.updateSpeed(SpeedInfo(totalBytesPerSecond = 500L))
        manager.setServiceRunning(true)
        assertTrue(manager.isServiceRunning.value)
        assertEquals(0L, manager.peakSpeedBytesPerSecond.value)
        assertTrue(manager.monitoringStartElapsedRealtime.value > 0L)
    }

    @Test fun `stopping service clears live session state`() = runTest {
        manager.setServiceRunning(true)
        manager.updateSpeed(SpeedInfo(totalBytesPerSecond = 300L))
        manager.setServiceRunning(false)
        assertFalse(manager.isServiceRunning.value)
        assertEquals(0L, manager.peakSpeedBytesPerSecond.value)
        assertEquals(0L, manager.monitoringStartElapsedRealtime.value)
        assertEquals(SpeedInfo(), manager.speed.value)
    }
}
