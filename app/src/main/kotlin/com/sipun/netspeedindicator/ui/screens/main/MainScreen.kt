package com.sipun.netspeedindicator.ui.screens.main

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.rememberNavController
import com.sipun.netspeedindicator.ui.components.AppBottomNavigation
import com.sipun.netspeedindicator.ui.navigation.MainRoute
import com.sipun.netspeedindicator.ui.screens.main.appusage.AppDataUsageScreen
import com.sipun.netspeedindicator.ui.screens.main.history.HistoryScreen
import com.sipun.netspeedindicator.ui.screens.main.home.HomeScreen
import com.sipun.netspeedindicator.ui.screens.main.settings.SettingsScreen
import com.sipun.netspeedindicator.ui.util.appNavComposable

@Composable
fun MainScreen() {
    MainScreenContent(rememberNavController())
}

@Composable
fun MainScreenContent(navController: NavHostController) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .statusBarsPadding()
    ) {
        NavHost(
            modifier = Modifier.fillMaxSize(),
            navController = navController,
            startDestination = MainRoute.Home
        ) {
            appNavComposable<MainRoute.Home> { HomeScreen() }
            appNavComposable<MainRoute.History> { HistoryScreen() }
            appNavComposable<MainRoute.AppDataUsage> { AppDataUsageScreen() }
            appNavComposable<MainRoute.Settings> { SettingsScreen() }
        }

        Box(
            modifier = Modifier.fillMaxSize(),
            contentAlignment = Alignment.BottomCenter
        ) {
            AppBottomNavigation(navController)
        }
    }
}
