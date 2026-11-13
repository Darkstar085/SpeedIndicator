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
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.unit.dp
import androidx.navigation.NavDestination
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
import com.sipun.netspeedindicator.ui.util.LocalBottomBarVisible
import com.sipun.netspeedindicator.ui.util.appNavComposable
import com.sipun.netspeedindicator.ui.util.getDefaultEnterTransition
import com.sipun.netspeedindicator.ui.util.getDefaultExitTransition
import com.sipun.netspeedindicator.ui.util.getDefaultPopEnterTransition
import com.sipun.netspeedindicator.ui.util.getDefaultPopExitTransition
import io.github.itshedimisawi.vitro.glassBackdrop
import io.github.itshedimisawi.vitro.rememberGlassBackdrop

private val tabRoutes = listOf(
    MainRoute.Home,
    MainRoute.History,
    MainRoute.AppDataUsage,
    MainRoute.Settings
)

private fun tabIndexOf(destination: NavDestination): Int =
    tabRoutes.indexOfFirst { route -> destination.hasRoute(route::class) }.coerceAtLeast(0)

/** Forward swipes/taps slide from the right; backward ones slide in from the left. */
private val tabEnterTransition: (androidx.compose.animation.AnimatedContentTransitionScope<androidx.navigation.NavBackStackEntry>.() -> EnterTransition?) = {
    if (tabIndexOf(targetState.destination) >= tabIndexOf(initialState.destination)) {
        getDefaultEnterTransition().invoke(this)
    } else {
        getDefaultPopEnterTransition().invoke(this)
    }
}
private val tabExitTransition: (androidx.compose.animation.AnimatedContentTransitionScope<androidx.navigation.NavBackStackEntry>.() -> ExitTransition?) = {
    if (tabIndexOf(targetState.destination) >= tabIndexOf(initialState.destination)) {
        getDefaultExitTransition().invoke(this)
    } else {
        getDefaultPopExitTransition().invoke(this)
    }
}

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
    val currentTab = tabRoutes
        .indexOfFirst { route -> navBackStackEntry?.destination?.hasRoute(route::class) == true }
        .coerceAtLeast(0)
    val swipeThresholdPx = with(LocalDensity.current) { 72.dp.toPx() }

    // Tapping a tab always reveals the bar again.
    LaunchedEffect(destinationId) { bottomBarVisible.value = true }

    fun navigateToTab(index: Int) {
        val route = tabRoutes.getOrNull(index) ?: return
        // Identical options to tapping the bottom bar.
        navController.navigate(route) {
            popUpTo(navController.graph.startDestinationId) { saveState = true }
            launchSingleTop = true
            restoreState = true
        }
    }

    CompositionLocalProvider(LocalBottomBarVisible provides bottomBarVisible) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background)
                .statusBarsPadding()
                .pointerInput(currentTab) {
                    var dragX = 0f
                    detectHorizontalDragGestures(
                        onDragStart = { dragX = 0f },
                        onHorizontalDrag = { _, amount -> dragX += amount },
                        onDragEnd = {
                            // Swipe left → next tab, swipe right → previous tab.
                            if (kotlin.math.abs(dragX) >= swipeThresholdPx) {
                                if (dragX < 0 && currentTab < tabRoutes.lastIndex) {
                                    navigateToTab(currentTab + 1)
                                } else if (dragX > 0 && currentTab > 0) {
                                    navigateToTab(currentTab - 1)
                                }
                            }
                            dragX = 0f
                        },
                        onDragCancel = { dragX = 0f }
                    )
                }
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
        appNavComposable<MainRoute.Home>(
            enterTransition = tabEnterTransition,
            exitTransition = tabExitTransition,
            popEnterTransition = tabEnterTransition,
            popExitTransition = tabExitTransition
        ) { HomeScreen() }
        appNavComposable<MainRoute.History>(
            enterTransition = tabEnterTransition,
            exitTransition = tabExitTransition,
            popEnterTransition = tabEnterTransition,
            popExitTransition = tabExitTransition
        ) { HistoryScreen() }
        appNavComposable<MainRoute.AppDataUsage>(
            enterTransition = tabEnterTransition,
            exitTransition = tabExitTransition,
            popEnterTransition = tabEnterTransition,
            popExitTransition = tabExitTransition
        ) { AppDataUsageScreen() }
        appNavComposable<MainRoute.Settings>(
            enterTransition = tabEnterTransition,
            exitTransition = tabExitTransition,
            popEnterTransition = tabEnterTransition,
            popExitTransition = tabExitTransition
        ) { SettingsScreen() }
    }
}
