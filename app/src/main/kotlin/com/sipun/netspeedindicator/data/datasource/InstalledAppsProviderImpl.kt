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
 * Arbitrary UIDs (non-launcher apps, shared system UIDs) are resolved
 * on demand and memoized for [UID_CACHE_MAX] entries at a time.
 */
@Singleton
class InstalledAppsProviderImpl @Inject constructor(
    @ApplicationContext private val context: Context
) : InstalledAppsProvider {

    private companion object {
        const val CACHE_TTL_MS = 5 * 60 * 1000L
        const val UID_CACHE_MAX = 512
    }

    private data class CacheEntry(val timestamp: Long, val apps: Map<Int, InstalledAppInfo>)

    @Volatile
    private var cache: CacheEntry? = null

    private val lock = Any()

    private val uidCacheLock = Any()
    private val uidCache = HashMap<Int, InstalledAppInfo?>()

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

    override fun appInfoForUid(uid: Int): InstalledAppInfo? {
        launcherApps()[uid]?.let { return it }
        synchronized(uidCacheLock) {
            if (uidCache.containsKey(uid)) return uidCache[uid]
        }
        val resolved = runCatching { resolveUid(uid) }.getOrDefault(null)
        synchronized(uidCacheLock) {
            if (uidCache.size >= UID_CACHE_MAX) uidCache.clear()
            uidCache[uid] = resolved
        }
        return resolved
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

    private fun resolveUid(uid: Int): InstalledAppInfo? {
        val pm = context.packageManager
        val packageName = runCatching { pm.getNameForUid(uid) }.getOrNull()
        if (packageName != null) {
            val label = runCatching {
                pm.getApplicationInfo(packageName, 0).loadLabel(pm).toString().trim()
            }.getOrNull()?.takeIf { it.isNotBlank() }
            return InstalledAppInfo(packageName, label ?: packageName)
        }
        if (uid < 10_000) {
            return InstalledAppInfo(packageName = "uid:$uid", appName = systemUidLabel(uid))
        }
        return null
    }

    private fun systemUidLabel(uid: Int): String = when (uid) {
        0 -> "Root"
        1000 -> "Android System"
        1001 -> "Telephony"
        1007 -> "Media Server"
        1010 -> "Android Media"
        1013 -> "Mediaserver"
        1016 -> "External Storage"
        else -> "System (UID $uid)"
    }
}
