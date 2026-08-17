package com.kap1bala.icypower.data.preferences

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.kap1bala.icypower.ui.theme.ThemeMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Persists the user's [ThemeMode] choice.
 *
 * Storage rules (matches feat.md §5.7):
 *   - We persist the literal "system" / "light" / "dark", NOT a resolved
 *     boolean. That way, when the user picks `system` and later changes the
 *     OS theme, the app follows automatically without us having to
 *     re-resolve.
 *   - The key is namespaced (`theme.mode`) so future preferences can share
 *     the same DataStore without collisions.
 */
class ThemePreferences(
    private val dataStore: DataStore<Preferences>,
) {
    val themeMode: Flow<ThemeMode> = dataStore.data.map { prefs ->
        ThemeMode.fromStorageKey(prefs[KEY_THEME_MODE])
    }

    suspend fun setThemeMode(mode: ThemeMode) {
        dataStore.edit { prefs ->
            prefs[KEY_THEME_MODE] = mode.name
        }
    }

    private companion object {
        val KEY_THEME_MODE = stringPreferencesKey("theme.mode")
    }
}
