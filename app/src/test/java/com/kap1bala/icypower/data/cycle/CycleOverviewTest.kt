package com.kap1bala.icypower.data.cycle

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit tests for [computeOverview] and the pure helpers in
 * [CycleOverview.kt] / [CycleDates.kt].
 *
 * `today` is pinned to `2026-08-17` (= epoch day 20671, see `today` below).
 * All `lastChargedAt` values are computed by [atLocalDay] so they're aligned
 * to midnight in the local TZ — matching how the production code reads
 * `lastChargedAt` (epoch ms stored at "已充电" click time, which can be
 * anywhere within a day).
 */
class CycleOverviewTest {

    /** Epoch day for 2026-08-17 local. */
    private val today: Long = epochDayOf(2026, 8, 17)

    // ─── Helpers ────────────────────────────────────────────────────────────

    private fun atLocalDay(year: Int, month: Int, day: Int): Long =
        epochDayOf(year, month, day) * MILLIS_PER_DAY +
            localUtcOffsetMillis(year, month, day)

    private fun localUtcOffsetMillis(year: Int, month: Int, day: Int): Long {
        val cal = java.util.Calendar.getInstance().apply {
            clear()
            set(year, month - 1, day, 12, 0, 0)  // noon avoids DST edges
            set(java.util.Calendar.MILLISECOND, 0)
        }
        return cal.timeZone.getOffset(cal.timeInMillis).toLong()
    }

    private fun device(
        id: String = "id-$System.nanoTime()",
        name: String = "dev",
        cycleDays: Int,
        lastChargedAt: Long,
    ) = CycleDevice(
        id = id,
        name = name,
        category = null,
        cycleDays = cycleDays,
        lastChargedAt = lastChargedAt,
        note = null,
        createdAt = lastChargedAt,
        updatedAt = lastChargedAt,
    )

    private fun statesOf(devices: List<CycleDevice>): List<CycleDeviceState> {
        val now = atLocalDay(2026, 8, 17) + (12L * 60 * 60 * 1000)  // noon today
        return devices.map { CycleDeviceState.from(it, now) }
    }

    // ─── Stats ──────────────────────────────────────────────────────────────

    @Test
    fun computeOverview_emptyList_returnsZeroStatsAndEmptyCalendar() {
        val result = computeOverview(emptyList(), today)

        assertEquals(CycleOverviewStats.ZERO, result.stats)
        assertTrue(result.calendar.isEmpty())
    }

    @Test
    fun computeOverview_singleOverdueDevice_bucketsOverdueOnly() {
        // 31 days past a 30-day cycle → severity Warning → overdueToday only.
        val dev = device(cycleDays = 30, lastChargedAt = atLocalDay(2026, 7, 17))
        val result = computeOverview(statesOf(listOf(dev)), today)

        assertEquals(1, result.stats.overdueToday)
        assertEquals(0, result.stats.dueTomorrow)
        assertEquals(0, result.stats.dueInNext7)
        assertEquals(0, result.stats.dueInNext30)
    }

    @Test
    fun computeOverview_dangerDevice_countsAsOverdueAndFlagsTodayDanger() {
        // 60 days past a 30-day cycle → Danger (>= 1.5×30 = 45 days).
        val dev = device(cycleDays = 30, lastChargedAt = atLocalDay(2026, 6, 18))
        val result = computeOverview(statesOf(listOf(dev)), today)

        assertEquals(1, result.stats.overdueToday)
        val summary = result.calendar[today]
        assertNotNull("today should have a summary", summary)
        assertTrue(summary!!.hasDanger)
        assertTrue(summary.hasOverdue)
    }

    @Test
    fun computeOverview_warningDevice_flagsHasOverdueButNotDanger() {
        // 31 days past a 30-day cycle → Warning only.
        val dev = device(cycleDays = 30, lastChargedAt = atLocalDay(2026, 7, 17))
        val result = computeOverview(statesOf(listOf(dev)), today)

        assertEquals(1, result.stats.overdueToday)
        val summary = result.calendar[today]
        assertNotNull(summary)
        assertTrue(summary!!.hasOverdue)
        assertFalse("31 < 1.5*30 = 45, so not danger", summary.hasDanger)
    }

