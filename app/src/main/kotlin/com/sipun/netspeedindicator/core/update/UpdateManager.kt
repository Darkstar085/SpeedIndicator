package com.sipun.netspeedindicator.core.update

import android.content.Context
import androidx.work.Constraints
import androidx.work.Data
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.time.Instant
import java.time.ZoneId
import java.time.format.DateTimeFormatter
import java.util.Locale
import java.util.concurrent.TimeUnit

@Serializable
data class GithubRelease(
    @SerialName("tag_name") val tagName: String,
    val name: String = "",
    val body: String = "",
    @SerialName("published_at") val publishedAt: String? = null,
    val draft: Boolean = false,
    val prerelease: Boolean = false,
    val assets: List<GithubAsset> = emptyList()
)

@Serializable
data class GithubAsset(
    val name: String,
    @SerialName("browser_download_url") val downloadUrl: String,
    @SerialName("content_type") val contentType: String = "",
    val size: Long = 0,
    val digest: String? = null
)

data class AppUpdate(
    val tag: String,
    val version: String,
    val name: String,
    val notes: String,
    val downloadUrl: String,
    val fileName: String,
    val size: Long,
    val digest: String?,
    val releaseDate: String?
)

object UpdateManager {
    const val ACTION_DOWNLOAD_UPDATE = "com.sipun.netspeedindicator.DOWNLOAD_UPDATE"
    const val ACTION_INSTALL_UPDATE = "com.sipun.netspeedindicator.INSTALL_UPDATE"

    private const val REPOSITORY = "Darkstar085/SpeedIndicator"
    private const val API_URL = "https://api.github.com/repos/$REPOSITORY/releases/latest"
    private const val PREFS = "app_updates"
    private const val KEY_TAG = "pending_tag"
    private const val KEY_VERSION = "pending_version"
    private const val KEY_NAME = "pending_name"
    private const val KEY_NOTES = "pending_notes"
    private const val KEY_URL = "pending_url"
    private const val KEY_FILE = "pending_file"
    private const val KEY_SIZE = "pending_size"
    private const val KEY_DIGEST = "pending_digest"
    private const val KEY_RELEASE_DATE = "pending_release_date"
    private const val KEY_NOTIFIED = "last_notified_tag"
    private const val CHECK_WORK = "release_update_check"

    private val json = Json { ignoreUnknownKeys = true }

    suspend fun findLatestUpdate(context: Context): AppUpdate? = withContext(Dispatchers.IO) {
        try {
            val currentVersion = currentVersion(context)
            val connection = (URL(API_URL).openConnection() as HttpURLConnection).apply {
                requestMethod = "GET"
                connectTimeout = 10_000
                readTimeout = 15_000
                setRequestProperty("Accept", "application/vnd.github+json")
                setRequestProperty("X-GitHub-Api-Version", "2026-03-10")
            }

            try {
                if (connection.responseCode != HttpURLConnection.HTTP_OK) return@withContext null
                val release = connection.inputStream.bufferedReader().use { json.decodeFromString<GithubRelease>(it.readText()) }
                if (release.draft || release.prerelease) return@withContext null

                val version = release.tagName.removePrefix("v").trim()
                if (!isNewerVersion(currentVersion, version)) return@withContext null

                val asset = release.assets.firstOrNull {
                    it.name.endsWith(".apk", ignoreCase = true) ||
                        it.contentType.equals("application/vnd.android.package-archive", ignoreCase = true)
                } ?: return@withContext null

                AppUpdate(
                    tag = release.tagName,
                    version = version,
                    name = release.name.ifBlank { "Net Speed Indicator $version" },
                    notes = release.body.toReleaseNotes(),
                    downloadUrl = asset.downloadUrl,
                    fileName = asset.name,
                    size = asset.size,
                    digest = asset.digest,
                    releaseDate = release.publishedAt?.let(::formatReleaseDate)
                )
            } finally {
                connection.disconnect()
            }
        } catch (_: Exception) {
            null
        }
    }

