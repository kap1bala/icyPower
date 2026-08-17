package com.kap1bala.icypower.data.cycle

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.serialization.SerializationException
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.Json

/**
 * Persistence for [CycleDevice].
 *
 * Storage shape: a single string preference holding a JSON array. v1 has
 * tiny data (typically <20 devices, <1KB serialized), so this is the
 * simplest viable approach. If the list ever grows large enough that
 * full re-serialization per write hurts, switch to per-id keys.
 *
 * Concurrency: a [Mutex] serializes writes so two concurrent edits don't
 * race and lose data.
 */
class CycleDeviceRepository(
    private val dataStore: DataStore<Preferences>,
) {
    private val mutex = Mutex()
    private val json = Json { ignoreUnknownKeys = true }
    private val listSerializer = ListSerializer(CycleDevice.serializer())

    /** Hot stream of all stored devices, decoded from JSON. */
    val devices: Flow<List<CycleDevice>> = dataStore.data.map { prefs ->
        decode(prefs[KEY_JSON])
    }

    /**
     * Insert or update. Caller is responsible for setting `id`, `createdAt`,
     * `lastChargedAt`, `updatedAt` correctly — this method just stamps
     * `updatedAt = now()` on update.
     */
    suspend fun upsert(device: CycleDevice) = mutex.withLock {
        val now = System.currentTimeMillis()
        val current = decodeBlocking()
        val updated = if (current.any { it.id == device.id }) {
            current.map { if (it.id == device.id) device.copy(updatedAt = now) else it }
        } else {
            current + device
        }
        writeBlocking(updated)
    }

    suspend fun remove(id: String) = mutex.withLock {
        val current = decodeBlocking()
        writeBlocking(current.filterNot { it.id == id })
    }

    /**
     * Reset [CycleDevice.lastChargedAt] to now — the action invoked by the
     * home card's "已充电" button.
     */
    suspend fun resetLastChargedAt(id: String) = mutex.withLock {
        val now = System.currentTimeMillis()
        val current = decodeBlocking()
        val updated = current.map {
            if (it.id == id) it.copy(lastChargedAt = now, updatedAt = now) else it
        }
        writeBlocking(updated)
    }

    /** One-shot lookup; null if not found. */
    suspend fun findById(id: String): CycleDevice? {
        return devices.first().firstOrNull { it.id == id }
    }

    // ─── helpers (must be called with mutex held) ──────────────────────

    private suspend fun decodeBlocking(): List<CycleDevice> =
        decode(dataStore.data.first()[KEY_JSON])

    private suspend fun writeBlocking(list: List<CycleDevice>) {
        dataStore.edit { prefs ->
            prefs[KEY_JSON] = json.encodeToString(listSerializer, list)
        }
    }

    private fun decode(raw: String?): List<CycleDevice> {
        if (raw.isNullOrBlank()) return emptyList()
        return try {
            json.decodeFromString(listSerializer, raw)
        } catch (_: SerializationException) {
            // Corrupt or schema-incompatible payload — treat as empty rather
            // than crashing. A real product would surface this as a UI banner
            // and back up the bad blob for diagnostics.
            emptyList()
        }
    }

    private companion object {
        val KEY_JSON = stringPreferencesKey("cycle_devices_json")
    }
}
