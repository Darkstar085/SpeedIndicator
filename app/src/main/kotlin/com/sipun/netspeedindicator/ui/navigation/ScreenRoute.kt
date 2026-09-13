package com.sipun.netspeedindicator.ui.navigation

import kotlinx.serialization.Serializable

@Serializable
sealed class ScreenRoute {
    @Serializable data object Onboarding : ScreenRoute()
    @Serializable data object Main : ScreenRoute()
    @Serializable data object Home : ScreenRoute()
    @Serializable data object History : ScreenRoute()
    @Serializable data object Settings : ScreenRoute()
}