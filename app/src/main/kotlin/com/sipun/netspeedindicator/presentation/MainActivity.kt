package com.sipun.netspeedindicator.presentation

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
import com.sipun.netspeedindicator.core.util.PermissionUtils
import com.sipun.netspeedindicator.data.preferences.PreferenceManager
import com.sipun.netspeedindicator.presentation.navigation.AppNavigation
import com.sipun.netspeedindicator.presentation.navigation.ScreenRoute
import com.sipun.netspeedindicator.presentation.theme.NetSpeedIndicatorTheme
import dagger.hilt.android.AndroidEntryPoint
import javax.inject.Inject

@AndroidEntryPoint
class MainActivity : ComponentActivity() {

    @Inject
    lateinit var preferenceManager: PreferenceManager

    private var usageAccessPromptedThisLaunch = false

    private val notificationPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)

        ContextCompat.startForegroundService(this, Intent(this, SpeedMonitorService::class.java))

        setContent {
            val appTheme by preferenceManager.appTheme.collectAsState(initial = 0)
            val dynamicColor by preferenceManager.dynamicColor.collectAsState(initial = true)
            val darkTheme = when (appTheme) {
                1 -> false
                2 -> true
                else -> isSystemInDarkTheme()
            }

            NetSpeedIndicatorTheme(darkTheme = darkTheme, dynamicColor = dynamicColor) {
                AppNavigation(
                    navController = rememberNavController(),
                    snackBarHostState = remember { SnackbarHostState() },
                    initialRoute = ScreenRoute.Main
                )
            }
        }
    }

    override fun onResume() {
        super.onResume()

        if (!PermissionUtils.hasUsageStatsPermission(this)) {
            if (!usageAccessPromptedThisLaunch) {
                usageAccessPromptedThisLaunch = true
                PermissionUtils.openUsageAccessSettings(this)
            }
            return
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            ContextCompat.checkSelfPermission(this, Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
        ) {
            notificationPermissionLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
        }
    }
}
