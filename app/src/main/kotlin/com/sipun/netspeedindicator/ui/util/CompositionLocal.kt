package com.sipun.netspeedindicator.ui.util

import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.navigation.NavHostController

val LocalNavController = staticCompositionLocalOf<NavHostController> { error("NavHostController must be provided") }
val LocalSnackBarHostState = staticCompositionLocalOf<SnackbarHostState> { error("SnackBarHostState must be provided") }