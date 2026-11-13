package com.sipun.netspeedindicator.data.datasource

import android.content.Context
import android.content.Intent
import android.os.SystemClock
import com.sipun.netspeedindicator.domain.model.InstalledAppInfo
import com.sipun.netspeedindicator.domain.repository.InstalledAppsProvider
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Cached [InstalledAppsProvider]. The launcher app list changes rarely, so
 * results are cached for [CACHE_TTL_MS] and refreshed on demand afterwards.
 */
@Singleton
class InstalledAppsProviderImpl @Inject constructor(
    @ApplicationContext private val context: Context
) : InstalledAppsProvider {

    private companion object {
        const val CACHE_TTL_MS = 5 * 60 * 1000L
    }

    private data class CacheEntry(val timestamp: Long, val apps: Map<Int, InstalledAppInfo>)

    @Volatile
    private var cache: CacheEntry? = null

    private val lock = Any()

    override fun launcherApps(): Map<Int, InstalledAppInfo> {
        val now = SystemClock.elapsedRealtime()
        cache?.let { if (now - it.timestamp < CACHE_TTL_MS) return it.apps }
        synchronized(lock) {
            cache?.let { if (now - it.timestamp < CACHE_TTL_MS) return it.apps }
            val fresh = queryLauncherApps()
            cache = CacheEntry(now, fresh)
            return fresh
        }
    }

    private fun queryLauncherApps(): Map<Int, InstalledAppInfo> {
        val pm = context.packageManager
        val launcherIntent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        val result = LinkedHashMap<Int, InstalledAppInfo>()

        for (info in pm.queryIntentActivities(launcherIntent, 0)) {
            val app = info.activityInfo?.applicationInfo ?: continue
            val label = app.loadLabel(pm).toString()
            if (label.isNotBlank()) {
                result.putIfAbsent(
                    app.uid,
                    InstalledAppInfo(packageName = app.packageName, appName = label)
                )
            }
        }
        return result
    }
}
