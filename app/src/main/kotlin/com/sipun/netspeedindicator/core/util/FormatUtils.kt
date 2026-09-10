package com.sipun.netspeedindicator.core.util

import java.util.Locale
import kotlin.math.ln
import kotlin.math.log10
import kotlin.math.pow

object FormatUtils {
    fun formatBytes(bytes: Long): String {
        if (bytes <= 0) return "0 B"
        val units = arrayOf("B", "KB", "MB", "GB", "TB", "PB")
        val group = (ln(bytes.toDouble()) / ln(1024.0)).toInt().coerceAtMost(units.lastIndex)
        val value = bytes / 1024.0.pow(group.toDouble())
        return when (group) {
            0, 1 -> "%.0f %s".format(Locale.US, value, units[group])
            2 -> "%.1f %s".format(Locale.US, value, units[group])
            else -> "%.2f %s".format(Locale.US, value, units[group])
        }
    }

    fun formatSpeed(bytesPerSecond: Long): String {
        if (bytesPerSecond < 0) return "0 B/s"
        if (bytesPerSecond < 1024) return "$bytesPerSecond B/s"
        val units = arrayOf("B/s", "KB/s", "MB/s", "GB/s", "TB/s")
        val group = (log10(bytesPerSecond.toDouble()) / log10(1024.0)).toInt().coerceAtMost(units.lastIndex)
        val value = bytesPerSecond / 1024.0.pow(group.toDouble())
        return String.format(Locale.US, "%.1f %s", value, units[group])
    }

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
        val formattedValue = if (unit == "KB") String.format(Locale.US, "%.0f", value)
        else if (value >= 99.95) String.format(Locale.US, "%.0f", value)
        else String.format(Locale.US, "%.1f", value)
        return Pair(formattedValue, "$unit/s")
    }

    fun formatSpeedValue(bytesPerSecond: Long): String {
        if (bytesPerSecond < 0) return "0"
        if (bytesPerSecond < 1024) return "$bytesPerSecond"
        val group = (log10(bytesPerSecond.toDouble()) / log10(1024.0)).toInt()
        val value = bytesPerSecond / 1024.0.pow(group.toDouble())
        return String.format(Locale.US, "%.1f", value)
    }

    fun formatSpeedUnit(bytesPerSecond: Long): String {
        if (bytesPerSecond < 1024) return "B/s"
        val units = arrayOf("B/s", "KB/s", "MB/s", "GB/s", "TB/s")
        val group = (log10(bytesPerSecond.toDouble()) / log10(1024.0)).toInt().coerceAtMost(units.lastIndex)
        return units[group]
    }

    fun formatBytesValue(bytes: Long): String {
        if (bytes <= 0L) return "0"
        if (bytes < 1024) return bytes.toString()
        val group = (log10(bytes.toDouble()) / log10(1024.0)).toInt()
        val value = bytes / 1024.0.pow(group.toDouble())
        return when (group) {
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

    fun formatDuration(seconds: Long): String =
        String.format(Locale.US, "%02d:%02d:%02d", seconds / 3600, (seconds % 3600) / 60, seconds % 60)
}