    @Test
    fun computeOverview_dueTomorrow_onlyExactlyOneDayAway() {
        // All three are severity == None with deltas 1, 2, 3 → all in 7-day bucket.
        val dueTmrw = device(cycleDays = 7, lastChargedAt = atLocalDay(2026, 8, 11))     // delta=1
        val dueDayAfter = device(cycleDays = 7, lastChargedAt = atLocalDay(2026, 8, 12))  // delta=2
        val dueIn3 = device(cycleDays = 7, lastChargedAt = atLocalDay(2026, 8, 13))      // delta=3

        val result = computeOverview(statesOf(listOf(dueTmrw, dueDayAfter, dueIn3)), today)

        assertEquals(1, result.stats.dueTomorrow)
        assertEquals(3, result.stats.dueInNext7)   // all three (delta 1..3 ⊂ 0..7)
        assertEquals(3, result.stats.dueInNext30)
        assertEquals(0, result.stats.overdueToday)
    }

    @Test
    fun computeOverview_dueIn7_isInclusive() {
        // delta=5 → cycleDays=5,  lastChargedAt=today     → in 7
        val a = device(cycleDays = 5, lastChargedAt = atLocalDay(2026, 8, 17))
        // delta=6 → cycleDays=10, lastChargedAt=4 days ago → in 7
        val b = device(cycleDays = 10, lastChargedAt = atLocalDay(2026, 8, 13))
        // delta=7 → cycleDays=8,  lastChargedAt=1 day ago  → in 7 (boundary inclusive)
        val c = device(cycleDays = 8, lastChargedAt = atLocalDay(2026, 8, 16))
        // delta=15 → cycleDays=15, lastChargedAt=today     → in 30, NOT in 7
        val d = device(cycleDays = 15, lastChargedAt = atLocalDay(2026, 8, 17))

        val result = computeOverview(statesOf(listOf(a, b, c, d)), today)

        assertEquals(3, result.stats.dueInNext7)   // a, b, c
        assertEquals(4, result.stats.dueInNext30)  // all four
        assertEquals(0, result.stats.dueTomorrow)  // none have delta == 1
        // None-severity devices don't pin to today's cell — they go to
        // lastChargedAt-day + cycleDays. No None device has delta==0 here,
        // so today's cell is absent.
        assertNull("today should have no summary (no overdue devices)",
            result.calendar[today])
    }

    @Test
    fun computeOverview_dueIn30_includesDays8Through30() {
        // delta=8  → cycleDays=8,  lastChargedAt=today     → in 30 only
        val d8 = device(cycleDays = 8, lastChargedAt = atLocalDay(2026, 8, 17))
        // delta=25 → cycleDays=30, lastChargedAt=5 days ago → in 30 only
        val d25 = device(cycleDays = 30, lastChargedAt = atLocalDay(2026, 8, 12))
        // delta=30 → cycleDays=30, lastChargedAt=today     → in 30 only
        val d30 = device(cycleDays = 30, lastChargedAt = atLocalDay(2026, 8, 17))
        // delta=60 → cycleDays=60, lastChargedAt=today     → NOT in 30
        val d60 = device(cycleDays = 60, lastChargedAt = atLocalDay(2026, 8, 17))

        val result = computeOverview(statesOf(listOf(d8, d25, d30, d60)), today)
        assertEquals(3, result.stats.dueInNext30)   // d8, d25, d30
        assertEquals(0, result.stats.dueInNext7)    // none have delta 0..7
        assertEquals(0, result.stats.dueTomorrow)
    }

    @Test
    fun computeCalendar_overdueDevices_pinToTodayOnly() {
        // Two overdue devices, one Warning + one Danger.
        val warn = device(cycleDays = 30, lastChargedAt = atLocalDay(2026, 7, 17))
        val dang = device(cycleDays = 30, lastChargedAt = atLocalDay(2026, 6, 18))

        val result = computeOverview(statesOf(listOf(warn, dang)), today)

        // Only today's cell in the calendar; nothing earlier.
        assertEquals(1, result.calendar.size)
        val todayCell = result.calendar[today]
        assertNotNull(todayCell)
        assertTrue(todayCell!!.hasOverdue)
        assertTrue(todayCell.hasDanger)  // because at least one device is Danger
    }

    @Test
    fun computeCalendar_twoUpcomingDevicesSameDay_singleEntryWithCount() {
        // Both devices should land on today + 5.
        // d1: cycleDays=5, lastChargedAt=today → calendar: today + 5
        val d1 = device(id = "a", cycleDays = 5, lastChargedAt = atLocalDay(2026, 8, 17))
        // d2: cycleDays=7, lastChargedAt=2 days ago → calendar: (today-2)+7 = today+5
        val d2 = device(id = "b", cycleDays = 7, lastChargedAt = atLocalDay(2026, 8, 15))

        val result = computeOverview(statesOf(listOf(d1, d2)), today)
        val dueDay = today + 5
        val summary = result.calendar[dueDay]
        assertNotNull("both devices should map to today+5", summary)
        assertEquals(2, summary!!.upcomingCount)
        assertFalse(summary.hasOverdue)
        assertFalse(summary.hasDanger)
    }

