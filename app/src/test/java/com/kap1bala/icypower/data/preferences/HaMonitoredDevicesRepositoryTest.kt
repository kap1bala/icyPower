package com.kap1bala.icypower.data.preferences

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

/**
 * Unit tests for [HaMonitoredDevicesRepository].
 *
 * Pinned behaviours:
 *  - Empty store → empty set, `isFirstRun = true`.
 *  - `replace` overwrites the whole set.
 *  - `toggle` adds when `enabled = true`, removes when `false`.
 *  - `markInitialized` flips `isFirstRun` to false and stays there.
 *  - `snapshot()` reads the current value without subscribing.
 */
class HaMonitoredDevicesRepositoryTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    private lateinit var dataStoreFile: File
    private lateinit var scope: CoroutineScope
    private lateinit var dataStore: DataStore<Preferences>
    private lateinit var repo: HaMonitoredDevicesRepository

    @Before
    fun setUp() {
        dataStoreFile = tempFolder.newFile("ha_monitored.preferences_pb")
        scope = CoroutineScope(Job())
        dataStore = PreferenceDataStoreFactory.create(scope = scope) { dataStoreFile }
        repo = HaMonitoredDevicesRepository(dataStore)
    }

    @After
    fun tearDown() {
        runBlocking { scope.coroutineContext[Job]?.cancelAndJoin() }
    }

    @Test
    fun monitoredIds_emptyWhenNothingPersisted() = runBlocking {
        assertEquals(emptySet<String>(), repo.monitoredIds.first())
    }

    @Test
    fun isFirstRun_trueInitially() = runBlocking {
        assertTrue(repo.isFirstRun.first())
    }

    // ─── replace ─────────────────────────────────────────────────────────────

    @Test
    fun replace_setsTheWholeSet() = runBlocking {
        repo.replace(setOf("sensor.a", "sensor.b", "sensor.c"))
        assertEquals(
            setOf("sensor.a", "sensor.b", "sensor.c"),
            repo.monitoredIds.first(),
        )
    }

    @Test
    fun replace_emptySetClearsAll() = runBlocking {
        repo.replace(setOf("sensor.a"))
        repo.replace(emptySet())
        assertTrue(repo.monitoredIds.first().isEmpty())
    }

    @Test
    fun replace_overwritesPreviousValue() = runBlocking {
        repo.replace(setOf("sensor.a", "sensor.b"))
        repo.replace(setOf("sensor.x"))
        assertEquals(setOf("sensor.x"), repo.monitoredIds.first())
    }

    // ─── toggle ──────────────────────────────────────────────────────────────

    @Test
    fun toggle_addsWhenEnabled() = runBlocking {
        repo.toggle("sensor.a", enabled = true)
        assertEquals(setOf("sensor.a"), repo.monitoredIds.first())
    }

    @Test
    fun toggle_addsToExistingSet() = runBlocking {
        repo.replace(setOf("sensor.a"))
        repo.toggle("sensor.b", enabled = true)
        assertEquals(setOf("sensor.a", "sensor.b"), repo.monitoredIds.first())
    }

    @Test
    fun toggle_isIdempotentWhenEnabled() = runBlocking {
        repo.toggle("sensor.a", enabled = true)
        repo.toggle("sensor.a", enabled = true)
        assertEquals(setOf("sensor.a"), repo.monitoredIds.first())
    }

    @Test
    fun toggle_removesWhenDisabled() = runBlocking {
        repo.replace(setOf("sensor.a", "sensor.b"))
        repo.toggle("sensor.a", enabled = false)
        assertEquals(setOf("sensor.b"), repo.monitoredIds.first())
    }

    @Test
    fun toggle_removeMissingEntryIsNoOp() = runBlocking {
        repo.replace(setOf("sensor.a"))
        repo.toggle("sensor.b", enabled = false)
        assertEquals(setOf("sensor.a"), repo.monitoredIds.first())
    }

    // ─── markInitialized ─────────────────────────────────────────────────────

    @Test
    fun markInitialized_flipsIsFirstRunToFalse() = runBlocking {
        assertTrue(repo.isFirstRun.first())
        repo.markInitialized()
        assertFalse(repo.isFirstRun.first())
    }

    @Test
    fun markInitialized_doesNotMutateMonitoredSet() = runBlocking {
        repo.replace(setOf("sensor.a"))
        repo.markInitialized()
        assertEquals(setOf("sensor.a"), repo.monitoredIds.first())
    }

    // ─── snapshot ────────────────────────────────────────────────────────────

    @Test
    fun snapshot_returnsCurrentValue() = runBlocking {
        repo.replace(setOf("sensor.a", "sensor.b"))
        assertEquals(setOf("sensor.a", "sensor.b"), repo.snapshot())
    }

    @Test
    fun snapshot_emptyWhenNothingPersisted() = runBlocking {
        assertEquals(emptySet<String>(), repo.snapshot())
    }
}