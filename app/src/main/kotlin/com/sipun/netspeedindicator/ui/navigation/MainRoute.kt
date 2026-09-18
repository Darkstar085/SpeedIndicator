package com.sipun.netspeedindicator.ui.navigation

import kotlinx.serialization.Serializable

@Serializable
sealed class MainRoute {
    @Serializable data object Home : MainRoute()
    @Serializable data object History : MainRoute()
    @Serializable data object Settings : MainRoute()
}
