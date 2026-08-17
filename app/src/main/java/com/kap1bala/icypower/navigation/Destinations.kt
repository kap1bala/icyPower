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
    const val SETTINGS_CYCLE = "settings/cycle"
    const val SETTINGS_CYCLE_EDIT = "settings/cycle/edit"
    const val SETTINGS_LANGUAGE = "settings/language"
    const val SETTINGS_HA = "settings/ha"

    /** Argument key for the optional device id on the edit route. */
    const val ARG_CYCLE_DEVICE_ID = "id"

    /** Route pattern for the edit screen; bind with [cycleDeviceEdit]. */
    const val SETTINGS_CYCLE_EDIT_PATTERN =
        "$SETTINGS_CYCLE_EDIT?$ARG_CYCLE_DEVICE_ID={$ARG_CYCLE_DEVICE_ID}"

    /** Build a concrete edit route. `id = null` → create new. */
    fun cycleDeviceEdit(id: String?): String =
        if (id == null) SETTINGS_CYCLE_EDIT
        else "$SETTINGS_CYCLE_EDIT?$ARG_CYCLE_DEVICE_ID=$id"
}
