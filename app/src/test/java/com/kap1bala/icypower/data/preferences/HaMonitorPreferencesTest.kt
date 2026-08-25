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
import org.junit.Assert.assertThrows
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

/**
 * Unit tests for [HaMonitorPreferences] validation contract.
 *
 * Behaviour pinned:
 *  - `warning` and `danger` must both be in `1..100`.
 *  - `danger` must be strictly less than `warning`.
 *
 * Persistence is exercised by writing a single valid pair and reading it
 * back via the exposed [Flow]s. Invalid input is asserted to throw
 * `IllegalArgumentException` BEFORE any disk write happens (the underlying
 * `require` blocks fire first).
 */
class HaMonitorPreferencesTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    private lateinit var dataStoreFile: File
    private lateinit var scope: CoroutineScope
    private lateinit var dataStore: DataStore<Preferences>
    private lateinit var prefs: HaMonitorPreferences

    @Before
    fun setUp() {
        dataStoreFile = tempFolder.newFile("ha_monitor.preferences_pb")
        scope = CoroutineScope(Job())
        dataStore = PreferenceDataStoreFactory.create(scope = scope) { dataStoreFile }
        prefs = HaMonitorPreferences(dataStore)
    }

    @After
    fun tearDown() {
        runBlocking { scope.coroutineContext[Job]?.cancelAndJoin() }
    }

    // ─── validation ──────────────────────────────────────────────────────────

    @Test
    fun setThresholds_rejectsWarningBelow1() {
        val e = assertThrows(IllegalArgumentException::class.java) {
            runBlocking { prefs.setThresholds(warning = 0, danger = 10) }
        }
        assertEquals(true, e.message?.contains("1..100"))
    }

    @Test
    fun setThresholds_rejectsWarningAbove100() {
        assertThrows(IllegalArgumentException::class.java) {
            runBlocking { prefs.setThresholds(warning = 101, danger = 10) }
        }
    }

    @Test
    fun setThresholds_rejectsDangerBelow1() {
        assertThrows(IllegalArgumentException::class.java) {
            runBlocking { prefs.setThresholds(warning = 20, danger = 0) }
        }
    }

    @Test
    fun setThresholds_rejectsDangerAbove100() {
        assertThrows(IllegalArgumentException::class.java) {
            runBlocking { prefs.setThresholds(warning = 20, danger = 101) }
        }
    }

    @Test
    fun setThresholds_rejectsDangerEqualToWarning() {
        // `danger < warning` is strict — equal is rejected.
        val e = assertThrows(IllegalArgumentException::class.java) {
            runBlocking { prefs.setThresholds(warning = 20, danger = 20) }
        }
        assertEquals(true, e.message?.contains("strictly less than"))
    }

    @Test
    fun setThresholds_rejectsDangerAboveWarning() {
        assertThrows(IllegalArgumentException::class.java) {
            runBlocking { prefs.setThresholds(warning = 20, danger = 30) }
        }
    }

    // ─── persistence ─────────────────────────────────────────────────────────

    @Test
    fun warningThreshold_defaultsTo20WhenUnset() = runBlocking {
        assertEquals(20, prefs.warningThreshold.first())
    }

    @Test
    fun dangerThreshold_defaultsTo10WhenUnset() = runBlocking {
        assertEquals(10, prefs.dangerThreshold.first())
    }

    @Test
    fun setThresholds_persistsAndIsReadable() = runBlocking {
        prefs.setThresholds(warning = 50, danger = 25)
        assertEquals(50, prefs.warningThreshold.first())
        assertEquals(25, prefs.dangerThreshold.first())
    }

    @Test
    fun setThresholds_overwritesPreviousValue() = runBlocking {
        prefs.setThresholds(warning = 50, danger = 25)
        prefs.setThresholds(warning = 60, danger = 30)
        assertEquals(60, prefs.warningThreshold.first())
        assertEquals(30, prefs.dangerThreshold.first())
    }

    @Test
    fun setThresholds_validBoundary_accepted() = runBlocking {
        // 1 / 100: smallest valid danger and largest valid warning (and
        // danger strictly less than warning). Other boundary: warning=2,
        // danger=1 (minimum gap).
        prefs.setThresholds(warning = 100, danger = 1)
        assertEquals(100, prefs.warningThreshold.first())
        assertEquals(1, prefs.dangerThreshold.first())

        prefs.setThresholds(warning = 2, danger = 1)
        assertEquals(2, prefs.warningThreshold.first())
        assertEquals(1, prefs.dangerThreshold.first())
    }
}