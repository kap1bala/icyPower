package com.kap1bala.icypower.navigation

/**
 * String constants for the NavHost route table.
 *
 * Routes are kept flat on purpose — once we add a real Settings graph
 * (cycle devices, HA connection, alert rules, quiet hours), we'll switch
 * to type-safe destinations or a sealed class hierarchy. v1 keeps it
 * simple.
 */
object Destinations {
    const val HOME = "home"
    const val SETTINGS = "settings"
    const val APPEARANCE = "settings/appearance"
}
