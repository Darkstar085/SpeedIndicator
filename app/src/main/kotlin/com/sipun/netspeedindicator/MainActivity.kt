package com.sipun.netspeedindicator

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import android.widget.Toast
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.core.content.ContextCompat
import androidx.navigation.compose.rememberNavController
import com.sipun.netspeedindicator.core.service.NetworkMonitorScheduler
import com.sipun.netspeedindicator.core.service.SpeedMonitorService
import com.sipun.netspeedindicator.data.preferences.PreferenceManager
import com.sipun.netspeedindicator.ui.navigation.AppNavigation
import com.sipun.netspeedindicator.ui.navigation.ScreenRoute
import com.sipun.netspeedindicator.ui.theme.NetSpeedIndicatorTheme
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {
    @Inject lateinit var preferenceManager: PreferenceManager

    private val notificationPermissionLauncher = registerForActivityResult(ActivityResultContracts.RequestPermission()) { isGranted ->
        if (isGranted) startMonitoringIfEnabled() else Toast.makeText(this, R.string.notification_permission_required, Toast.LENGTH_LONG).show()
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
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
            val darkTheme = when (appTheme) { 1 -> false; 2 -> true; else -> isSystemInDarkTheme() }
            val pureBlackEnabled = pureBlackTheme && darkTheme

            LaunchedEffect(darkTheme, pureBlackTheme) {
                if (!darkTheme && pureBlackTheme) preferenceManager.setPureBlackTheme(false)
            }

            NetSpeedIndicatorTheme(darkTheme = darkTheme, dynamicColor = dynamicColor, pureBlack = pureBlackEnabled) {
                AppNavigation(rememberNavController(), remember { SnackbarHostState() }, ScreenRoute.Main)
            }
        }
    }

    private fun startMonitoringIfEnabled() {
        if (preferenceManager.isMonitoringEnabled()) {
            NetworkMonitorScheduler.schedule(this)
            ContextCompat.startForegroundService(this, Intent(this, SpeedMonitorService::class.java))
        }
    }
}
