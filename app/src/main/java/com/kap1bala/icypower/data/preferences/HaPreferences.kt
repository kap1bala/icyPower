package com.kap1bala.icypower.data.preferences

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Plain-text DataStore wrapper for HA connection preferences that are
 * not sensitive (i.e. the [baseUrl]). The Long-Lived Access Token lives
 * in [com.kap1bala.icypower.data.security.EncryptedSecureStorage] instead.
 *
 * Keys are namespaced under `ha.*` so they coexist with other preferences
 * in the same DataStore without collisions.
 */
class HaPreferences(
    private val dataStore: DataStore<Preferences>,
) {
    /** Emits the persisted base URL or `null` if never set / cleared. */
    val baseUrl: Flow<String?> = dataStore.data.map { prefs -> prefs[KEY_BASE_URL] }

    suspend fun setBaseUrl(url: String) {
        dataStore.edit { prefs -> prefs[KEY_BASE_URL] = url }
    }

    suspend fun clear() {
        dataStore.edit { prefs -> prefs.remove(KEY_BASE_URL) }
    }

    private companion object {
        val KEY_BASE_URL = stringPreferencesKey("ha.base_url")
    }
}
