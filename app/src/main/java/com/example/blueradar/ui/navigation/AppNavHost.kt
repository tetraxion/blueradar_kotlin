package com.example.blueradar.ui.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.navArgument
import com.example.blueradar.ui.screen.dashboard.DashboardScreen
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

        // Screen 1: Dashboard Scanner
        composable(Screen.Dashboard.route) {
            DashboardScreen(
                onNavigateToRadar = { macAddress ->
                    navController.navigate(Screen.Radar.createRoute(macAddress))
                },
                onNavigateToHistory = {
                    navController.navigate(Screen.History.route)
                }
            )
        }

        // Screen 2: Radar View
        composable(
            route = Screen.Radar.route,
            arguments = listOf(
                navArgument("macAddress") { type = NavType.StringType }
            )
        ) { backStackEntry ->
            val macAddress = backStackEntry.arguments?.getString("macAddress") ?: ""
            RadarScreen(
                macAddress = macAddress,
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }

        // Screen 3: History Log
        composable(Screen.History.route) {
            HistoryScreen(
                onNavigateBack = {
                    navController.popBackStack()
                }
            )
        }
    }
}