    // ─── daysUntilDue ───────────────────────────────────────────────────────

    @Test
    fun daysUntilDue_positiveDelta_returnsDelta() {
        val dev = device(cycleDays = 30, lastChargedAt = atLocalDay(2026, 8, 2))  // 15 days ago
        val state = statesOf(listOf(dev)).first()
        assertEquals(15L, daysUntilDue(state))
    }

    @Test
    fun daysUntilDue_clockSkewFuture_returnsLargerDelta() {
        // Clock-skew: lastChargedAt 5 days in the future.
        // delta = 30 - (-5) = 35 — future lastChargedAt pushes the next
        // charge even further out, which is the correct behavior.
        val dev = device(cycleDays = 30, lastChargedAt = atLocalDay(2026, 8, 22))
        val state = statesOf(listOf(dev)).first()
        assertEquals(35L, daysUntilDue(state))
    }

    @Test
    fun daysUntilDue_cycleDaysZero_returnsZero() {
        val dev = device(cycleDays = 0, lastChargedAt = atLocalDay(2026, 8, 17))
        val state = statesOf(listOf(dev)).first()
        // 0 - 0 = 0; not negative, so no clamp.
        assertEquals(0L, daysUntilDue(state))
    }

    // ─── nextChargeDateEpochDay ─────────────────────────────────────────────

    @Test
    fun nextChargeDate_severityNone_isLastChargedPlusCycle() {
        // cycleDays=7, lastChargedAt 6 days ago → daysSinceLastCharge=6,
        // severity == None (6 < 7), so due on lastChargedAt-day + 7 = today + 1.
        val dev = device(cycleDays = 7, lastChargedAt = atLocalDay(2026, 8, 11))
        val state = statesOf(listOf(dev)).first()
        assertEquals(OverdueSeverity.None, state.severity)
        val expectedDay = today + 1
        assertEquals(expectedDay, nextChargeDateEpochDay(state, today))
    }

    @Test
    fun nextChargeDate_warningDevice_isToday() {
        val dev = device(cycleDays = 30, lastChargedAt = atLocalDay(2026, 7, 17))  // 31 days ago
        val state = statesOf(listOf(dev)).first()
        assertEquals(OverdueSeverity.Warning, state.severity)
        assertEquals(today, nextChargeDateEpochDay(state, today))
    }

    @Test
    fun nextChargeDate_dangerDevice_isToday() {
        val dev = device(cycleDays = 30, lastChargedAt = atLocalDay(2026, 6, 18))
        val state = statesOf(listOf(dev)).first()
        assertEquals(OverdueSeverity.Danger, state.severity)
        assertEquals(today, nextChargeDateEpochDay(state, today))
    }

    @Test
    fun nextChargeDate_cycleDaysZero_isToday() {
        val dev = device(cycleDays = 0, lastChargedAt = atLocalDay(2026, 8, 17))
        val state = statesOf(listOf(dev)).first()
        assertEquals(OverdueSeverity.Danger, state.severity)
        assertEquals(today, nextChargeDateEpochDay(state, today))
    }

    @Test
    fun nextChargeDate_futureLastCharged_isFutureDate() {
        // Clock-skew: lastChargedAt 5 days in the future.
        val dev = device(cycleDays = 30, lastChargedAt = atLocalDay(2026, 8, 22))
        val state = statesOf(listOf(dev)).first()
        val expectedDay = epochDayOf(2026, 8, 22) + 30
        assertEquals(expectedDay, nextChargeDateEpochDay(state, today))
    }

    // ─── edge: single empty calendar cell lookup ────────────────────────────

    @Test
    fun computeCalendar_lookupsOnAbsentDays_returnNull() {
        val result = computeOverview(emptyList(), today)
        assertNull(result.calendar[today])
        assertNull(result.calendar[today + 5])
    }

    @Test
    fun computeCalendar_cycleDaySummary_hasAnyReflectsPresence() {
        val empty = CycleDaySummary(today, hasOverdue = false, hasDanger = false, upcomingCount = 0)
        assertFalse(empty.hasAny)

        val overdue = CycleDaySummary(today, hasOverdue = true, hasDanger = false, upcomingCount = 0)
        assertTrue(overdue.hasAny)

        val upcoming = CycleDaySummary(today, hasOverdue = false, hasDanger = false, upcomingCount = 2)
        assertTrue(upcoming.hasAny)
    }
}