package com.example.blueradar.ui.navigation

/**
 * Sealed class untuk route navigation
 */
sealed class Screen(val route: String) {
    data object Splash : Screen("splash")
    data object Dashboard : Screen("dashboard")
    data object Radar : Screen("radar")
    data object DeviceDetail : Screen("device_detail/{macAddress}") {
        fun createRoute(macAddress: String) = "device_detail/$macAddress"
    }
    data object History : Screen("history")
}
