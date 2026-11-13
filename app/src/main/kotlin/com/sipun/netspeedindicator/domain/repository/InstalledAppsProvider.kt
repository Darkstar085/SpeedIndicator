package com.sipun.netspeedindicator.domain.repository

import com.sipun.netspeedindicator.domain.model.InstalledAppInfo

/**
 * Provides a cached map of launcher-visible applications keyed by app UID.
 * Shared by the per-app usage history and the live rates poller so the
 * expensive `PackageManager.queryIntentActivities` call happens at most
 * once every few minutes.
 */
interface InstalledAppsProvider {
    fun launcherApps(): Map<Int, InstalledAppInfo>
}
