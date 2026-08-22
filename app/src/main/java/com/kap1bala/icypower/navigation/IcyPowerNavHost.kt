package com.kap1bala.icypower.navigation

import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.runtime.Composable
import androidx.navigation.NavHostController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument
import com.kap1bala.icypower.ui.cycle.CycleDeviceEditScreen
import com.kap1bala.icypower.ui.cycle.CycleDeviceListScreen
import com.kap1bala.icypower.ui.ha.HaDeviceSelectionScreen
import com.kap1bala.icypower.ui.home.HomeScreen
import com.kap1bala.icypower.ui.settings.AppearanceScreen
import com.kap1bala.icypower.ui.settings.HaSettingsScreen
import com.kap1bala.icypower.ui.settings.LanguageScreen
import com.kap1bala.icypower.ui.settings.SettingsScreen

/**
 * Root navigation graph.
 *
 * `home` is the start destination. Back stack follows the natural Material
 * pattern — each child screen renders its own TopAppBar with a back button,
 * wired to `navController.popBackStack()` via the `onBack` callbacks.
 */

/** Nav cross-fade duration (ms). 120ms ≈ "snappy" — see [IcyPowerNavHost]. */
private const val FAST_NAV_MS = 120
@Composable
fun IcyPowerNavHost(
    navController: NavHostController = rememberNavController(),
) {
    NavHost(
        navController = navController,
        startDestination = Destinations.HOME,
        // Settings / sub-pages transition via the Material3 default fade
        // (~220ms) which reads as sluggish when drilling into a form.
        // Drop it to a snappy 120ms cross-fade — fast enough to feel
        // instant, still a transition so the destination change is legible.
        enterTransition = { fadeIn(animationSpec = tween(FAST_NAV_MS)) },
        exitTransition = { fadeOut(animationSpec = tween(FAST_NAV_MS)) },
        popEnterTransition = { fadeIn(animationSpec = tween(FAST_NAV_MS)) },
        popExitTransition = { fadeOut(animationSpec = tween(FAST_NAV_MS)) },
    ) {
        composable(Destinations.HOME) {
            HomeScreen(
                onOpenSettings = {
                    navController.navigate(Destinations.SETTINGS)
                },
                onChooseHaDevices = {
                    navController.navigate(Destinations.SETTINGS_HA_DEVICES)
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
                onOpenHa = {
                    navController.navigate(Destinations.SETTINGS_HA)
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

        composable(Destinations.SETTINGS_HA) {
            HaSettingsScreen(
                onBack = { navController.popBackStack() },
                onOpenDevices = {
                    navController.navigate(Destinations.SETTINGS_HA_DEVICES)
                },
            )
        }

        composable(Destinations.SETTINGS_HA_DEVICES) {
            HaDeviceSelectionScreen(
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
