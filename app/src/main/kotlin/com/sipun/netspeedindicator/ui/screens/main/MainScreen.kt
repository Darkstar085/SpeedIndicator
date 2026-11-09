package com.sipun.netspeedindicator.ui.screens.main

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.navigation.NavDestination.Companion.hasRoute
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.currentBackStackEntryAsState
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
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val isStaticScreen = navBackStackEntry?.destination?.let { destination ->
        destination.hasRoute(MainRoute.Home::class) || destination.hasRoute(MainRoute.History::class)
    } != false

    if (isStaticScreen) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
        ) {
            MainNavHost(modifier = Modifier.weight(1f), navController = navController)
            AppBottomNavigation(navController)
        }
    } else {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .statusBarsPadding()
        ) {
            MainNavHost(modifier = Modifier.fillMaxSize(), navController = navController)
            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.BottomCenter
            ) {
                AppBottomNavigation(navController)
            }
        }
    }
}

@Composable
private fun MainNavHost(modifier: Modifier, navController: NavHostController) {
    NavHost(
        modifier = modifier,
        navController = navController,
        startDestination = MainRoute.Home
    ) {
        appNavComposable<MainRoute.Home> { HomeScreen() }
        appNavComposable<MainRoute.History> { HistoryScreen() }
        appNavComposable<MainRoute.AppDataUsage> { AppDataUsageScreen() }
        appNavComposable<MainRoute.Settings> { SettingsScreen() }
    }
}
