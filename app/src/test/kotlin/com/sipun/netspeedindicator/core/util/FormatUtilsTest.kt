package com.sipun.netspeedindicator.core.util

import org.junit.Assert.assertEquals
import org.junit.Test

class FormatUtilsTest {

    @Test
    fun `formatBytes handles zero and negative as zero bytes`() {
        assertEquals("0 B", FormatUtils.formatBytes(0))
        assertEquals("0 B", FormatUtils.formatBytes(-100))
    }

    @Test
    fun `formatBytes formats each unit group with expected precision`() {
        assertEquals("512 B", FormatUtils.formatBytes(512))
        assertEquals("1 KB", FormatUtils.formatBytes(1024))
        assertEquals("1.0 MB", FormatUtils.formatBytes(1024L * 1024))
        assertEquals("1.00 GB", FormatUtils.formatBytes(1024L * 1024 * 1024))
    }

    @Test
    fun `formatSpeed handles zero negative and normal values`() {
        assertEquals("0 B/s", FormatUtils.formatSpeed(0))
        assertEquals("0 B/s", FormatUtils.formatSpeed(-1))
        assertEquals("500 B/s", FormatUtils.formatSpeed(500))
        assertEquals("1.5 KB/s", FormatUtils.formatSpeed(1536))
        assertEquals("1.0 MB/s", FormatUtils.formatSpeed(1024L * 1024))
    }

    @Test
    fun `formatSpeedCompact keeps low speeds readable`() {
        assertEquals("0" to "B/s", FormatUtils.formatSpeedCompact(0))
        assertEquals("500" to "B/s", FormatUtils.formatSpeedCompact(500))
        assertEquals("1" to "KB/s", FormatUtils.formatSpeedCompact(1024))
        assertEquals("50" to "KB/s", FormatUtils.formatSpeedCompact(50 * 1024L))
    }

    @Test
    fun `formatSpeedCompact switches between MB and GB near threshold`() {
        assertEquals("1.2" to "MB/s", FormatUtils.formatSpeedCompact((1.2 * 1024 * 1024).toLong()))
        assertEquals("2.0" to "GB/s", FormatUtils.formatSpeedCompact(2L * 1024 * 1024 * 1024))
        assertEquals("GB/s", FormatUtils.formatSpeedCompact((1024.0 * 1024 * 1024 * 0.9996).toLong()).second)
        assertEquals("MB/s", FormatUtils.formatSpeedCompact((1024.0 * 1024 * 1024 * 0.9994).toLong()).second)
    }

    @Test
    fun `formatSpeedValue and formatSpeedUnit split values correctly`() {
        assertEquals("1.5", FormatUtils.formatSpeedValue(1536))
        assertEquals("KB/s", FormatUtils.formatSpeedUnit(1536))
        assertEquals("0", FormatUtils.formatSpeedValue(-5))
        assertEquals("B/s", FormatUtils.formatSpeedUnit(-5))
    }

    @Test
    fun `formatBytesValue and formatBytesUnit split values correctly`() {
        assertEquals("0", FormatUtils.formatBytesValue(0))
        assertEquals("B", FormatUtils.formatBytesUnit(0))
        assertEquals("1.0", FormatUtils.formatBytesValue(1024L * 1024))
        assertEquals("MB", FormatUtils.formatBytesUnit(1024L * 1024))
    }

    @Test
    fun `formatDuration pads to HH-MM-SS`() {
        assertEquals("00:00:00", FormatUtils.formatDuration(0))
        assertEquals("00:01:05", FormatUtils.formatDuration(65))
        assertEquals("02:00:00", FormatUtils.formatDuration(7200))
    }

    @Test
    fun `formatSpeed handles very large values`() {
        assertEquals("1.0 TB/s", FormatUtils.formatSpeed(1024L * 1024 * 1024 * 1024))
        assertEquals("TB/s", FormatUtils.formatSpeedUnit(10L * 1024 * 1024 * 1024 * 1024))
    }
}
