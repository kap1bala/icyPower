package com.kap1bala.icypower.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.kap1bala.icypower.ui.cycle.CycleDeviceEditScreen
import com.kap1bala.icypower.ui.cycle.CycleDeviceListScreen
import com.kap1bala.icypower.ui.home.HomeScreen
import com.kap1bala.icypower.ui.settings.AppearanceScreen
import com.kap1bala.icypower.ui.settings.LanguageScreen
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
                onOpenCycleDevices = {
                    navController.navigate(Destinations.SETTINGS_CYCLE)
                },
                onOpenLanguage = {
                    navController.navigate(Destinations.SETTINGS_LANGUAGE)
                },
            )
        }

        composable(Destinations.APPEARANCE) {
            AppearanceScreen(
                onBack = { navController.popBackStack() },
            )
        }

        composable(Destinations.SETTINGS_LANGUAGE) {
            LanguageScreen(
                onBack = { navController.popBackStack() },
            )
        }

        composable(Destinations.SETTINGS_CYCLE) {
            CycleDeviceListScreen(
                onBack = { navController.popBackStack() },
                onAdd = {
                    navController.navigate(Destinations.cycleDeviceEdit(null))
                },
                onEdit = { id ->
                    navController.navigate(Destinations.cycleDeviceEdit(id))
                },
            )
        }

        composable(
            route = Destinations.SETTINGS_CYCLE_EDIT_PATTERN,
            arguments = listOf(
                navArgument(Destinations.ARG_CYCLE_DEVICE_ID) {
                    type = NavType.StringType
                    nullable = true
                    defaultValue = null
                },
            ),
        ) { entry ->
            val id = entry.arguments?.getString(Destinations.ARG_CYCLE_DEVICE_ID)
            CycleDeviceEditScreen(
                deviceId = id,
                onBack = { navController.popBackStack() },
                onDone = { navController.popBackStack() },
            )
        }
    }
}
