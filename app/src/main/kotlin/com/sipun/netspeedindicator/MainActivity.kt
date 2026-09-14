package com.sipun.netspeedindicator

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
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
import androidx.core.content.FileProvider
import androidx.core.graphics.drawable.toBitmap
import androidx.navigation.compose.rememberNavController
import com.sipun.netspeedindicator.core.service.NetworkMonitorScheduler
import com.sipun.netspeedindicator.core.service.SpeedMonitorService
import com.sipun.netspeedindicator.core.update.UpdateManager
import com.sipun.netspeedindicator.core.update.UpdateNotificationHelper
import com.sipun.netspeedindicator.core.update.AppUpdate
import com.sipun.netspeedindicator.data.preferences.PreferenceManager
import com.sipun.netspeedindicator.ui.components.UpdateDialog
import com.sipun.netspeedindicator.ui.navigation.AppNavigation
import com.sipun.netspeedindicator.ui.navigation.ScreenRoute
import com.sipun.netspeedindicator.ui.theme.NetSpeedIndicatorTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    @Inject lateinit var preferenceManager: PreferenceManager

    private val notificationPermissionLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
        if (isGranted) startMonitoringIfEnabled() else Toast.makeText(this, R.string.notification_permission_required, Toast.LENGTH_LONG).show()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
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
            val darkTheme = when (appTheme) { 1 -> false; 2 -> true; else -> isSystemInDarkTheme() }
            val pureBlackEnabled = pureBlackTheme && darkTheme
            val appIcon = remember { packageManager.getApplicationIcon(applicationInfo).toBitmap().asImageBitmap() }
            val updateIntentAction = intent?.action

            LaunchedEffect(darkTheme, pureBlackTheme) {
                if (!darkTheme && pureBlackTheme) preferenceManager.setPureBlackTheme(false)
            }

            LaunchedEffect(updateIntentAction) {
                if (updateIntentAction == UpdateManager.ACTION_DOWNLOAD_UPDATE || updateIntentAction == UpdateManager.ACTION_INSTALL_UPDATE) return@LaunchedEffect
                val update = withContext(Dispatchers.IO) { UpdateManager.findLatestUpdate(this@MainActivity) }
                if (update != null) {
                    UpdateManager.savePendingUpdate(this@MainActivity, update)
                    UpdateManager.markNotified(this@MainActivity, update.tag)
                    pendingUpdate = update
                }
            }

            NetSpeedIndicatorTheme(darkTheme = darkTheme, dynamicColor = dynamicColor, pureBlack = pureBlackEnabled) {
                AppNavigation(rememberNavController(), remember { SnackbarHostState() }, ScreenRoute.Main)
                pendingUpdate?.let { update ->
                    UpdateDialog(
                        update = update,
                        appIcon = appIcon,
                        onDownload = {
                            UpdateManager.enqueueDownload(this@MainActivity, update)
                            pendingUpdate = null
                            Toast.makeText(this@MainActivity, R.string.update_download_started, Toast.LENGTH_SHORT).show()
                        },
                        onDismiss = { pendingUpdate = null }
                    )
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
            UpdateManager.ACTION_DOWNLOAD_UPDATE -> {
                UpdateManager.enqueueDownload(this, UpdateManager.getPendingUpdate(this))
                Toast.makeText(this, R.string.update_download_started, Toast.LENGTH_SHORT).show()
            }
            UpdateManager.ACTION_INSTALL_UPDATE -> installDownloadedUpdate()
        }
    }

    private fun installDownloadedUpdate() {
        val updateDir = File(filesDir, "updates")
        val apk = updateDir.listFiles()
            ?.filter { it.isFile && it.extension.equals("apk", ignoreCase = true) }
            ?.maxByOrNull { it.lastModified() }

        if (apk == null) {
            Toast.makeText(this, R.string.update_file_missing, Toast.LENGTH_LONG).show()
            return
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O && !packageManager.canRequestPackageInstalls()) {
            Toast.makeText(this, R.string.allow_install_updates, Toast.LENGTH_LONG).show()
            startActivity(Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES, Uri.parse("package:$packageName")))
            return
        }

        val apkUri = FileProvider.getUriForFile(this, "$packageName.files", apk)
        val installIntent = Intent(Intent.ACTION_INSTALL_PACKAGE).apply {
            data = apkUri
            type = "application/vnd.android.package-archive"
            addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION)
        }
        try {
            startActivity(installIntent)
        } catch (_: Exception) {
            Toast.makeText(this, R.string.update_install_failed, Toast.LENGTH_LONG).show()
        }
    }

    private fun startMonitoringIfEnabled() {
        if (preferenceManager.isMonitoringEnabled()) {
            NetworkMonitorScheduler.schedule(this)
            ContextCompat.startForegroundService(this, Intent(this, SpeedMonitorService::class.java))
        }
    }
}
