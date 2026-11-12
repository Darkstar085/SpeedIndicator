package com.sipun.netspeedindicator.ui.screens.main

import androidx.compose.foundation.background
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
import com.sipun.netspeedindicator.ui.util.LocalBottomBarVisible
import com.sipun.netspeedindicator.ui.util.appNavComposable
import io.github.itshedimisawi.vitro.glassBackdrop
import io.github.itshedimisawi.vitro.rememberGlassBackdrop

@Composable
fun MainScreen() {
    MainScreenContent(rememberNavController())
}

@Composable
fun MainScreenContent(navController: NavHostController) {
    val backdrop = rememberGlassBackdrop()
    val bottomBarVisible = remember { mutableStateOf(true) }
    val navBackStackEntry by navController.currentBackStackEntryAsState()
    val destinationId = navBackStackEntry?.destination?.id

    // Tapping a tab always reveals the bar again.
    LaunchedEffect(destinationId) { bottomBarVisible.value = true }

    CompositionLocalProvider(LocalBottomBarVisible provides bottomBarVisible) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .statusBarsPadding()
        ) {
            MainNavHost(
                modifier = Modifier
                    .fillMaxSize()
                    .glassBackdrop(backdrop),
                navController = navController
            )
            AnimatedVisibility(
                visible = bottomBarVisible.value,
                enter = slideInVertically(initialOffsetY = { it }) + fadeIn(),
                exit = slideOutVertically(targetOffsetY = { it }) + fadeOut(),
                modifier = Modifier.align(Alignment.BottomCenter)
            ) {
                AppBottomNavigation(
                    navController = navController,
                    backdrop = backdrop
                )
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
