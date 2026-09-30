package com.sipun.netspeedindicator.data.datasource

import android.app.AppOpsManager
import android.app.usage.NetworkStats
import android.app.usage.NetworkStatsManager
import android.content.Context
import android.content.Intent
import android.net.ConnectivityManager
import android.os.Process
import android.provider.Settings
import android.util.Log
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
    @ApplicationContext private val context: Context
) {
    companion object { private const val TAG = "AppDataUsageDataSource" }

    private val networkStatsManager: NetworkStatsManager? by lazy {
        context.getSystemService(Context.NETWORK_STATS_SERVICE) as? NetworkStatsManager
    }

    fun hasUsageAccess(): Boolean {
        val appOps = context.getSystemService(Context.APP_OPS_SERVICE) as? AppOpsManager ?: return false
        return appOps.checkOpNoThrow(
            AppOpsManager.OPSTR_GET_USAGE_STATS,
            Process.myUid(),
            context.packageName
        ) == AppOpsManager.MODE_ALLOWED
    }

    fun openUsageAccessSettings() {
        context.startActivity(
            Intent(Settings.ACTION_USAGE_ACCESS_SETTINGS)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
        )
    }

    @Suppress("DEPRECATION")
    fun getAppDataUsage(startMillis: Long, endMillis: Long): List<AppDataUsageRecord> {
        val manager = networkStatsManager ?: return emptyList()
        if (!hasUsageAccess()) return emptyList()

        return try {
            val applications = visibleApplications()
            val usageByUid = HashMap<Int, MutableUsage>()

            collect(manager, ConnectivityManager.TYPE_WIFI, startMillis, endMillis, usageByUid, true)
            collect(manager, ConnectivityManager.TYPE_MOBILE, startMillis, endMillis, usageByUid, false)

            usageByUid.mapNotNull { (uid, usage) ->
                val app = applications[uid] ?: return@mapNotNull null
                if (usage.totalBytes <= 0L) return@mapNotNull null

                AppDataUsageRecord(
                    uid = uid,
                    packageName = app.first,
                    appName = app.second,
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

    private fun visibleApplications(): Map<Int, Pair<String, String>> {
        val pm = context.packageManager
        val launcherIntent = Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER)
        val result = LinkedHashMap<Int, Pair<String, String>>()

        for (info in pm.queryIntentActivities(launcherIntent, 0)) {
            val app = info.activityInfo?.applicationInfo ?: continue
            val label = app.loadLabel(pm).toString()
            if (label.isNotBlank()) {
                result.putIfAbsent(app.uid, app.packageName to label)
            }
        }
        return result
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