    fun savePendingUpdate(context: Context, update: AppUpdate) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit()
            .putString(KEY_TAG, update.tag)
            .putString(KEY_VERSION, update.version)
            .putString(KEY_NAME, update.name)
            .putString(KEY_NOTES, update.notes)
            .putString(KEY_URL, update.downloadUrl)
            .putString(KEY_FILE, update.fileName)
            .putLong(KEY_SIZE, update.size)
            .putString(KEY_DIGEST, update.digest)
            .putString(KEY_RELEASE_DATE, update.releaseDate)
            .apply()
    }

    fun getPendingUpdate(context: Context): AppUpdate? {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val tag = prefs.getString(KEY_TAG, null) ?: return null
        val version = prefs.getString(KEY_VERSION, null) ?: return null
        if (!isNewerVersion(currentVersion(context), version)) {
            clearPendingUpdate(context)
            return null
        }
        val name = prefs.getString(KEY_NAME, null) ?: return null
        val notes = prefs.getString(KEY_NOTES, "").orEmpty()
        val url = prefs.getString(KEY_URL, null) ?: return null
        val file = prefs.getString(KEY_FILE, null) ?: return null
        val releaseDate = prefs.getString(KEY_RELEASE_DATE, null)
        return AppUpdate(tag, version, name, notes, url, file, prefs.getLong(KEY_SIZE, 0), prefs.getString(KEY_DIGEST, null), releaseDate)
    }

    fun getDownloadedUpdate(context: Context): AppUpdate? {
        val update = getPendingUpdate(context) ?: return null
        val apk = File(File(context.filesDir, "updates"), update.fileName)
        return update.takeIf { apk.isFile && apk.length() > 0L }
    }

    fun clearPendingUpdate(context: Context) {
        val prefs = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        prefs.getString(KEY_FILE, null)?.let { fileName ->
            File(File(context.filesDir, "updates"), fileName).delete()
        }
        prefs.edit()
            .remove(KEY_TAG)
            .remove(KEY_VERSION)
            .remove(KEY_NAME)
            .remove(KEY_NOTES)
            .remove(KEY_URL)
            .remove(KEY_FILE)
            .remove(KEY_SIZE)
            .remove(KEY_DIGEST)
            .remove(KEY_RELEASE_DATE)
            .apply()
    }

    fun wasNotified(context: Context, tag: String): Boolean =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(KEY_NOTIFIED, null) == tag

    fun markNotified(context: Context, tag: String) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putString(KEY_NOTIFIED, tag).apply()
    }

    fun enqueuePeriodicCheck(context: Context) {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .setRequiresBatteryNotLow(true)
            .build()
        val request = PeriodicWorkRequestBuilder<UpdateCheckWorker>(6, TimeUnit.HOURS)
            .setConstraints(constraints)
            .build()
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(CHECK_WORK, ExistingPeriodicWorkPolicy.KEEP, request)
    }

    fun enqueueDownload(context: Context, update: AppUpdate? = null) {
        val target = update ?: getPendingUpdate(context) ?: return
        val dataBuilder = Data.Builder()
            .putString(UpdateDownloadWorker.KEY_TAG, target.tag)
            .putString(UpdateDownloadWorker.KEY_URL, target.downloadUrl)
            .putString(UpdateDownloadWorker.KEY_FILE, target.fileName)
        target.digest?.let { dataBuilder.putString(UpdateDownloadWorker.KEY_DIGEST, it) }
        val request = OneTimeWorkRequestBuilder<UpdateDownloadWorker>()
            .setInputData(dataBuilder.build())
            .build()
        WorkManager.getInstance(context).enqueueUniqueWork(
            "update_download_${target.tag}",
            ExistingWorkPolicy.KEEP,
            request
        )
    }

    private fun currentVersion(context: Context): String = context.packageManager
        .getPackageInfo(context.packageName, 0)
        .versionName
        .orEmpty()

    private fun isNewerVersion(current: String, latest: String): Boolean {
        val currentParts = current.removePrefix("v").split(".").map { it.takeWhile(Char::isDigit).toIntOrNull() ?: 0 }
        val latestParts = latest.removePrefix("v").split(".").map { it.takeWhile(Char::isDigit).toIntOrNull() ?: 0 }
        val count = maxOf(currentParts.size, latestParts.size)
        for (index in 0 until count) {
            val currentPart = currentParts.getOrElse(index) { 0 }
            val latestPart = latestParts.getOrElse(index) { 0 }
            if (latestPart != currentPart) return latestPart > currentPart
        }
        return false
    }

    private fun String.toReleaseNotes(): String = lineSequence()
        .map { it.trim() }
        .filter { it.startsWith("-") }
        .map { it.removePrefix("-").trim().replace(Regex("\\[([^]]+)]\\([^)]*\\)"), "$1") }
        .joinToString("\n")

    private fun formatReleaseDate(value: String): String? = runCatching {
        Instant.parse(value)
            .atZone(ZoneId.systemDefault())
            .format(DateTimeFormatter.ofPattern("MMM d, yyyy", Locale.getDefault()))
    }.getOrNull()
}
