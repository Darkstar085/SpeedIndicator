package com.sipun.netspeedindicator.core.util

import java.util.Locale
import kotlin.math.ln
import kotlin.math.log10
import kotlin.math.pow

/**
 * Utility functions for formatting bytes and speeds
 */
object FormatUtils {

    /**
     * Format bytes to human-readable string
     * @param bytes Number of bytes
     * @return Formatted string (e.g., "1.52 GB", "250.1 MB", "10 KB")
     */
    fun formatBytes(bytes: Long): String {
        if (bytes <= 0) return "0 B"

        val units = arrayOf("B", "KB", "MB", "GB", "TB", "PB")
        // Calculate which unit group it belongs to: index = log1024(bytes)
        val digitGroups = (ln(bytes.toDouble()) / ln(1024.0)).toInt()
            .coerceAtMost(units.lastIndex)

        val value = bytes / 1024.0.pow(digitGroups.toDouble())

        return when (digitGroups) {
            0, 1 -> "%.0f %s".format(Locale.US, value, units[digitGroups])
            2 -> "%.1f %s".format(Locale.US, value, units[digitGroups])
            else -> "%.2f %s".format(Locale.US, value, units[digitGroups])
        }
    }

    /**
     * Format speed (bytes per second) to human-readable string
     * @param bytesPerSecond Speed in bytes per second
     * @return Formatted string (e.g., "2.1 MB/s", "480 KB/s", "5 Mbps")
     */
    fun formatSpeed(bytesPerSecond: Long): String {
        if (bytesPerSecond < 0) return "0 B/s"
        if (bytesPerSecond < 1024) return "$bytesPerSecond B/s"

        val units = arrayOf("B/s", "KB/s", "MB/s", "GB/s", "TB/s")
        val digitGroups = (log10(bytesPerSecond.toDouble()) / log10(1024.0)).toInt()
            .coerceAtMost(units.lastIndex)

        val value = bytesPerSecond / 1024.0.pow(digitGroups.toDouble())
        return String.format(Locale.US, "%.1f %s", value, units[digitGroups])
    }

    /**
     * Format speed for status bar icon (compact)
     * @param bytesPerSecond Speed in bytes per second
     * @return Pair of (Value String, Unit String) e.g. ("50", "KB")
     */
    fun formatSpeedCompact(bytesPerSecond: Long): Pair<String, String> {
        val value: Double
        val unit: String

        if (bytesPerSecond >= 1024.0 * 1024 * 1024 * 0.9995) {
            value = bytesPerSecond / (1024.0 * 1024 * 1024)
            unit = "GB"
        } else if (bytesPerSecond >= 1024.0 * 1024 * 0.9995) {
            value = bytesPerSecond / (1024.0 * 1024)
            unit = "MB"
        } else {
            value = bytesPerSecond / 1024.0
            unit = "KB"
        }

        val formattedValue = if (unit == "KB") {
            String.format(Locale.US, "%.0f", value)
        } else {
            if (value >= 99.95) {
                String.format(Locale.US, "%.0f", value)
            } else {
                String.format(Locale.US, "%.1f", value)
            }
        }

        return Pair(formattedValue, "$unit/s")
    }

    /**
     * Format speed - returns only the numeric value part
     */
    fun formatSpeedValue(bytesPerSecond: Long): String {
        if (bytesPerSecond < 0) return "0"
        if (bytesPerSecond < 1024) return "$bytesPerSecond"

        val digitGroups = (log10(bytesPerSecond.toDouble()) / log10(1024.0)).toInt()
        val value = bytesPerSecond / 1024.0.pow(digitGroups.toDouble())
        return String.format(Locale.US, "%.1f", value)
    }

    /**
     * Format speed - returns only the unit part
     */
    fun formatSpeedUnit(bytesPerSecond: Long): String {
        if (bytesPerSecond < 0) return "B/s"
        if (bytesPerSecond < 1024) return "B/s"

        val units = arrayOf("B/s", "KB/s", "MB/s", "GB/s", "TB/s")
        val digitGroups = (log10(bytesPerSecond.toDouble()) / log10(1024.0)).toInt()
            .coerceAtMost(units.lastIndex)
        return units[digitGroups]
    }

    /**
     * Format bytes - returns only the numeric value part
     */
    fun formatBytesValue(bytes: Long): String {
        if (bytes <= 0L) return "0"
        if (bytes < 1024) return bytes.toString()

        val digitGroups = (log10(bytes.toDouble()) / log10(1024.0)).toInt()
        val value = bytes / 1024.0.pow(digitGroups.toDouble())

        return when (digitGroups) {
            1 -> "%.0f".format(Locale.US, value)
            2 -> "%.1f".format(Locale.US, value)
            else -> "%.2f".format(Locale.US, value)
        }
    }

    /**
     * Format bytes - returns only the unit part
     */
    fun formatBytesUnit(bytes: Long): String {
        if (bytes <= 0L) return "B"

        val units = listOf("B", "KB", "MB", "GB", "TB")
        var value = bytes.toDouble()
        var index = 0

        while (value >= 1024 && index < units.lastIndex) {
            value /= 1024
            index++
        }

        return units[index]
    }

    /**
     * Format duration in seconds to HH:MM:SS
     */
    fun formatDuration(seconds: Long): String {
        val h = seconds / 3600
        val m = (seconds % 3600) / 60
        val s = seconds % 60
        return String.format(Locale.US, "%02d:%02d:%02d", h, m, s)
    }
}
