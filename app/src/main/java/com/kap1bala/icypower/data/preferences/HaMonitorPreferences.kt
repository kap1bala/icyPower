package com.kap1bala.icypower.data.preferences

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

/**
 * Threshold defaults for the Home Assistant battery monitor.
 *
 * Per `feat.md §1.2`: warning default 20%, danger default 10%. Per-device
 * overrides are explicitly deferred to a follow-up PR — v1 ships these
 * two numbers only, both process-global.
 *
 * Storage lives in [icyPowerDataStore] alongside the other HA config
 * (URL, token). Keys are namespaced under `ha.*`.
 */
class HaMonitorPreferences(
    private val dataStore: DataStore<Preferences>,
) {
    /** Default 20 — battery% strictly less than this becomes "Warning". */
    val warningThreshold: Flow<Int> = dataStore.data.map {
        it[KEY_WARNING] ?: DEFAULT_WARNING
    }

    /** Default 10 — battery% strictly less than this becomes "Danger". */
    val dangerThreshold: Flow<Int> = dataStore.data.map {
        it[KEY_DANGER] ?: DEFAULT_DANGER
    }

    suspend fun setThresholds(warning: Int, danger: Int) {
        require(warning in 1..100 && danger in 1..100) {
            "Thresholds must be in 1..100 (got warning=$warning, danger=$danger)"
        }
        require(danger < warning) {
            "Danger threshold ($danger) must be strictly less than warning ($warning)"
        }
        dataStore.edit { prefs ->
            prefs[KEY_WARNING] = warning
            prefs[KEY_DANGER] = danger
        }
    }

    private companion object {
        const val DEFAULT_WARNING = 20
        const val DEFAULT_DANGER = 10
        val KEY_WARNING = intPreferencesKey("ha.warning_threshold")
        val KEY_DANGER = intPreferencesKey("ha.danger_threshold")
    }
}