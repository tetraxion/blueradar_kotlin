package com.example.blueradar.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.example.blueradar.ui.screen.dashboard.DashboardScreen
import com.example.blueradar.ui.screen.detail.DeviceDetailScreen
import com.example.blueradar.ui.screen.history.HistoryScreen
import com.example.blueradar.ui.screen.radar.RadarScreen
import com.example.blueradar.ui.screen.splash.SplashScreen

@Composable
fun AppNavHost(
    navController: NavHostController,
    modifier: Modifier = Modifier
) {
    NavHost(
        navController = navController,
        startDestination = Screen.Splash.route,
        modifier = modifier
    ) {
        // Splash Screen
        composable(Screen.Splash.route) {
            SplashScreen(
                onSplashFinished = {
                    navController.navigate(Screen.Dashboard.route) {
                        popUpTo(Screen.Splash.route) { inclusive = true }
                    }
                }
            )
        }

        // Tab 1: Dashboard Scanner
        composable(Screen.Dashboard.route) {
            DashboardScreen(
                onNavigateToRadar = {
                    navController.navigate(Screen.Radar.route)
                },
                onNavigateToDetail = { macAddress ->
                    navController.navigate(Screen.DeviceDetail.createRoute(macAddress))
                },
                onNavigateToHistory = {
                    navController.navigate(Screen.History.route)
                }
            )
        }

        // Tab 2: 360 Spatial Radar Scope View
        composable(Screen.Radar.route) {
            RadarScreen(
                onNavigateToDetail = { macAddress ->
                    navController.navigate(Screen.DeviceDetail.createRoute(macAddress))
                },
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }

        // Tab 3: History Log
        composable(Screen.History.route) {
            HistoryScreen(
                onNavigateBack = {
                    navController.popBackStack()
                },
                onNavigateToDetail = { macAddress ->
                    navController.navigate(Screen.DeviceDetail.createRoute(macAddress))
                }
            )
        }

        // Sub-page: Single Device Detail Tracking
        composable(
            route = Screen.DeviceDetail.route,
            arguments = listOf(
                navArgument("macAddress") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val macAddress = backStackEntry.arguments?.getString("macAddress") ?: ""
            DeviceDetailScreen(
                macAddress = macAddress,
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }
    }
}
