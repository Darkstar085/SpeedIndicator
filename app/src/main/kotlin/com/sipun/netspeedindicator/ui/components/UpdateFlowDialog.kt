package com.sipun.netspeedindicator.ui.components

import android.widget.Toast
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.ImageBitmap
import androidx.compose.ui.platform.LocalContext
import com.sipun.netspeedindicator.R
import com.sipun.netspeedindicator.core.update.AppUpdate
import com.sipun.netspeedindicator.core.update.DownloadProgress
import com.sipun.netspeedindicator.core.update.UpdateInstaller
import com.sipun.netspeedindicator.core.update.UpdateManager
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import kotlinx.coroutines.Dispatchers

private enum class UpdatePhase { AVAILABLE, DOWNLOADING, DOWNLOADED }

/**
 * Self-contained update flow: shows the update prompt, a live download with a
 * progress bar, and an inline Install button — no notification round-trips.
 * Shared by MainActivity (automatic prompt) and Settings (manual check).
 */
@Composable
fun UpdateFlowDialog(
    update: AppUpdate,
    appIcon: ImageBitmap,
    onDismiss: () -> Unit
) {
    val context = LocalContext.current
    var phase by remember(update.tag) { mutableStateOf(UpdatePhase.AVAILABLE) }
    var progress by remember(update.tag) { mutableStateOf<DownloadProgress?>(null) }
    var initialized by remember(update.tag) { mutableStateOf(false) }

    // Restore any in-flight/finished state for this update when the dialog opens.
    LaunchedEffect(update.tag) {
        val existing = UpdateManager.getDownloadProgress(context, update.tag)
        val downloaded = withContext(Dispatchers.IO) { UpdateManager.getDownloadedUpdate(context) }
        when {
            downloaded?.tag == update.tag -> phase = UpdatePhase.DOWNLOADED
            existing != null && !existing.isFinished && !existing.isFailed -> {
                phase = UpdatePhase.DOWNLOADING
                progress = existing
            }
        }
        initialized = true
    }

    // Poll progress while downloading; settle into DOWNLOADED or back to AVAILABLE.
    LaunchedEffect(phase, update.tag) {
        if (phase != UpdatePhase.DOWNLOADING) return@LaunchedEffect
        while (true) {
            val status = withContext(Dispatchers.IO) {
                UpdateManager.getDownloadProgress(context, update.tag)
            }
            if (status == null) {
                phase = UpdatePhase.AVAILABLE
                progress = null
                break
            }
            progress = status
            if (status.isFinished) {
                phase = UpdatePhase.DOWNLOADED
                break
            }
            if (status.isFailed) {
                phase = UpdatePhase.AVAILABLE
                progress = null
                Toast.makeText(context, R.string.update_download_failed, Toast.LENGTH_LONG).show()
                break
            }
            delay(250L)
        }
    }

    val startDownload: () -> Unit = {
        phase = UpdatePhase.DOWNLOADING
        UpdateManager.enqueueDownload(context, update)
    }

    val install: () -> Unit = {
        val installer = UpdateInstaller(context.applicationContext)
        when (installer.installDownloadedUpdate()) {
            UpdateInstaller.Result.Success -> onDismiss()
            UpdateInstaller.Result.PermissionRequired -> {
                Toast.makeText(context, R.string.allow_install_updates, Toast.LENGTH_LONG).show()
                installer.openInstallPermissionSettings()
            }
            UpdateInstaller.Result.FileMissing ->
                Toast.makeText(context, R.string.update_file_missing, Toast.LENGTH_LONG).show()

            UpdateInstaller.Result.Failed ->
                Toast.makeText(context, R.string.update_install_failed, Toast.LENGTH_LONG).show()
        }
    }

    when (phase) {
        UpdatePhase.DOWNLOADING -> DownloadProgressDialog(
            update = update,
            appIcon = appIcon,
            progress = progress ?: DownloadProgress(
                downloadedBytes = 0L,
                totalBytes = update.size,
                percent = 0,
                isFinished = false,
                isFailed = false
            ),
            onCancel = {
                UpdateManager.cancelDownload(context, update.tag)
                phase = UpdatePhase.AVAILABLE
                progress = null
            }
        )

        else -> UpdateDialog(
            update = update,
            appIcon = appIcon,
            isDownloaded = phase == UpdatePhase.DOWNLOADED,
            isDownloading = false,
            onDownload = startDownload,
            onInstall = install,
            onDismiss = onDismiss
        )
    }
}
