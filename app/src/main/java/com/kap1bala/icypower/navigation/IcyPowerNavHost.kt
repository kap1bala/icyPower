package com.kap1bala.icypower.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.kap1bala.icypower.ui.home.HomeScreen
import com.kap1bala.icypower.ui.settings.AppearanceScreen
import com.kap1bala.icypower.ui.settings.SettingsScreen

/**
 * Root navigation graph.
 *
 * `home` is the start destination. Back stack follows the natural Material
 * pattern — each child screen renders its own TopAppBar with a back button,
 * wired to `navController.popBackStack()` via the `onBack` callbacks.
 */
@Composable
fun IcyPowerNavHost(
    navController: NavHostController = rememberNavController(),
) {
    NavHost(
        navController = navController,
        startDestination = Destinations.HOME,
    ) {
        composable(Destinations.HOME) {
            HomeScreen(
                onOpenSettings = {
                    navController.navigate(Destinations.SETTINGS)
                },
            )
        }

        composable(Destinations.SETTINGS) {
            SettingsScreen(
                onBack = { navController.popBackStack() },
                onOpenAppearance = {
                    navController.navigate(Destinations.APPEARANCE)
                },
            )
        }

        composable(Destinations.APPEARANCE) {
            AppearanceScreen(
                onBack = { navController.popBackStack() },
            )
        }
    }
}
