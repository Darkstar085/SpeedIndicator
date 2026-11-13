package com.sipun.netspeedindicator.data.datasource

import android.app.usage.NetworkStats
import android.app.usage.NetworkStatsManager
import android.content.Context
import android.net.ConnectivityManager
import android.util.Log
import com.sipun.netspeedindicator.core.util.PermissionUtils
import com.sipun.netspeedindicator.domain.repository.InstalledAppsProvider
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

data class AppDataUsageRecord(
    val uid: Int,
    val packageName: String,
    val appName: String,
    val wifiBytes: Long,
    val mobileBytes: Long,
    val downloadBytes: Long,
    val uploadBytes: Long
) {
    val totalBytes: Long get() = wifiBytes + mobileBytes
}

@Singleton
class AppDataUsageDataSource @Inject constructor(
    @ApplicationContext private val context: Context,
    private val installedAppsProvider: InstalledAppsProvider
) {
    companion object {
        private const val TAG = "AppDataUsageDataSource"
    }

    private val networkStatsManager: NetworkStatsManager? by lazy {
        context.getSystemService(Context.NETWORK_STATS_SERVICE) as? NetworkStatsManager
    }

    fun hasUsageAccess(): Boolean = PermissionUtils.hasUsageStatsPermission(context)


    @Suppress("DEPRECATION")
    fun getAppDataUsage(startMillis: Long, endMillis: Long): List<AppDataUsageRecord> {
        val manager = networkStatsManager ?: return emptyList()
        if (!hasUsageAccess()) return emptyList()

        return try {
            val applications = installedAppsProvider.launcherApps()
            val usageByUid = HashMap<Int, MutableUsage>()

            collect(
                manager,
                ConnectivityManager.TYPE_WIFI,
                startMillis,
                endMillis,
                usageByUid,
                true
            )
            collect(
                manager,
                ConnectivityManager.TYPE_MOBILE,
                startMillis,
                endMillis,
                usageByUid,
                false
            )

            usageByUid.mapNotNull { (uid, usage) ->
                val app = applications[uid] ?: return@mapNotNull null
                if (usage.totalBytes <= 0L) return@mapNotNull null

                AppDataUsageRecord(
                    uid = uid,
                    packageName = app.packageName,
                    appName = app.appName,
                    wifiBytes = usage.wifiBytes,
                    mobileBytes = usage.mobileBytes,
                    downloadBytes = usage.downloadBytes,
                    uploadBytes = usage.uploadBytes
                )
            }.sortedByDescending { it.totalBytes }
        } catch (e: SecurityException) {
            Log.w(TAG, "Network usage access is unavailable", e)
            emptyList()
        } catch (e: Exception) {
            Log.e(TAG, "Failed to query per-app network usage", e)
            emptyList()
        }
    }

    @Suppress("DEPRECATION")
    private fun collect(
        manager: NetworkStatsManager,
        networkType: Int,
        startMillis: Long,
        endMillis: Long,
        usageByUid: MutableMap<Int, MutableUsage>,
        wifi: Boolean
    ) {
        val stats = manager.querySummary(networkType, null, startMillis, endMillis)
        stats.use {
            val bucket = NetworkStats.Bucket()
            while (it.hasNextBucket()) {
                it.getNextBucket(bucket)
                if (bucket.uid < 0) continue

                val usage = usageByUid.getOrPut(bucket.uid) { MutableUsage() }
                val rx = bucket.rxBytes.coerceAtLeast(0L)
                val tx = bucket.txBytes.coerceAtLeast(0L)

                if (wifi) {
                    usage.wifiBytes += rx + tx
                } else {
                    usage.mobileBytes += rx + tx
                }

                usage.downloadBytes += rx
                usage.uploadBytes += tx
            }
        }
    }

    private class MutableUsage(
        var wifiBytes: Long = 0L,
        var mobileBytes: Long = 0L,
        var downloadBytes: Long = 0L,
        var uploadBytes: Long = 0L
    ) {
        val totalBytes: Long get() = wifiBytes + mobileBytes
    }
}