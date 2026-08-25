package com.kap1bala.icypower.ui.cycle

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import com.kap1bala.icypower.data.cycle.CycleDevice
import com.kap1bala.icypower.data.cycle.CycleDeviceRepository
import com.kap1bala.icypower.data.cycle.OverdueSeverity
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.yield
import kotlinx.coroutines.test.TestScope
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

/**
 * Unit tests for [CycleDeviceListViewModel.markCharged] — specifically the
 * "all-clear" event contract.
 *
 * Pinned behaviours:
 *  - Emits [AllClearEvent.AllDevicesCharged] when the last overdue device
 *    is marked charged.
 *  - Does NOT emit when there are no overdue devices to begin with.
 *  - Does NOT emit when at least one other device stays overdue.
 *
 * Uses a real [CycleDeviceRepository] backed by a temp-file Preference
 * DataStore — cheapest way to exercise the full
 * `repo.devices → StateFlow → viewModelScope.launch` path without mocking.
 *
 * Dispatcher notes:
 *  - `UnconfinedTestDispatcher` is set as `Main` so `viewModelScope.launch`
 *    blocks inside `markCharged` start executing immediately, and the
 *    `devices` StateFlow (collected on `backgroundScope`) is subscribed
 *    synchronously. This avoids the race where `markCharged`'s inner
 *    `devices.value` read happens before the upstream Flow has refreshed.
 *  - `advanceUntilIdle()` is called after `markCharged` to let the
 *    DataStore's internal actor flush its write and emit the new state.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class CycleDeviceListViewModelTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    private lateinit var dataStoreFile: File
    private lateinit var ioScope: CoroutineScope
    private lateinit var dataStore: DataStore<Preferences>
    private lateinit var repo: CycleDeviceRepository
    private val testDispatcher = UnconfinedTestDispatcher()

    // Now in millis. Pinned so all "days ago" calculations are
    // deterministic regardless of when the test happens to run.
    private val fixedNow: Long = 1_700_000_000_000L  // arbitrary, far-past

    @Before
    fun setUp() {
        Dispatchers.setMain(testDispatcher)
        dataStoreFile = tempFolder.newFile("vm_cycle.preferences_pb")
        // Use the SAME testDispatcher for DataStore's internal actor —
        // otherwise writes go through a real Dispatcher and
        // advanceUntilIdle() can't drive them to completion.
        ioScope = CoroutineScope(testDispatcher + Job())
        dataStore = PreferenceDataStoreFactory.create(scope = ioScope) { dataStoreFile }
        repo = CycleDeviceRepository(dataStore)
    }

    @After
    fun tearDown() {
        runBlocking {
            ioScope.coroutineContext[Job]?.cancelAndJoin()
            Dispatchers.resetMain()
        }
    }

    // ─── helpers ─────────────────────────────────────────────────────────────

    private fun device(id: String, cycleDays: Int, lastChargedAt: Long): CycleDevice =
        CycleDevice(
            id = id,
            name = "n-$id",
            category = null,
            cycleDays = cycleDays,
            lastChargedAt = lastChargedAt,
            note = null,
            createdAt = lastChargedAt,
            updatedAt = lastChargedAt,
        )

    private fun newVm() = CycleDeviceListViewModel(repo, clock = { fixedNow })

    /** Suspend until [predicate] holds for the latest emitted list. */
    private suspend fun awaitDevices(
        vm: CycleDeviceListViewModel,
        predicate: (List<com.kap1bala.icypower.data.cycle.CycleDeviceState>) -> Boolean,
    ) {
        // `first {}` subscribes internally and returns the first emission
        // matching `predicate`. If the StateFlow has already emitted a value
        // matching it, this returns immediately; otherwise it waits for the
        // next matching one.
        vm.devices.first(predicate)
    }

    // ─── all-clear event ─────────────────────────────────────────────────────

    @Test
    fun markCharged_lastOverdue_emitsAllClear() = runTest(testDispatcher) {
        // One device, charged 60 days ago on a 30-day cycle → Danger.
        repo.upsert(device(id = "a", cycleDays = 30, lastChargedAt = fixedNow - 60L * 86_400_000L))

        val vm = newVm()
        // Keep the devices StateFlow hot so .value reflects new writes.
        val collectJob = backgroundScope.launch { vm.devices.collect { /* drain */ } }
        val events = mutableListOf<AllClearEvent>()
        val eventJob = backgroundScope.launch { vm.allClearEvents.collect { events.add(it) } }

        // Wait for the overdue state to be visible in the StateFlow before
        // we call markCharged — this is what `wasOverdue` checks.
        awaitDevices(vm) { it.any { s -> s.severity == OverdueSeverity.Danger } }

        vm.markCharged("a")
        // Drive the scheduler repeatedly so the DataStore actor, the
        // downstream `.map { … }`, and the StateFlow's internal collect
        // all flush before we read `events`. A single advanceUntilIdle()
        // is sometimes not enough because the inner `viewModelScope.launch`
        // can resume and read `devices.value` before the StateFlow's
        // collector has had a chance to transform the new write.
        repeat(5) {
            advanceUntilIdle()
            yield()
        }

        assertEquals(listOf(AllClearEvent.AllDevicesCharged), events)

        collectJob.cancel()
        eventJob.cancel()
    }

    @Test
    fun markCharged_neverOverdue_doesNotEmit() = runTest(testDispatcher) {
        // One device, charged today on a 30-day cycle → None severity.
        repo.upsert(device(id = "a", cycleDays = 30, lastChargedAt = fixedNow))

        val vm = newVm()
        val collectJob = backgroundScope.launch { vm.devices.collect { /* drain */ } }
        val events = mutableListOf<AllClearEvent>()
        val eventJob = backgroundScope.launch { vm.allClearEvents.collect { events.add(it) } }

        awaitDevices(vm) { it.isNotEmpty() }

        vm.markCharged("a")
        advanceUntilIdle()
        yield()
        awaitDevices(vm) { it.isNotEmpty() && it.all { s -> s.severity == OverdueSeverity.None } }
        advanceUntilIdle()

        assertTrue("no all-clear should fire when there was nothing overdue", events.isEmpty())

        collectJob.cancel()
        eventJob.cancel()
    }

    @Test
    fun markCharged_otherDeviceStillOverdue_doesNotEmit() = runTest(testDispatcher) {
        // Two overdue devices — charging one leaves the other overdue.
        repo.upsert(device(id = "a", cycleDays = 30, lastChargedAt = fixedNow - 60L * 86_400_000L))
        repo.upsert(device(id = "b", cycleDays = 30, lastChargedAt = fixedNow - 60L * 86_400_000L))

        val vm = newVm()
        val collectJob = backgroundScope.launch { vm.devices.collect { /* drain */ } }
        val events = mutableListOf<AllClearEvent>()
        val eventJob = backgroundScope.launch { vm.allClearEvents.collect { events.add(it) } }

        awaitDevices(vm) { it.size == 2 && it.all { s -> s.severity != OverdueSeverity.None } }

        vm.markCharged("a")
        advanceUntilIdle()
        yield()

        // Confirm b is still overdue after the reset.
        awaitDevices(vm) { it.any { s -> s.severity != OverdueSeverity.None } }

        assertTrue("second device still overdue → no all-clear", events.isEmpty())

        collectJob.cancel()
        eventJob.cancel()
    }

    @Test
    fun markCharged_warningSeverityCountsAsOverdue() = runTest(testDispatcher) {
        // 30 days on a 30-day cycle → Warning (severity != None).
        repo.upsert(device(id = "a", cycleDays = 30, lastChargedAt = fixedNow - 30L * 86_400_000L))

        val vm = newVm()
        val collectJob = backgroundScope.launch { vm.devices.collect { /* drain */ } }
        val events = mutableListOf<AllClearEvent>()
        val eventJob = backgroundScope.launch { vm.allClearEvents.collect { events.add(it) } }

        awaitDevices(vm) { it.any { s -> s.severity == OverdueSeverity.Warning } }

        vm.markCharged("a")
        repeat(5) {
            advanceUntilIdle()
            yield()
        }
        awaitDevices(vm) { it.isNotEmpty() && it.all { s -> s.severity == OverdueSeverity.None } }
        repeat(3) {
            advanceUntilIdle()
            yield()
        }

        assertEquals(listOf(AllClearEvent.AllDevicesCharged), events)

        collectJob.cancel()
        eventJob.cancel()
    }

    @Test
    fun markCharged_unknownIdIsNoOp() = runTest(testDispatcher) {
        repo.upsert(device(id = "a", cycleDays = 30, lastChargedAt = fixedNow - 60L * 86_400_000L))

        val vm = newVm()
        val collectJob = backgroundScope.launch { vm.devices.collect { /* drain */ } }
        val events = mutableListOf<AllClearEvent>()
        val eventJob = backgroundScope.launch { vm.allClearEvents.collect { events.add(it) } }

        awaitDevices(vm) { it.any { s -> s.severity != OverdueSeverity.None } }

        // Unknown id: repo's resetLastChargedAt silently no-ops; "a" stays
        // overdue → the all-clear branch must not fire.
        vm.markCharged("does-not-exist")
        advanceUntilIdle()

        assertTrue(events.isEmpty())

        collectJob.cancel()
        eventJob.cancel()
    }

    @Test
    fun devices_sortBySeverityThenDaysDescending() = runTest(testDispatcher) {
        repo.upsert(device(id = "none", cycleDays = 30, lastChargedAt = fixedNow))
        repo.upsert(device(id = "danger", cycleDays = 30, lastChargedAt = fixedNow - 60L * 86_400_000L))
        repo.upsert(device(id = "warning", cycleDays = 30, lastChargedAt = fixedNow - 31L * 86_400_000L))

        val vm = newVm()
        val collectJob = backgroundScope.launch { vm.devices.collect { /* drain */ } }

        // Wait for all three to land in the StateFlow.
        awaitDevices(vm) { it.size == 3 }
        advanceUntilIdle()

        val list = vm.devices.value
        // Severities sorted descending: Danger (2), Warning (1), None (0).
        assertEquals(OverdueSeverity.Danger, list[0].severity)
        assertEquals(OverdueSeverity.Warning, list[1].severity)
        assertEquals(OverdueSeverity.None, list[2].severity)

        collectJob.cancel()
    }
}