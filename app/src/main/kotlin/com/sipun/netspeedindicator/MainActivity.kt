package com.sipun.netspeedindicator

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.os.PowerManager
import android.provider.Settings
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
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
import com.sipun.netspeedindicator.core.util.PermissionUtils
import com.sipun.netspeedindicator.data.preferences.AccentGradient
import com.sipun.netspeedindicator.data.preferences.AccentStyle
import com.sipun.netspeedindicator.data.preferences.PreferenceManager
import com.sipun.netspeedindicator.ui.components.UpdateFlowDialog
import com.sipun.netspeedindicator.ui.navigation.AppNavigation
import com.sipun.netspeedindicator.ui.navigation.ScreenRoute
import com.sipun.netspeedindicator.ui.theme.NetSpeedIndicatorTheme
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
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
    private var waitingForBatteryOptimizationSettings = false
    private var waitingForUsageAccessSettings = false

    private val notificationPermissionLauncher =
        registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
            if (isGranted) {
                startMonitoringIfEnabled()
            } else {
                Toast.makeText(this, R.string.notification_permission_required, Toast.LENGTH_LONG).show()
            }
            requestBatteryOptimization()
        }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            window.isNavigationBarContrastEnforced = false
        }
        UpdateNotificationHelper.createChannel(this)
        handleUpdateIntent(intent)

        lifecycleScope.launch {
            val onboardingCompleted = preferenceManager.onboardingSeen.first()
            val notificationGranted = Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU ||
                ContextCompat.checkSelfPermission(
                    this@MainActivity,
                    Manifest.permission.POST_NOTIFICATIONS
                ) == PackageManager.PERMISSION_GRANTED

            if (notificationGranted) {
                startMonitoringIfEnabled()
            } else if (onboardingCompleted) {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }

        setContent {
            val appTheme by preferenceManager.appTheme.collectAsState(initial = 0)
            val dynamicColor by preferenceManager.dynamicColor.collectAsState(initial = false)
            val pureBlackTheme by preferenceManager.pureBlackTheme.collectAsState(initial = false)
            val onboardingSeen by preferenceManager.onboardingSeen.collectAsState(initial = null)
            val customColor by preferenceManager.customColor.collectAsState(initial = 0xFF42A5F5.toInt())
            val accentStyle by preferenceManager.accentStyle.collectAsState(initial = AccentStyle.SOLID)
            val accentGradient by preferenceManager.accentGradient.collectAsState(initial = AccentGradient.SUNSET)
            val customGradientStart by preferenceManager.customGradientStart.collectAsState(initial = 0xFFE92DE2.toInt())
            val customGradientEnd by preferenceManager.customGradientEnd.collectAsState(initial = 0xFF4A00E0.toInt())
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
                customColor = customColor,
                accentStyle = accentStyle,
                accentGradient = accentGradient,
                customGradientStart = customGradientStart,
                customGradientEnd = customGradientEnd
            ) {
                onboardingSeen?.let { seen ->
                    AppNavigation(
                        rememberNavController(),
                        remember { SnackbarHostState() },
                        if (seen) ScreenRoute.Main else ScreenRoute.Onboarding,
                        onOnboardingFinished = {
                            lifecycleScope.launch {
                                preferenceManager.setOnboardingSeen(true)
                                requestNotificationPermission()
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

    override fun onResume() {
        super.onResume()
        if (waitingForBatteryOptimizationSettings) {
            waitingForBatteryOptimizationSettings = false
            requestUsageAccess()
        } else if (waitingForUsageAccessSettings) {
            waitingForUsageAccessSettings = false
            if (!PermissionUtils.hasUsageStatsPermission(this)) {
                Toast.makeText(this, R.string.usage_access_required, Toast.LENGTH_LONG).show()
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

    private fun requestNotificationPermission() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(
                this,
                Manifest.permission.POST_NOTIFICATIONS
            ) != PackageManager.PERMISSION_GRANTED
        ) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        } else {
            startMonitoringIfEnabled()
            requestBatteryOptimization()
        }
    }

    private fun requestBatteryOptimization() {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.M) {
            requestUsageAccess()
            return
        }

        val powerManager = getSystemService(POWER_SERVICE) as PowerManager
        if (powerManager.isIgnoringBatteryOptimizations(packageName)) {
            requestUsageAccess()
            return
        }

        val requestIntent = Intent(Settings.ACTION_REQUEST_IGNORE_BATTERY_OPTIMIZATIONS).apply {
            data = Uri.parse("package:$packageName")
        }
        waitingForBatteryOptimizationSettings = true
        try {
            startActivity(requestIntent)
        } catch (_: Exception) {
            try {
                startActivity(Intent(Settings.ACTION_IGNORE_BATTERY_OPTIMIZATION_SETTINGS))
            } catch (_: Exception) {
                waitingForBatteryOptimizationSettings = false
                requestUsageAccess()
            }
        }
    }

    private fun requestUsageAccess() {
        if (PermissionUtils.hasUsageStatsPermission(this)) return

        waitingForUsageAccessSettings = true
        try {
            PermissionUtils.openUsageAccessSettings(this)
        } catch (_: Exception) {
            waitingForUsageAccessSettings = false
            Toast.makeText(this, R.string.usage_access_required, Toast.LENGTH_LONG).show()
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
