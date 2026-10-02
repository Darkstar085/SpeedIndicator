package com.sipun.netspeedindicator.core.update

import android.content.Context
import androidx.work.Constraints
import androidx.work.Data
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ExistingWorkPolicy
import androidx.work.NetworkType
import androidx.work.OneTimeWorkRequestBuilder
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkInfo
import androidx.work.WorkManager
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import java.io.File
import java.net.HttpURLConnection
import java.net.URL
import java.net.URLDecoder
import java.nio.charset.StandardCharsets
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
    @SerialName("url") val apiUrl: String,
    val name: String,
    @SerialName("browser_download_url") val downloadUrl: String,
    @SerialName("content_type") val contentType: String = "",
    val size: Long = 0,
    val digest: String? = null
)

@Serializable
private data class UpdateManifest(
    val version: String,
    val tag: String,
    val name: String = "",
    val description: String = "",
    val download_url: String,
    val size: Long = 0,
    val sha256: String,
    val published_at: String? = null
)

data class DownloadProgress(
    val downloadedBytes: Long,
    val totalBytes: Long,
    val percent: Int,
    val isFinished: Boolean = false,
    val isFailed: Boolean = false
)

data class AppUpdate(
    val tag: String,
    val version: String,
    val name: String,
    val notes: String,
    val downloadUrl: String,
    val fileName: String,
    val size: Long,
    val digest: String,
    val releaseDate: String?
)

object UpdateManager {
    const val ACTION_DOWNLOAD_UPDATE = "com.sipun.netspeedindicator.DOWNLOAD_UPDATE"
    const val ACTION_SHOW_UPDATE = "com.sipun.netspeedindicator.SHOW_UPDATE"
    const val ACTION_INSTALL_UPDATE = "com.sipun.netspeedindicator.INSTALL_UPDATE"

