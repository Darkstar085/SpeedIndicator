package com.sipun.netspeedindicator.data.manager

import com.sipun.netspeedindicator.domain.model.SpeedInfo
import com.sipun.netspeedindicator.domain.model.UsageInfo
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

class TrafficStateManagerTest {

    private lateinit var manager: TrafficStateManager

    @Before
    fun setUp() {
        manager = TrafficStateManager()
    }

    @Test
    fun `initial state has default values`() {
        assertEquals(0L, manager.speed.value.totalBytesPerSecond)
        assertEquals(0L, manager.dailyUsage.value.totalBytes)
        assertFalse(manager.isServiceRunning.value)
        assertEquals(0L, manager.peakSpeedBytesPerSecond.value)
        assertEquals(0L, manager.monitoringStartElapsedRealtime.value)
    }

    @Test
    fun `updateSpeed updates speed and tracks peak`() {
        manager.updateSpeed(SpeedInfo(totalBytesPerSecond = 100L))
        assertEquals(100L, manager.speed.value.totalBytesPerSecond)
        assertEquals(100L, manager.peakSpeedBytesPerSecond.value)

        manager.updateSpeed(SpeedInfo(totalBytesPerSecond = 50L))
        assertEquals(100L, manager.peakSpeedBytesPerSecond.value)

        manager.updateSpeed(SpeedInfo(totalBytesPerSecond = 200L))
        assertEquals(200L, manager.peakSpeedBytesPerSecond.value)
    }

    @Test
    fun `updateDailyUsage updates daily usage`() {
        val usage = UsageInfo(date = "2024-01-01", wifiRxBytes = 1000L)
        manager.updateDailyUsage(usage)
        assertEquals(usage, manager.dailyUsage.value)
    }

    @Test
    fun `setServiceRunning starts monitoring and resets peak`() {
        manager.updateSpeed(SpeedInfo(totalBytesPerSecond = 500L))
        manager.setServiceRunning(true)

        assertTrue(manager.isServiceRunning.value)
        assertEquals(0L, manager.peakSpeedBytesPerSecond.value)
        assertTrue(manager.monitoringStartElapsedRealtime.value > 0L)
    }

    @Test
    fun `setServiceRunning false preserves peak`() {
        manager.setServiceRunning(true)
        manager.updateSpeed(SpeedInfo(totalBytesPerSecond = 300L))
        manager.setServiceRunning(false)

        assertFalse(manager.isServiceRunning.value)
        assertEquals(300L, manager.peakSpeedBytesPerSecond.value)
    }
}
