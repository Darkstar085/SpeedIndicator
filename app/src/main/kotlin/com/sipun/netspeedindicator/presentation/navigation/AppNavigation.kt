package com.sipun.netspeedindicator.presentation.navigation

import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.add
import androidx.compose.foundation.layout.ime
import androidx.compose.foundation.layout.navigationBars
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import com.sipun.netspeedindicator.presentation.MainScreen
import com.sipun.netspeedindicator.presentation.util.LocalNavController
import com.sipun.netspeedindicator.presentation.util.LocalSnackBarHostState
import com.sipun.netspeedindicator.presentation.util.appNavComposable

@Composable
fun AppNavigation(navController: NavHostController, snackBarHostState: SnackbarHostState, initialRoute: ScreenRoute) {
    Scaffold(contentWindowInsets = WindowInsets(0), snackbarHost = { SnackbarHost(hostState = snackBarHostState, modifier = Modifier.padding(bottom = 20.dp).windowInsetsPadding(WindowInsets.ime.add(WindowInsets.navigationBars))) { Snackbar(snackbarData = it) } }) {
        CompositionLocalProvider(LocalSnackBarHostState provides snackBarHostState, LocalNavController provides navController) {
            AppNavHost(modifier = Modifier.padding(it), navController = navController, initialRoute = initialRoute)
        }
    }
}

@Composable
fun AppNavHost(modifier: Modifier = Modifier, navController: NavHostController, initialRoute: ScreenRoute) {
    NavHost(modifier = modifier, navController = navController, startDestination = initialRoute) {
        appNavComposable<ScreenRoute.Main> { MainScreen() }
    }
}
