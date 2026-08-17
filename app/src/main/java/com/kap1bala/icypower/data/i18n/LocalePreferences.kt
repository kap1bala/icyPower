package com.kap1bala.icypower.data.i18n

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Persists the user's [AppLocale] choice (see `feat.md` §5).
 *
 * Storage shape: a single string preference. The value is the persisted
 * [AppLocale.tag] — including the empty string for [AppLocale.System].
 *
 * Read by [com.kap1bala.icypower.MainActivity.attachBaseContext] (synchronously,
 * via a `runBlocking` boundary at app boot) to seed the Activity's
 * Configuration so `R.string.*` lookups pick the right resource bundle.
 */
class LocalePreferences(
    private val dataStore: DataStore<Preferences>,
) {
    val tag: Flow<String?> = dataStore.data.map { prefs -> prefs[KEY_TAG] }

    /** Persist [locale].tag; use the empty string for [AppLocale.System]. */
    suspend fun setLocale(locale: AppLocale) {
        dataStore.edit { prefs -> prefs[KEY_TAG] = locale.tag }
    }

    private companion object {
        val KEY_TAG = stringPreferencesKey("app.locale")
    }
}
