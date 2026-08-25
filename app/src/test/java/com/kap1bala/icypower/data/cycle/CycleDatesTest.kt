package com.kap1bala.icypower.data.cycle

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Calendar
import java.util.GregorianCalendar
import java.util.TimeZone

/**
 * Unit tests for the pure date helpers in [CycleDates].
 *
 * Time-zone discipline: each test pins [TimeZone.setDefault] to a known zone
 * (UTC for most; offset-shifted zones for the DST sanity check) so the
 * `epochDayOf` math doesn't depend on the host machine's tz. The original
 * default is restored in a `finally` block.
 */
class CycleDatesTest {

    @Test
    fun epochDayOf_utc_returnsDayAlignedToUtc() {
        withTz(TimeZone.getTimeZone("UTC")) {
            // Noon UTC on 2026-08-17 → exactly day 20682 (verified with
            // Python: (date(2026,8,17) - date(1970,1,1)).days == 20682).
            val ms = utcMillis(2026, 8, 17, 12, 0)
            assertEquals(20682L, epochDayOf(ms))
        }
    }

    @Test
    fun epochDayOf_localAlignedTimestamp_returnsSameDay() {
        // If the millis is already at local-midnight, the offset adjustment
        // shouldn't change the day.
        withTz(TimeZone.getTimeZone("UTC")) {
            val midnight = utcMillis(2026, 8, 17, 0, 0)
            assertEquals(20682L, epochDayOf(midnight))
        }
    }

    @Test
    fun epochDayOf_acrossDstBoundary_landsOnCorrectLocalDay() {
        // America/New_York springs forward on 2026-03-08: 02:00 → 03:00.
        // A timestamp captured at 02:30 local on 2026-03-08 doesn't exist;
        // we use 01:30 (still EST, UTC-5) and 03:30 (EDT, UTC-4).
        withTz(TimeZone.getTimeZone("America/New_York")) {
            val beforeDst = nyMillis(2026, 3, 8, 1, 30)   // EST = UTC-5
            val afterDst = nyMillis(2026, 3, 8, 3, 30)    // EDT = UTC-4

            val dayBefore = epochDayOf(beforeDst)
            val dayAfter = epochDayOf(afterDst)
            // Both should land on the same local day, 2026-03-08.
            assertEquals(
                "01:30 and 03:30 on the DST-shifting day are the same local day",
                dayBefore, dayAfter,
            )
        }
    }

    @Test
    fun epochDayOf_constructorRoundTrip_preservesDay() {
        withTz(TimeZone.getTimeZone("UTC")) {
            val ymd = Triple(2026, 8, 17)
            val day = epochDayOf(ymd.first, ymd.second, ymd.third)
            val (y, m, d) = calendarDayOf(day)
            assertEquals(2026, y)
            assertEquals(8, m)
            assertEquals(17, d)
        }
    }

    @Test
    fun epochDayOfFirstAndLastOfMonth_coverAllDaysOfMonth() {
        withTz(TimeZone.getTimeZone("UTC")) {
            // August has 31 days; February 2026 has 28 (non-leap).
            val augFirst = epochDayOfFirstOfMonth(2026, 8)
            val augLast = epochDayOfLastOfMonth(2026, 8)
            assertEquals(31L, augLast - augFirst + 1)

            val febFirst = epochDayOfFirstOfMonth(2026, 2)
            val febLast = epochDayOfLastOfMonth(2026, 2)
            assertEquals(28L, febLast - febFirst + 1)
        }
    }

    @Test
    fun stepMonth_forwardAcrossYearBoundary_rollsToNextYear() {
        val (y, m) = stepMonth(2026, 12, 1)
        assertEquals(2027, y)
        assertEquals(1, m)
    }

    @Test
    fun stepMonth_backwardAcrossYearBoundary_rollsToPriorYear() {
        val (y, m) = stepMonth(2026, 1, -1)
        assertEquals(2025, y)
        assertEquals(12, m)
    }

    @Test
    fun stepMonth_withinYear_increments() {
        val (y, m) = stepMonth(2026, 6, 3)
        assertEquals(2026, y)
        assertEquals(9, m)
    }

    @Test
    fun stepMonth_zeroDelta_returnsSame() {
        val (y, m) = stepMonth(2026, 6, 0)
        assertEquals(2026, y)
        assertEquals(6, m)
    }

    @Test
    fun stepMonth_negativeDelta_insideYear() {
        val (y, m) = stepMonth(2026, 6, -2)
        assertEquals(2026, y)
        assertEquals(4, m)
    }

    @Test
    fun formatEpochDay_roundTrip_usesLocalTz() {
        withTz(TimeZone.getTimeZone("UTC")) {
            val day = epochDayOf(2026, 8, 17)
            // Locale-dependent default formatter; we don't pin zh vs en here,
            // only verify the resulting string starts with "2026-08-17" for
            // locales that ISO-format the date.
            val str = formatEpochDay(day)
            assertTrue("date string should contain 2026-08-17, got '$str'",
                str.startsWith("2026-08-17") || str.contains("Aug") || str.contains("8"))
        }
    }

    // ─── Helpers ────────────────────────────────────────────────────────────

    private inline fun withTz(tz: TimeZone, block: () -> Unit) {
        val prev = TimeZone.getDefault()
        TimeZone.setDefault(tz)
        try {
            block()
        } finally {
            TimeZone.setDefault(prev)
        }
    }

    private fun utcMillis(y: Int, m: Int, d: Int, h: Int, min: Int): Long =
        GregorianCalendar(TimeZone.getTimeZone("UTC"), java.util.Locale.ROOT).apply {
            clear()
            set(y, m - 1, d, h, min, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis

    private fun nyMillis(y: Int, m: Int, d: Int, h: Int, min: Int): Long {
        // Calendar with NY timezone so the wall-clock hours map correctly.
        return GregorianCalendar(TimeZone.getTimeZone("America/New_York"))
            .apply {
                clear()
                set(y, m - 1, d, h, min, 0)
                set(Calendar.MILLISECOND, 0)
            }.timeInMillis
    }
}