    private const val REPOSITORY = "Darkstar085/SpeedIndicator"
    private const val API_BASE = "https://api.github.com/repos/" + REPOSITORY
    private const val LATEST_API_URL = API_BASE + "/releases/latest"
    private const val MANIFEST_URL =
        "https://github.com/" + REPOSITORY + "/releases/latest/download/app-release.json"
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
            loadManifest(currentVersion)?.let { return@withContext it }
            loadRelease(currentVersion, LATEST_API_URL)?.let { return@withContext it }
            null
        } catch (_: Exception) {
            null
        }
    }

    private fun loadManifest(currentVersion: String): AppUpdate? {
        val connection = openMetadataConnection(MANIFEST_URL)
        return try {
            if (connection.responseCode != HttpURLConnection.HTTP_OK) return null
            val manifest = connection.inputStream.bufferedReader().use {
                json.decodeFromString<UpdateManifest>(it.readText())
            }
            if (!isNewerVersion(currentVersion, manifest.version)) return null
            AppUpdate(
                tag = manifest.tag,
                version = manifest.version,
                name = manifest.name.ifBlank { "Net Speed Indicator " + manifest.version },
                notes = manifest.description.toReleaseNotes(),
                downloadUrl = manifest.download_url,
                fileName = manifest.download_url.substringAfterLast('/'),
                size = manifest.size,
                digest = "sha256:" + manifest.sha256.removePrefix("sha256:"),
                releaseDate = manifest.published_at?.let(::formatReleaseDate)
            )
        } finally {
            connection.disconnect()
        }
    }

    private fun loadRelease(currentVersion: String, endpoint: String): AppUpdate? {
        val connection = openMetadataConnection(endpoint)
        return try {
            if (connection.responseCode != HttpURLConnection.HTTP_OK) return null
            val release = connection.inputStream.bufferedReader().use {
                json.decodeFromString<GithubRelease>(it.readText())
            }
            release.toAppUpdate(currentVersion)
        } finally {
            connection.disconnect()
        }
    }

    private fun GithubRelease.toAppUpdate(currentVersion: String): AppUpdate? {
        if (draft) return null
        val version = tagName.removePrefix("v").trim()
        if (!isNewerVersion(currentVersion, version)) return null

        val asset = assets.firstOrNull {
            it.name.endsWith(".apk", ignoreCase = true) &&
                it.contentType.equals(
                    "application/vnd.android.package-archive",
                    ignoreCase = true
                ) &&
                it.digest?.startsWith("sha256:", ignoreCase = true) == true
        } ?: return null

        return AppUpdate(
            tag = tagName,
            version = version,
            name = name.ifBlank { "Net Speed Indicator " + version },
            notes = body.toReleaseNotes(),
            downloadUrl = asset.downloadUrl,
            fileName = asset.name,
            size = asset.size,
            digest = asset.digest.orEmpty(),
            releaseDate = publishedAt?.let(::formatReleaseDate)
        )
    }

    private fun openMetadataConnection(url: String): HttpURLConnection =
        (URL(url).openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            connectTimeout = 10_000
            readTimeout = 15_000
            instanceFollowRedirects = true
            setRequestProperty("Accept", "application/vnd.github+json")
            setRequestProperty("User-Agent", "SpeedIndicator")
            setRequestProperty("X-GitHub-Api-Version", "2026-03-10")
        }

    suspend fun resolveDownloadUrl(browserDownloadUrl: String): String =
        withContext(Dispatchers.IO) {
            val direct = URL(browserDownloadUrl)
            val path = direct.path.trim('/').split('/')
            if (path.size < 5 || path[2] != "releases" || path[3] != "download") {
                return@withContext browserDownloadUrl
            }

            val owner = path[0]
            val repository = path[1]
            val tag = URLDecoder.decode(path[4], StandardCharsets.UTF_8.name())
            val fileName = URLDecoder.decode(path.last(), StandardCharsets.UTF_8.name())
            val endpoint =
                "https://api.github.com/repos/" + owner + "/" + repository +
                    "/releases/tags/" + encodePath(tag)
            val connection = openMetadataConnection(endpoint)
            try {
                if (connection.responseCode != HttpURLConnection.HTTP_OK) {
                    return@withContext browserDownloadUrl
                }
                val release = connection.inputStream.bufferedReader().use {
                    json.decodeFromString<GithubRelease>(it.readText())
                }
                release.assets.firstOrNull { it.name == fileName }?.apiUrl
                    ?: browserDownloadUrl
            } finally {
                connection.disconnect()
            }
        }

    private fun encodePath(value: String): String =
        value.split('/').joinToString("/") {
            java.net.URLEncoder.encode(it, StandardCharsets.UTF_8.name())
                .replace("+", "%20")
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
        val digest = prefs.getString(KEY_DIGEST, null)
            ?.takeIf { it.startsWith("sha256:", ignoreCase = true) }
            ?: return null
        val releaseDate = prefs.getString(KEY_RELEASE_DATE, null)
        return AppUpdate(
            tag,
            version,
            name,
            notes,
            url,
            file,
            prefs.getLong(KEY_SIZE, 0),
            digest,
            releaseDate
        )
    }


    suspend fun getValidatedPendingUpdate(context: Context): AppUpdate? =
        withContext(Dispatchers.IO) {
            val pending = getPendingUpdate(context) ?: return@withContext null
            try {
                val currentVersion = currentVersion(context)
                val manifestConnection = openMetadataConnection(MANIFEST_URL)
                val manifestResult = try {
                    if (manifestConnection.responseCode == HttpURLConnection.HTTP_OK) {
                        val manifest = manifestConnection.inputStream.bufferedReader().use {
                            json.decodeFromString<UpdateManifest>(it.readText())
                        }
                        if (!isNewerVersion(currentVersion, manifest.version)) {
                            clearPendingUpdate(context)
                            return@withContext null
                        }
                        AppUpdate(
                            tag = manifest.tag,
                            version = manifest.version,
                            name = manifest.name.ifBlank { "Net Speed Indicator " + manifest.version },
                            notes = manifest.description.toReleaseNotes(),
                            downloadUrl = manifest.download_url,
                            fileName = manifest.download_url.substringAfterLast('/'),
                            size = manifest.size,
                            digest = "sha256:" + manifest.sha256.removePrefix("sha256:"),
                            releaseDate = manifest.published_at?.let(::formatReleaseDate)
                        )
                    } else {
                        null
                    }
                } finally {
                    manifestConnection.disconnect()
                }
                if (manifestResult != null) {
                    savePendingUpdate(context, manifestResult)
                    return@withContext manifestResult
                }

                val releaseConnection = openMetadataConnection(LATEST_API_URL)
                try {
                    when (releaseConnection.responseCode) {
                        HttpURLConnection.HTTP_OK -> {
                            val release = releaseConnection.inputStream.bufferedReader().use {
                                json.decodeFromString<GithubRelease>(it.readText())
                            }
                            val latest = release.toAppUpdate(currentVersion)
                            if (latest == null) {
                                clearPendingUpdate(context)
                                null
                            } else {
                                savePendingUpdate(context, latest)
                                latest
                            }
                        }
                        HttpURLConnection.HTTP_NOT_FOUND -> {
                            clearPendingUpdate(context)
                            null
                        }
                        else -> pending
                    }
                } finally {
                    releaseConnection.disconnect()
                }
            } catch (_: Exception) {
                pending
            }
        }

    fun getDownloadedUpdate(context: Context): AppUpdate? {
        val update = getPendingUpdate(context) ?: return null
        val apk = File(File(context.filesDir, "updates"), update.fileName)
        return update.takeIf {
            apk.isFile &&
                apk.length() > 0L &&
                UpdateVerifier.verifyDigest(apk, update.digest)
        }
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
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getString(KEY_NOTIFIED, null) == tag

    fun markNotified(context: Context, tag: String) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .putString(KEY_NOTIFIED, tag)
            .apply()
    }

    fun enqueuePeriodicCheck(context: Context) {
        val constraints = Constraints.Builder()
            .setRequiredNetworkType(NetworkType.CONNECTED)
            .setRequiresBatteryNotLow(true)
            .build()
        val request = PeriodicWorkRequestBuilder<UpdateCheckWorker>(
            24,
            TimeUnit.HOURS
        )
            .setConstraints(constraints)
            .build()
        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            CHECK_WORK,
            ExistingPeriodicWorkPolicy.UPDATE,
            request
        )
    }

    fun enqueueDownload(context: Context, update: AppUpdate? = null) {
        val target = update ?: getPendingUpdate(context) ?: return
        val dataBuilder = Data.Builder()
            .putString(UpdateDownloadWorker.KEY_TAG, target.tag)
            .putString(UpdateDownloadWorker.KEY_URL, target.downloadUrl)
            .putString(UpdateDownloadWorker.KEY_FILE, target.fileName)
            .putString(UpdateDownloadWorker.KEY_DIGEST, target.digest)
            .putLong(UpdateDownloadWorker.KEY_SIZE, target.size)
        val request = OneTimeWorkRequestBuilder<UpdateDownloadWorker>()
            .setInputData(dataBuilder.build())
            .build()
        WorkManager.getInstance(context).enqueueUniqueWork(
            "update_download_" + target.tag,
            ExistingWorkPolicy.KEEP,
            request
        )
    }

    fun getDownloadProgress(context: Context, tag: String): DownloadProgress? {
        val work = WorkManager.getInstance(context)
            .getWorkInfosForUniqueWork("update_download_" + tag)
            .get()
            .firstOrNull() ?: return null
        val downloaded = work.progress.getLong(UpdateDownloadWorker.PROGRESS_DOWNLOADED, 0L)
        val total = work.progress.getLong(UpdateDownloadWorker.PROGRESS_TOTAL, 0L)
        val percent = work.progress.getInt(UpdateDownloadWorker.PROGRESS_PERCENT, 0)
        return when (work.state) {
            WorkInfo.State.SUCCEEDED -> DownloadProgress(downloaded, total, 100, isFinished = true)
            WorkInfo.State.FAILED -> DownloadProgress(downloaded, total, percent, isFailed = true)
            WorkInfo.State.CANCELLED -> null
            else -> DownloadProgress(downloaded, total, percent)
        }
    }

    fun cancelDownload(context: Context, tag: String) {
        WorkManager.getInstance(context).cancelUniqueWork("update_download_" + tag)
    }

    private fun currentVersion(context: Context): String = context.packageManager
        .getPackageInfo(context.packageName, 0)
        .versionName
        .orEmpty()

    private fun isNewerVersion(current: String, latest: String): Boolean {
        val currentParts = versionParts(current)
        val latestParts = versionParts(latest)
        val count = maxOf(currentParts.size, latestParts.size)
        for (index in 0 until count) {
            val currentPart = currentParts.getOrElse(index) { 0 }
            val latestPart = latestParts.getOrElse(index) { 0 }
            if (latestPart != currentPart) return latestPart > currentPart
        }
        return false
    }

    private fun versionParts(value: String): List<Int> = value.removePrefix("v").split(".")
        .map { it.takeWhile(Char::isDigit).toIntOrNull() ?: 0 }

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
