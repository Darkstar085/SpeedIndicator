package com.sipun.netspeedindicator.domain.model

/** Launcher-visible application that can be mapped to a shared UID. */
data class InstalledAppInfo(
    val packageName: String,
    val appName: String
)
