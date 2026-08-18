package com.kap1bala.icypower.data.preferences

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringSetPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map

/**
 * Per-user selection of Home Assistant entities that the home screen
 * should monitor. `feat.md §1.2` calls these "the list of devices the
 * user has chosen to track".
 *
 * Persistence shape:
 *   - `ha.monitored_entity_ids` → `Set<String>` of entity_ids.
 *   - `ha.monitored_initialized` → Boolean flag, set the first time the
 *     user opens the selection page (at which point we auto-fill the
 *     set with every battery-bearing entity HA reports). Used so that
 *     we only do the auto-fill-on-first-open dance once per install.
 *
 * All operations are `suspend` — DataStore prefers serialised writes.
 */
class HaMonitoredDevicesRepository(
    private val dataStore: DataStore<Preferences>,
) {
    /** Hot flow of the persisted set; empty until the user has set things up. */
    val monitoredIds: Flow<Set<String>> = dataStore.data.map { prefs ->
        prefs[KEY_MONITORED] ?: emptySet()
    }

    /** True until the selection screen has been visited once. */
    val isFirstRun: Flow<Boolean> = dataStore.data.map { it[KEY_INITIALIZED] != true }

    suspend fun replace(ids: Set<String>) {
        dataStore.edit { prefs ->
            prefs[KEY_MONITORED] = ids
        }
    }

    suspend fun toggle(entityId: String, enabled: Boolean) {
        dataStore.edit { prefs ->
            val current = prefs[KEY_MONITORED] ?: emptySet()
            prefs[KEY_MONITORED] = if (enabled) current + entityId else current - entityId
        }
    }

    suspend fun markInitialized() {
        dataStore.edit { it[KEY_INITIALIZED] = true }
    }

    /** Snapshot read for callers that need a value rather than a flow. */
    suspend fun snapshot(): Set<String> = monitoredIds.first()

    private companion object {
        val KEY_MONITORED = stringSetPreferencesKey("ha.monitored_entity_ids")
        val KEY_INITIALIZED = booleanPreferencesKey("ha.monitored_initialized")
    }
}