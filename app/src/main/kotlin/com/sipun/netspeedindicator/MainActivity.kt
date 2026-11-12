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
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.isSystemInDarkTheme
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.toBitmap
import androidx.navigation.compose.rememberNavController
import androidx.lifecycle.lifecycleScope
import com.sipun.netspeedindicator.core.service.NetworkMonitorScheduler
import com.sipun.netspeedindicator.core.service.SpeedMonitorService
import com.sipun.netspeedindicator.core.update.AppUpdate
import com.sipun.netspeedindicator.core.update.UpdateInstaller
import com.sipun.netspeedindicator.core.update.UpdateManager
import com.sipun.netspeedindicator.core.update.UpdateNotificationHelper
import com.sipun.netspeedindicator.data.preferences.PreferenceManager
import com.sipun.netspeedindicator.ui.components.UpdateFlowDialog
import com.sipun.netspeedindicator.ui.navigation.AppNavigation
import com.sipun.netspeedindicator.ui.navigation.ScreenRoute
import com.sipun.netspeedindicator.ui.theme.NetSpeedIndicatorTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    @Inject
    lateinit var preferenceManager: PreferenceManager

    @Inject
    lateinit var updateInstaller: UpdateInstaller

    private val updateFlowRequested = MutableStateFlow<AppUpdate?>(null)

    private val notificationPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
            if (isGranted) startMonitoringIfEnabled() else Toast.makeText(
                this,
                R.string.notification_permission_required,
                Toast.LENGTH_LONG
            ).show()
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            window.isNavigationBarContrastEnforced = false
        }
        UpdateNotificationHelper.createChannel(this)
        handleUpdateIntent(intent)

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.POST_NOTIFICATIONS
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        } else {
            startMonitoringIfEnabled()
        }

        setContent {
            val appTheme by preferenceManager.appTheme.collectAsState(initial = 0)
            val dynamicColor by preferenceManager.dynamicColor.collectAsState(initial = true)
            val pureBlackTheme by preferenceManager.pureBlackTheme.collectAsState(initial = false)
            val onboardingSeen by preferenceManager.onboardingSeen.collectAsState(initial = null)
            val accent by preferenceManager.accent.collectAsState(initial = 0)
            var pendingUpdate by remember { mutableStateOf<AppUpdate?>(null) }
            val darkTheme = when (appTheme) {
                1 -> false; 2 -> true; else -> isSystemInDarkTheme()
            }
            val pureBlackEnabled = pureBlackTheme && darkTheme
            val appIcon = remember {
                packageManager.getApplicationIcon(applicationInfo).toBitmap().asImageBitmap()
            }

            LaunchedEffect(darkTheme, pureBlackTheme) {
                if (!darkTheme && pureBlackTheme) preferenceManager.setPureBlackTheme(false)
            }

            LaunchedEffect(Unit) {
                val storedUpdate =
                    withContext(Dispatchers.IO) { UpdateManager.getValidatedPendingUpdate(this@MainActivity) }
                if (storedUpdate != null) {
                    pendingUpdate = storedUpdate
                } else {
                    val latestUpdate =
                        withContext(Dispatchers.IO) { UpdateManager.findLatestUpdate(this@MainActivity) }
                    if (latestUpdate != null) {
                        UpdateManager.savePendingUpdate(this@MainActivity, latestUpdate)
                        pendingUpdate = latestUpdate
                    }
                }
                launch {
                    updateFlowRequested.collect { update -> if (update != null) pendingUpdate = update }
                }
            }

            NetSpeedIndicatorTheme(
                darkTheme = darkTheme,
                dynamicColor = dynamicColor,
                pureBlack = pureBlackEnabled,
                accent = accent
            ) {
                onboardingSeen?.let { seen ->
                    AppNavigation(
                        rememberNavController(),
                        remember { SnackbarHostState() },
                        if (seen) ScreenRoute.Main else ScreenRoute.Onboarding,
                        onOnboardingFinished = {
                            lifecycleScope.launch {
                                preferenceManager.setOnboardingSeen(true)
                            }
                        }
                    )
                } ?: Box(
                    modifier = Modifier.fillMaxSize()
                )
                pendingUpdate?.let { update ->
                    UpdateFlowDialog(
                        update = update,
                        appIcon = appIcon,
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
            UpdateManager.ACTION_DOWNLOAD_UPDATE -> startDownloadOfPendingUpdate()
            UpdateManager.ACTION_SHOW_UPDATE -> showUpdateFlow()
            UpdateManager.ACTION_INSTALL_UPDATE -> installDownloadedUpdate()
        }
    }

    /** Notification "Download" action: kick off the download, then show the in-app flow. */
    private fun startDownloadOfPendingUpdate() {
        lifecycleScope.launch {
            val update = UpdateManager.getValidatedPendingUpdate(this@MainActivity)
                ?: UpdateManager.findLatestUpdate(this@MainActivity)
                ?: return@launch
            UpdateManager.savePendingUpdate(this@MainActivity, update)
            UpdateManager.enqueueDownload(this@MainActivity, update)
            updateFlowRequested.value = update
        }
    }

    /** Notification "Show" action: surface the in-app update flow. */
    private fun showUpdateFlow() {
        lifecycleScope.launch {
            val update = UpdateManager.getValidatedPendingUpdate(this@MainActivity)
                ?: UpdateManager.findLatestUpdate(this@MainActivity)
                ?: return@launch
            UpdateManager.savePendingUpdate(this@MainActivity, update)
            updateFlowRequested.value = update
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
            ContextCompat.startForegroundService(
                this,
                Intent(this, SpeedMonitorService::class.java)
            )
        }
    }
}
