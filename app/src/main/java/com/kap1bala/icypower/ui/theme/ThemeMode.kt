package com.kap1bala.icypower.ui.theme

/**
 * User-selected theme mode.
 *
 * Persisted as a string in DataStore — DO NOT reorder the entries or rename
 * their `name`, or existing users will see their theme silently switch.
 */
enum class ThemeMode {
    /** Follow the OS via [androidx.compose.foundation.isSystemInDarkTheme]. */
    System,

    /** Force the light color scheme regardless of OS. */
    Light,

    /** Force the dark color scheme regardless of OS. */
    Dark;

    companion object {
        val DEFAULT: ThemeMode = System

        fun fromStorageKey(key: String?): ThemeMode =
            entries.firstOrNull { it.name == key } ?: DEFAULT
    }
}
