package com.sipun.netspeedindicator.core.update

import android.content.Context
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import java.io.File
import java.io.FileOutputStream
import java.net.HttpURLConnection
import java.net.URL
import java.security.MessageDigest

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
    }

    override suspend fun doWork(): Result {
        val url = inputData.getString(KEY_URL) ?: return Result.failure()
        val fileName = inputData.getString(KEY_FILE) ?: return Result.failure()
        val tag = inputData.getString(KEY_TAG) ?: return Result.failure()
        val expectedDigest = inputData.getString(KEY_DIGEST)

        return try {
            UpdateNotificationHelper.showDownloadProgress(applicationContext, fileName)
            val updateDir = File(applicationContext.filesDir, "updates").apply { mkdirs() }
            val apk = File(updateDir, fileName)
            download(url, apk)

            if (!expectedDigest.isNullOrBlank() && !verifyDigest(apk, expectedDigest)) {
                apk.delete()
                throw IllegalStateException("Downloaded update failed integrity verification")
            }

            UpdateNotificationHelper.showUpdateReady(applicationContext, tag, apk)
            Result.success()
        } catch (_: Exception) {
            UpdateNotificationHelper.showDownloadFailed(applicationContext)
            Result.retry()
        }
    }

    private fun download(url: String, destination: File) {
        val connection = (URL(url).openConnection() as HttpURLConnection).apply {
            requestMethod = "GET"
            connectTimeout = 15_000
            readTimeout = 30_000
            instanceFollowRedirects = true
            setRequestProperty("Accept", "application/vnd.android.package-archive")
        }
        try {
            if (connection.responseCode !in 200..299) throw IllegalStateException("Download failed: ${connection.responseCode}")
            connection.inputStream.use { input ->
                FileOutputStream(destination).use { output ->
                    val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
                    while (true) {
                        val read = input.read(buffer)
                        if (read < 0) break
                        output.write(buffer, 0, read)
                    }
                }
            }
        } finally {
            connection.disconnect()
        }
    }

    private fun verifyDigest(file: File, expected: String): Boolean {
        val expectedHash = expected.substringAfter("sha256:", expected).lowercase()
        val digest = MessageDigest.getInstance("SHA-256")
        file.inputStream().use { input ->
            val buffer = ByteArray(DEFAULT_BUFFER_SIZE)
            while (true) {
                val read = input.read(buffer)
                if (read < 0) break
                digest.update(buffer, 0, read)
            }
        }
        return digest.digest().joinToString("") { "%02x".format(it) } == expectedHash
    }
}
