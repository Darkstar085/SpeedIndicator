package com.sipun.netspeedindicator

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.Image
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.res.stringResource
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.toBitmap
import androidx.navigation.compose.rememberNavController
import com.sipun.netspeedindicator.core.service.NetworkMonitorScheduler
import com.sipun.netspeedindicator.core.service.SpeedMonitorService
import com.sipun.netspeedindicator.core.update.UpdateManager
import com.sipun.netspeedindicator.core.update.UpdateNotificationHelper
import com.sipun.netspeedindicator.core.update.UpdateInstaller
import com.sipun.netspeedindicator.core.update.AppUpdate
import com.sipun.netspeedindicator.core.update.DownloadProgress
import com.sipun.netspeedindicator.data.preferences.PreferenceManager
import com.sipun.netspeedindicator.ui.components.DownloadProgressDialog
import com.sipun.netspeedindicator.ui.components.UpdateDialog
import com.sipun.netspeedindicator.ui.navigation.AppNavigation
import com.sipun.netspeedindicator.ui.navigation.ScreenRoute
import com.sipun.netspeedindicator.ui.theme.NetSpeedIndicatorTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    @Inject lateinit var preferenceManager: PreferenceManager
    @Inject lateinit var updateInstaller: UpdateInstaller

    private val downloadRequested = mutableStateOf(false)
    private val updateNotificationRequested = mutableStateOf(false)

    private val notificationPermissionLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
        if (isGranted) startMonitoringIfEnabled() else Toast.makeText(this, R.string.notification_permission_required, Toast.LENGTH_LONG).show()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        UpdateNotificationHelper.createChannel(this)
        handleUpdateIntent(intent)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        } else {
            startMonitoringIfEnabled()
        }

        setContent {
            val appTheme by preferenceManager.appTheme.collectAsState(initial = 0)
            val dynamicColor by preferenceManager.dynamicColor.collectAsState(initial = true)
            val pureBlackTheme by preferenceManager.pureBlackTheme.collectAsState(initial = false)
            var pendingUpdate by remember { mutableStateOf<AppUpdate?>(null) }
            var downloadedUpdate by remember { mutableStateOf<AppUpdate?>(null) }
            var downloadingUpdate by remember { mutableStateOf(false) }
            var downloadProgress by remember { mutableStateOf<DownloadProgress?>(null) }
            val shouldStartDownload by downloadRequested
            val shouldShowUpdate by updateNotificationRequested
            val darkTheme = when (appTheme) { 1 -> false; 2 -> true; else -> isSystemInDarkTheme() }
            val pureBlackEnabled = pureBlackTheme && darkTheme
            val appIcon = remember { packageManager.getApplicationIcon(applicationInfo).toBitmap().asImageBitmap() }

            LaunchedEffect(darkTheme, pureBlackTheme) {
                if (!darkTheme && pureBlackTheme) preferenceManager.setPureBlackTheme(false)
            }

            LaunchedEffect(Unit) {
                val storedUpdate = withContext(Dispatchers.IO) { UpdateManager.getPendingUpdate(this@MainActivity) }
                if (storedUpdate != null) {
                    pendingUpdate = storedUpdate
                    downloadedUpdate = withContext(Dispatchers.IO) { UpdateManager.getDownloadedUpdate(this@MainActivity) }
                    withContext(Dispatchers.IO) { UpdateManager.getDownloadProgress(this@MainActivity, storedUpdate.tag) }?.let {
                        if (!it.isFinished && !it.isFailed) { downloadProgress = it; downloadingUpdate = true }
                    }
                } else {
                    val latestUpdate = withContext(Dispatchers.IO) { UpdateManager.findLatestUpdate(this@MainActivity) }
                    if (latestUpdate != null) {
                        UpdateManager.savePendingUpdate(this@MainActivity, latestUpdate)
                        UpdateManager.markNotified(this@MainActivity, latestUpdate.tag)
                        pendingUpdate = latestUpdate
                    }
                }
            }

            LaunchedEffect(shouldShowUpdate, pendingUpdate?.tag) {
                if (!shouldShowUpdate) return@LaunchedEffect
                if (pendingUpdate == null) {
                    val update = withContext(Dispatchers.IO) { UpdateManager.getPendingUpdate(this@MainActivity) }
                        ?: withContext(Dispatchers.IO) { UpdateManager.findLatestUpdate(this@MainActivity) }
                    update?.let {
                        UpdateManager.savePendingUpdate(this@MainActivity, it)
                        pendingUpdate = it
                    }
                }
                updateNotificationRequested.value = false
            }

            LaunchedEffect(shouldStartDownload, pendingUpdate?.tag) {
                if (!shouldStartDownload) return@LaunchedEffect
                val update = pendingUpdate ?: return@LaunchedEffect
                downloadingUpdate = true
                UpdateManager.enqueueDownload(this@MainActivity, update)
                downloadRequested.value = false
            }

            LaunchedEffect(pendingUpdate?.tag, downloadingUpdate) {
                val update = pendingUpdate ?: return@LaunchedEffect
                if (!downloadingUpdate) return@LaunchedEffect
                while (downloadingUpdate) {
                    val status = withContext(Dispatchers.IO) {
                        UpdateManager.getDownloadProgress(this@MainActivity, update.tag)
                    }
                    if (status != null) {
                        downloadProgress = status
                        if (status.isFinished) {
                            downloadedUpdate = withContext(Dispatchers.IO) {
                                UpdateManager.getDownloadedUpdate(this@MainActivity)
                            }
                            downloadingUpdate = false
                            downloadProgress = null
                            break
                        }
                        if (status.isFailed) {
                            downloadingUpdate = false
                            downloadProgress = null
                            Toast.makeText(this@MainActivity, R.string.update_download_failed, Toast.LENGTH_LONG).show()
                            break
                        }
                    }
                    delay(250)
                }
            }

            NetSpeedIndicatorTheme(darkTheme = darkTheme, dynamicColor = dynamicColor, pureBlack = pureBlackEnabled) {
                AppNavigation(rememberNavController(), remember { SnackbarHostState() }, ScreenRoute.Main)
                pendingUpdate?.let { update ->
                    if (downloadingUpdate && downloadProgress != null) {
                        DownloadProgressDialog(
                            update = update,
                            appIcon = appIcon,
                            progress = downloadProgress!!,
                            onCancel = {
                                UpdateManager.cancelDownload(this@MainActivity, update.tag)
                                downloadingUpdate = false
                                downloadProgress = null
                            }
                        )
                    } else {
                        UpdateDialog(
                            update = update,
                            appIcon = appIcon,
                            isDownloaded = downloadedUpdate?.tag == update.tag,
                            onDownload = { downloadRequested.value = true },
                            onInstall = { installDownloadedUpdate() },
                            onDismiss = {
                                pendingUpdate = null
                                downloadedUpdate = null
                                downloadingUpdate = false
                                downloadProgress = null
                            }
                        )
                    }
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleUpdateIntent(intent)
    }

    private fun handleUpdateIntent(intent: Intent?) {
        when (intent?.action) {
            UpdateManager.ACTION_DOWNLOAD_UPDATE -> downloadRequested.value = true
            UpdateManager.ACTION_SHOW_UPDATE -> updateNotificationRequested.value = true
            UpdateManager.ACTION_INSTALL_UPDATE -> installDownloadedUpdate()
        }
    }

    private fun installDownloadedUpdate() {
        when (updateInstaller.installDownloadedUpdate()) {
            UpdateInstaller.Result.Success -> Unit
            UpdateInstaller.Result.FileMissing ->
                Toast.makeText(this, R.string.update_file_missing, Toast.LENGTH_LONG).show()
            UpdateInstaller.Result.PermissionRequired -> {
                Toast.makeText(this, R.string.allow_install_updates, Toast.LENGTH_LONG).show()
                updateInstaller.openInstallPermissionSettings()
            }
            UpdateInstaller.Result.Failed ->
                Toast.makeText(this, R.string.update_install_failed, Toast.LENGTH_LONG).show()
        }
    }

    private fun startMonitoringIfEnabled() {
        if (preferenceManager.isMonitoringEnabled()) {
            NetworkMonitorScheduler.schedule(this)
        }
    }
}
