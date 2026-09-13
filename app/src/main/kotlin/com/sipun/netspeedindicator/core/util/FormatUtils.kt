package com.sipun.netspeedindicator.core.util

import java.util.Locale
import kotlin.math.ln
import kotlin.math.log10
import kotlin.math.pow

object FormatUtils {
    fun formatBytes(bytes: Long): String {
        if (bytes <= 0) return "0 B"
        val units = arrayOf("B", "KB", "MB", "GB", "TB", "PB")
        val digitGroups = (ln(bytes.toDouble()) / ln(1024.0)).toInt().coerceIn(0, units.lastIndex)
        val value = bytes / 1024.0.pow(digitGroups.toDouble())
        return when (digitGroups) {
            0, 1 -> "%.0f %s".format(Locale.US, value, units[digitGroups])
            2 -> "%.1f %s".format(Locale.US, value, units[digitGroups])
            else -> "%.2f %s".format(Locale.US, value, units[digitGroups])
        }
    }

    fun formatSpeed(bytesPerSecond: Long): String {
        if (bytesPerSecond < 0) return "0 B/s"
        if (bytesPerSecond < 1024) return "$bytesPerSecond B/s"
        val units = arrayOf("B/s", "KB/s", "MB/s", "GB/s", "TB/s")
        val digitGroups = (log10(bytesPerSecond.toDouble()) / log10(1024.0)).toInt().coerceIn(0, units.lastIndex)
        val value = bytesPerSecond / 1024.0.pow(digitGroups.toDouble())
        return String.format(Locale.US, "%.1f %s", value, units[digitGroups])
    }

    fun formatSpeedCompact(bytesPerSecond: Long): Pair<String, String> {
        if (bytesPerSecond < 0) return "0" to "B/s"
        if (bytesPerSecond < 1024L) return bytesPerSecond.toString() to "B/s"
        val units = arrayOf("KB/s", "MB/s", "GB/s")
        val thresholds = longArrayOf(1024L, 1024L * 1024, 1024L * 1024 * 1024)
        var index = 0
        while (index < thresholds.lastIndex && bytesPerSecond >= thresholds[index + 1]) index++
        val value = bytesPerSecond.toDouble() / thresholds[index].toDouble()
        val formatted = if (index == 0) String.format(Locale.US, "%.0f", value)
        else if (value >= 99.95) String.format(Locale.US, "%.0f", value)
        else String.format(Locale.US, "%.1f", value)
        return formatted to units[index]
    }

    fun formatSpeedValue(bytesPerSecond: Long): String {
        if (bytesPerSecond < 0) return "0"
        if (bytesPerSecond < 1024) return "$bytesPerSecond"
        val units = arrayOf("B/s", "KB/s", "MB/s", "GB/s", "TB/s")
        val digitGroups = (log10(bytesPerSecond.toDouble()) / log10(1024.0)).toInt().coerceIn(0, units.lastIndex)
        val value = bytesPerSecond / 1024.0.pow(digitGroups.toDouble())
        return String.format(Locale.US, "%.1f", value)
    }

    fun formatSpeedUnit(bytesPerSecond: Long): String {
        if (bytesPerSecond < 0 || bytesPerSecond < 1024) return "B/s"
        val units = arrayOf("B/s", "KB/s", "MB/s", "GB/s", "TB/s")
        val digitGroups = (log10(bytesPerSecond.toDouble()) / log10(1024.0)).toInt().coerceIn(0, units.lastIndex)
        return units[digitGroups]
    }

    fun formatBytesValue(bytes: Long): String {
        if (bytes <= 0L) return "0"
        if (bytes < 1024) return bytes.toString()
        val digitGroups = (log10(bytes.toDouble()) / log10(1024.0)).toInt().coerceIn(0, 4)
        val value = bytes / 1024.0.pow(digitGroups.toDouble())
        return when (digitGroups) {
            1 -> "%.0f".format(Locale.US, value)
            2 -> "%.1f".format(Locale.US, value)
            else -> "%.2f".format(Locale.US, value)
        }
    }

    fun formatBytesUnit(bytes: Long): String {
        if (bytes <= 0L) return "B"
        val units = listOf("B", "KB", "MB", "GB", "TB")
        var value = bytes.toDouble()
        var index = 0
        while (value >= 1024 && index < units.lastIndex) { value /= 1024; index++ }
        return units[index]
    }

    fun formatDuration(seconds: Long): String {
        val h = seconds / 3600
        val m = (seconds % 3600) / 60
        val s = seconds % 60
        return String.format(Locale.US, "%02d:%02d:%02d", h, m, s)
    }
}
