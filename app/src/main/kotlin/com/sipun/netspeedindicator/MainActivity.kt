package com.sipun.netspeedindicator

import android.Manifest
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.core.content.ContextCompat
import androidx.navigation.compose.rememberNavController
import com.sipun.netspeedindicator.core.service.SpeedMonitorService
import com.sipun.netspeedindicator.data.preferences.PreferenceManager
import com.sipun.netspeedindicator.ui.navigation.AppNavigation
import com.sipun.netspeedindicator.ui.navigation.ScreenRoute
import com.sipun.netspeedindicator.ui.theme.NetSpeedIndicatorTheme
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var preferenceManager: PreferenceManager

    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        // Handle permission result if needed
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        // Start monitoring service automatically
        ContextCompat.startForegroundService(this, Intent(this, SpeedMonitorService::class.java))

        // Request notification permission for Android 13+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            if (ContextCompat.checkSelfPermission(
                    this,
                    Manifest.permission.POST_NOTIFICATIONS
                ) != PackageManager.PERMISSION_GRANTED
            ) {
                notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }

        setContent {
            val appTheme by preferenceManager.appTheme.collectAsState(initial = 0)
            val dynamicColor by preferenceManager.dynamicColor.collectAsState(initial = true)

            val darkTheme = when (appTheme) {
                1 -> false // Light
                2 -> true  // Dark
                else -> isSystemInDarkTheme() // System
            }

            NetSpeedIndicatorTheme(
                darkTheme = darkTheme,
                dynamicColor = dynamicColor
            ) {
                AppNavigation(
                    navController = rememberNavController(),
                    snackBarHostState = remember { SnackbarHostState() },
                    initialRoute = ScreenRoute.Main
                )
            }
        }
    }
}