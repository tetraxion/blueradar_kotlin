package com.example.blueradar.ui.navigation

/**
 * Sealed class untuk route navigation
 */
sealed class Screen(val route: String) {
    data object Splash : Screen("splash")
    data object Dashboard : Screen("dashboard")
    data object Radar : Screen("radar/{macAddress}") {
        fun createRoute(macAddress: String) = "radar/$macAddress"
    }
    data object History : Screen("history")
}
