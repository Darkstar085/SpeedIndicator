package com.sipun.netspeedindicator.ui.screens.main

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.Scaffold
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.rememberNavController
import com.sipun.netspeedindicator.ui.components.AppBottomNavigation
import com.sipun.netspeedindicator.ui.navigation.ScreenRoute
import com.sipun.netspeedindicator.ui.screens.main.history.HistoryScreen
import com.sipun.netspeedindicator.ui.screens.main.home.HomeScreen
import com.sipun.netspeedindicator.ui.screens.main.settings.SettingsScreen
import com.sipun.netspeedindicator.ui.util.appNavComposable

@Composable
fun MainScreen() {
    val navController = rememberNavController()
    MainScreenContent(navController = navController)
}

@Composable
fun MainScreenContent(navController: NavHostController) {
    Scaffold(modifier = Modifier.fillMaxSize().statusBarsPadding(), bottomBar = { AppBottomNavigation(navController = navController) }) { paddingValues ->
        Column(modifier = Modifier.fillMaxSize().padding(paddingValues), horizontalAlignment = Alignment.CenterHorizontally) {
            NavHost(modifier = Modifier.weight(1f), navController = navController, startDestination = ScreenRoute.Home) {
                appNavComposable<ScreenRoute.Home> { HomeScreen() }
                appNavComposable<ScreenRoute.History> { HistoryScreen() }
                appNavComposable<ScreenRoute.Settings> { SettingsScreen() }
            }
        }
    }
}