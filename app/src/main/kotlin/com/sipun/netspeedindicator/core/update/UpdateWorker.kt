package com.sipun.netspeedindicator.core.update

import android.content.Context
import android.content.pm.PackageManager
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import kotlinx.coroutines.delay
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL

class UpdateCheckWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {
    override suspend fun doWork(): Result {
        return try {
            val update = UpdateManager.findLatestUpdate(applicationContext) ?: return Result.success()
            UpdateManager.savePendingUpdate(applicationContext, update)
            if (!UpdateManager.wasNotified(applicationContext, update.tag)) {
                UpdateNotificationHelper.showUpdateAvailable(applicationContext, update)
                UpdateManager.markNotified(applicationContext, update.tag)
            }
            Result.success()
        } catch (_: Exception) {
            Result.retry()
        }
    }
}

class UpdateDownloadWorker(
    context: Context,
    params: WorkerParameters
) : CoroutineWorker(context, params) {
    companion object {
        const val KEY_TAG = "tag"
        const val KEY_URL = "url"
        const val KEY_FILE = "file"
        const val KEY_DIGEST = "digest"
        const val KEY_SIZE = "size"
        const val PROGRESS_DOWNLOADED = "downloaded_bytes"
        const val PROGRESS_TOTAL = "total_bytes"
        const val PROGRESS_PERCENT = "percent"
    }

    override suspend fun doWork(): Result {
        val url = inputData.getString(KEY_URL) ?: return Result.failure()
        val fileName = inputData.getString(KEY_FILE) ?: return Result.failure()
        val tag = inputData.getString(KEY_TAG) ?: return Result.failure()
        val expectedDigest = inputData.getString(KEY_DIGEST)
            ?.takeIf { it.startsWith("sha256:", ignoreCase = true) }
            ?: return Result.failure()

        val updateDir = File(applicationContext.filesDir, "updates").apply { mkdirs() }
        val apk = File(updateDir, fileName)
        val partial = File(updateDir, fileName + ".part")

        return try {
            partial.delete()
            downloadWithFallback(url, partial, inputData.getLong(KEY_SIZE, 0L), fileName)

            if (!UpdateVerifier.verifyDigest(partial, expectedDigest)) {
                throw IntegrityException("Downloaded update failed SHA-256 verification")
            }

            if (!verifyPackageSignature(partial)) {
                throw IntegrityException("Downloaded update is not signed by the installed application")
            }

            if (!partial.renameTo(apk)) {
                throw IllegalStateException("Failed to finalize downloaded update")
            }

            UpdateNotificationHelper.showUpdateReady(applicationContext, tag, apk)
            Result.success()
        } catch (_: IntegrityException) {
            partial.delete()
            apk.delete()
            UpdateNotificationHelper.showDownloadFailed(applicationContext)
            Result.failure()
        } catch (_: Exception) {
            partial.delete()
            apk.delete()
            UpdateNotificationHelper.showDownloadFailed(applicationContext)
            Result.retry()
        }
    }

    private suspend fun downloadWithFallback(
        url: String,
        destination: File,
        expectedTotal: Long,
        fileName: String
    ) {
        var lastError: Exception? = null

        repeat(3) { attempt ->
            try {
                download(url, destination, expectedTotal, fileName)
                return
            } catch (error: Exception) {
                lastError = error
                if (attempt < 2) delay(1_000L * (attempt + 1))
            }
        }

        val fallbackUrl = UpdateManager.resolveDownloadUrl(url)
        if (fallbackUrl != url) {
            destination.delete()
            download(fallbackUrl, destination, expectedTotal, fileName)
            return
        }

        throw lastError ?: IllegalStateException("Download failed")
    }

    private suspend fun download(
        url: String,
        destination: File,
        expectedTotal: Long,
        fileName: String
    ) {
        val connection = (URL(url).openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            connectTimeout = 15_000
            readTimeout = 30_000
            instanceFollowRedirects = true
            setRequestProperty("Accept", "application/octet-stream")
            setRequestProperty(
                "User-Agent",
                "SpeedIndicator/" + applicationContext.packageManager
                    .getPackageInfo(applicationContext.packageName, 0).versionName
            )
            setRequestProperty("X-GitHub-Api-Version", "2026-03-10")
        }

        try {
            val responseCode = connection.responseCode
            if (responseCode !in 200..299) {
                throw DownloadHttpException(responseCode)
            }

            val total = connection.contentLengthLong.takeIf { it > 0L } ?: expectedTotal
            var downloaded = 0L
            var lastReportedBytes = 0L
            var lastReportedAt = 0L

            connection.inputStream.use { input ->
                FileOutputStream(destination).use { output ->
                    val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                    while (true) {
                        val read = input.read(buffer)
                        if (read < 0) break
                        output.write(buffer, 0, read)
                        downloaded += read

                        val now = System.currentTimeMillis()
                        if (
                            downloaded - lastReportedBytes >= 256 * 1024L ||
                            now - lastReportedAt >= 250L
                        ) {
                            val percent = if (total > 0L) {
                                ((downloaded * 100L) / total).coerceIn(0L, 100L).toInt()
                            } else {
                                0
                            }
                            setProgress(
                                androidx.work.Data.Builder()
                                    .putLong(PROGRESS_DOWNLOADED, downloaded)
                                    .putLong(PROGRESS_TOTAL, total)
                                    .putInt(PROGRESS_PERCENT, percent)
                                    .build()
                            )
                            UpdateNotificationHelper.showDownloadProgress(
                                applicationContext,
                                fileName,
                                downloaded,
                                total,
                                percent
                            )
                            lastReportedBytes = downloaded
                            lastReportedAt = now
                        }
                    }
                }
            }

            setProgress(
                androidx.work.Data.Builder()
                    .putLong(PROGRESS_DOWNLOADED, downloaded)
                    .putLong(PROGRESS_TOTAL, total)
                    .putInt(PROGRESS_PERCENT, 100)
                    .build()
            )
            UpdateNotificationHelper.showDownloadProgress(
                applicationContext,
                fileName,
                downloaded,
                total,
                100
            )
        } finally {
            connection.disconnect()
        }
    }

    private fun verifyPackageSignature(apk: File): Boolean {
        val packageManager = applicationContext.packageManager
        val installedInfo = packageManager.getPackageInfo(
            applicationContext.packageName,
            PackageManager.GET_SIGNING_CERTIFICATES
        )
        val archiveInfo = packageManager.getPackageArchiveInfo(
            apk.absolutePath,
            PackageManager.GET_SIGNING_CERTIFICATES
        ) ?: return false

        val installedSigningInfo = installedInfo.signingInfo ?: return false
        val archiveSigningInfo = archiveInfo.signingInfo ?: return false

        if (archiveInfo.packageName != applicationContext.packageName) return false

        val installedSignatures = installedSigningInfo.apkContentsSigners
        val archiveSignatures = archiveSigningInfo.apkContentsSigners
        if (installedSignatures.size != archiveSignatures.size) return false
        return installedSignatures.zip(archiveSignatures).all { (installed, archive) ->
            installed.toByteArray().contentEquals(archive.toByteArray())
        }
    }

    private class IntegrityException(message: String) : Exception(message)

    private class DownloadHttpException(code: Int) :
        Exception("Download failed: HTTP " + code)
}
