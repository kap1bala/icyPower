package com.kap1bala.icypower.data.cycle

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

/**
 * Unit tests for [CycleDeviceRepository].
 *
 * Pinned behaviours:
 *  - Empty DataStore → empty device list.
 *  - `upsert` inserts and updates by id; `updatedAt` is refreshed on update.
 *  - `remove` drops by id; leaves others intact.
 *  - `resetLastChargedAt` updates `lastChargedAt` AND `updatedAt` to now.
 *  - Corrupt JSON blob in storage is tolerated — repository yields empty
 *    list rather than crashing (see `decode()` catch in production code).
 *  - Concurrent upserts serialise via the mutex; no entries lost.
 */
class CycleDeviceRepositoryTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    private lateinit var dataStoreFile: File
    private lateinit var scope: CoroutineScope
    private lateinit var dataStore: DataStore<Preferences>
    private lateinit var repo: CycleDeviceRepository

    @Before
    fun setUp() {
        dataStoreFile = tempFolder.newFile("cycle_devices.preferences_pb")
        scope = CoroutineScope(Job())
        dataStore = PreferenceDataStoreFactory.create(scope = scope) { dataStoreFile }
        repo = CycleDeviceRepository(dataStore)
    }

    @After
    fun tearDown() {
        runBlocking { scope.coroutineContext[Job]?.cancelAndJoin() }
    }

    // ─── empty state ─────────────────────────────────────────────────────────

    @Test
    fun devices_emptyWhenNothingPersisted() = runBlocking {
        assertEquals(emptyList<CycleDevice>(), repo.devices.first())
    }

    // ─── upsert ──────────────────────────────────────────────────────────────

    @Test
    fun upsert_insertsNewDevice() = runBlocking {
        val dev = makeDevice(id = "a", name = "Door Lock")
        repo.upsert(dev)
        assertEquals(listOf(dev), repo.devices.first())
    }

    @Test
    fun upsert_updatesExistingDevice() = runBlocking {
        val original = makeDevice(id = "a", name = "Door Lock", cycleDays = 30)
        repo.upsert(original)

        val edited = original.copy(name = "Front Door Lock", cycleDays = 45)
        repo.upsert(edited)

        val stored = repo.devices.first()
        assertEquals(1, stored.size)
        val storedDev = stored.first()
        assertEquals("Front Door Lock", storedDev.name)
        assertEquals(45, storedDev.cycleDays)
        // updatedAt should be refreshed to "now" (>= original).
        assertTrue("updatedAt must be >= original", storedDev.updatedAt >= original.updatedAt)
    }

    @Test
    fun upsert_preservesCreatedAtOnUpdate() = runBlocking {
        val original = makeDevice(id = "a", createdAt = 1000L, updatedAt = 1000L)
        repo.upsert(original)

        val edited = original.copy(name = "Renamed", updatedAt = 9999L)
        repo.upsert(edited)

        val stored = repo.devices.first().first()
        // createdAt is caller-owned — repo never overwrites it.
        assertEquals(1000L, stored.createdAt)
    }

    @Test
    fun upsert_multipleDevicesAllPersisted() = runBlocking {
        repo.upsert(makeDevice(id = "a", name = "A"))
        repo.upsert(makeDevice(id = "b", name = "B"))
        repo.upsert(makeDevice(id = "c", name = "C"))

        val stored = repo.devices.first().sortedBy { it.id }
        assertEquals(listOf("a", "b", "c"), stored.map { it.id })
        assertEquals(listOf("A", "B", "C"), stored.map { it.name })
    }

    // ─── remove ──────────────────────────────────────────────────────────────

    @Test
    fun remove_dropsByIdOnly() = runBlocking {
        repo.upsert(makeDevice(id = "a", name = "A"))
        repo.upsert(makeDevice(id = "b", name = "B"))
        repo.upsert(makeDevice(id = "c", name = "C"))

        repo.remove("b")

        val stored = repo.devices.first().sortedBy { it.id }
        assertEquals(listOf("a", "c"), stored.map { it.id })
    }

    @Test
    fun remove_unknownIdIsNoOp() = runBlocking {
        repo.upsert(makeDevice(id = "a", name = "A"))
        repo.remove("nonexistent")
        assertEquals(1, repo.devices.first().size)
    }

    @Test
    fun remove_lastDeviceYieldsEmptyList() = runBlocking {
        repo.upsert(makeDevice(id = "a", name = "A"))
        repo.remove("a")
        assertTrue(repo.devices.first().isEmpty())
    }

    // ─── resetLastChargedAt ──────────────────────────────────────────────────

    @Test
    fun resetLastChargedAt_updatesLastChargedAndUpdatedAt() = runBlocking {
        val original = makeDevice(id = "a", lastChargedAt = 1_000L)
        repo.upsert(original)

        Thread.sleep(2)  // ensure "now" > lastChargedAt
        repo.resetLastChargedAt("a")

        val stored = repo.devices.first().first()
        assertTrue("lastChargedAt must be > original", stored.lastChargedAt > 1_000L)
        assertTrue("updatedAt must be > original", stored.updatedAt > 1_000L)
    }

    @Test
    fun resetLastChargedAt_onlyTouchesTargetDevice() = runBlocking {
        repo.upsert(makeDevice(id = "a", lastChargedAt = 1_000L))
        repo.upsert(makeDevice(id = "b", lastChargedAt = 2_000L))

        repo.resetLastChargedAt("a")

        val stored = repo.devices.first().associateBy { it.id }
        assertTrue(stored.getValue("a").lastChargedAt > 1_000L)
        assertEquals(2_000L, stored.getValue("b").lastChargedAt)
    }

    // ─── findById ────────────────────────────────────────────────────────────

    @Test
    fun findById_returnsDevice() = runBlocking {
        val dev = makeDevice(id = "a", name = "A")
        repo.upsert(dev)
        assertEquals(dev, repo.findById("a"))
    }

    @Test
    fun findById_returnsNullWhenAbsent() = runBlocking {
        assertNull(repo.findById("nonexistent"))
        repo.upsert(makeDevice(id = "a", name = "A"))
        assertNull(repo.findById("b"))
    }

    // ─── corrupt storage ─────────────────────────────────────────────────────

    @Test
    fun devices_corruptJsonYieldsEmptyListNotCrash() = runBlocking {
        // Seed the DataStore with non-JSON garbage; on read, the repository
        // should swallow the SerializationException and return empty.
        dataStore.edit { prefs ->
            prefs[KEY_DIRTY] = "not-valid-json{["
        }

        // Reading the flow must not throw.
        val list = repo.devices.first()
        assertTrue(list.isEmpty())
    }

    @Test
    fun devices_emptyStringBlobYieldsEmptyList() = runBlocking {
        dataStore.edit { prefs ->
            prefs[KEY_DIRTY] = ""
        }
        assertTrue(repo.devices.first().isEmpty())
    }

    // ─── concurrency ─────────────────────────────────────────────────────────

    @Test
    fun concurrentUpserts_allEntriesSurvive() = runBlocking {
        // Race a swarm of upserts; the mutex serialises writes, so all
        // ids should end up persisted. The exact count is what matters —
        // ordering is not asserted (that's not part of the contract).
        val ids = (1..20).map { "id-$it" }
        val jobs = ids.map { id ->
            async(Dispatchers.IO) {
                repo.upsert(makeDevice(id = id, name = "n-$id"))
            }
        }
        awaitAll(*jobs.toTypedArray())

        val stored = repo.devices.first()
        assertEquals(ids.toSet(), stored.map { it.id }.toSet())
        assertNotNull(stored.firstOrNull())
    }

    // ─── helpers ─────────────────────────────────────────────────────────────

    private fun makeDevice(
        id: String = "id",
        name: String = "device",
        cycleDays: Int = 30,
        lastChargedAt: Long = 0L,
        createdAt: Long = 0L,
        updatedAt: Long = 0L,
    ) = CycleDevice(
        id = id,
        name = name,
        category = null,
        cycleDays = cycleDays,
        lastChargedAt = lastChargedAt,
        note = null,
        createdAt = createdAt,
        updatedAt = updatedAt,
    )

    private companion object {
        // Match the key used by [CycleDeviceRepository] so we can dirty the
        // stored blob directly when testing the corrupt-blob fallback.
        val KEY_DIRTY = stringPreferencesKey("cycle_devices_json")
    }
}