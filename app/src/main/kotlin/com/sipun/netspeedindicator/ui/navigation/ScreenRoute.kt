package com.sipun.netspeedindicator.ui.navigation

import kotlinx.serialization.Serializable

@Serializable
sealed class ScreenRoute {
    @Serializable data object Onboarding : ScreenRoute()
    @Serializable data object Main : ScreenRoute()
}