package com.kap1bala.icypower.data.cycle

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone

/**
 * Day-level date utilities for the cycle overview / calendar surfaces.
 *
 * Why epoch day (Long) instead of [java.time.LocalDate]?
 *   - The project targets `minSdk = 24`, so using `java.time` directly would
 *     require `coreLibraryDesugaringEnabled`. We avoid that surface area to
 *     keep the build simple and JVM-only unit-testable.
 *   - Epoch day fits `Map<Long, CycleDaySummary>` cleanly as a key.
 *   - Display formatting still uses [SimpleDateFormat] / [Calendar], the same
 *     path that [com.kap1bala.icypower.ui.cycle.CycleFormatting] already uses.
 *
 * Time-zone semantics:
 *   - "Day" here means a local-time calendar day in the device's current
 *     default time zone. Two timestamps captured on adjacent local days map to
 *     distinct epoch days; the same local day maps to the same epoch day
 *     regardless of clock-time within the day.
 *   - We read [TimeZone.getDefault] each call; v1 doesn't react to runtime TZ
 *     changes (extremely rare on phones).
 */
internal const val MILLIS_PER_DAY = 86_400_000L

/**
 * Epoch day (days since 1970-01-01 UTC) for the local-time day containing
 * [millis]. Aligns the timestamp to the start of its local day before dividing.
 */
internal fun epochDayOf(millis: Long): Long {
    val offsetMillis = TimeZone.getDefault().getOffset(millis).toLong()
    return (millis + offsetMillis) / MILLIS_PER_DAY
}

/** Today as epoch day in local TZ. Wraps [System.currentTimeMillis]. */
internal fun todayEpochDay(): Long = epochDayOf(System.currentTimeMillis())

/**
 * Calendar fields (year, month 1..12, day-of-month 1..31) for an epoch day.
 * Inverse of [epochDayOf] combined with [calendarDayOf].
 */
internal fun calendarDayOf(day: Long): Triple<Int, Int, Int> {
    val cal = Calendar.getInstance().apply {
        timeZone = TimeZone.getDefault()
        timeInMillis = startOfDayMillis(day)
    }
    val y = cal.get(Calendar.YEAR)
    val m = cal.get(Calendar.MONTH) + 1  // Calendar.MONTH is 0-based
    val d = cal.get(Calendar.DAY_OF_MONTH)
    return Triple(y, m, d)
}

/** Inverse: build epoch day from explicit (year, month 1..12, day 1..31). */
internal fun epochDayOf(y: Int, m: Int, d: Int): Long {
    val cal = Calendar.getInstance().apply {
        clear()
        set(y, m - 1, d, 0, 0, 0)
        set(Calendar.MILLISECOND, 0)
    }
    val offsetMillis = cal.timeZone.getOffset(cal.timeInMillis).toLong()
    // Floor to start-of-local-day before dividing, so DST transitions don't
    // shift the result by ±1.
    val aligned = cal.timeInMillis + offsetMillis
    return aligned / MILLIS_PER_DAY
}

/** Epoch day for the first day of the given (year, month 1..12). */
internal fun epochDayOfFirstOfMonth(year: Int, month: Int): Long =
    epochDayOf(year, month, 1)

/** Epoch day for the last day of the given (year, month 1..12). */
internal fun epochDayOfLastOfMonth(year: Int, month: Int): Long {
    val cal = Calendar.getInstance().apply {
        clear()
        set(year, month - 1, 1, 0, 0, 0)
        set(Calendar.MILLISECOND, 0)
    }
    val last = cal.getActualMaximum(Calendar.DAY_OF_MONTH)
    return epochDayOf(year, month, last)
}

private fun startOfDayMillis(day: Long): Long = day * MILLIS_PER_DAY

// ─── Formatting (locale-aware; no string resources required) ────────────────

/** "yyyy-MM-dd" — mirrors `CycleFormatting.formatDisplayDate` style. */
internal fun formatEpochDay(day: Long): String =
    SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date(day * MILLIS_PER_DAY))

/**
 * "2026 年 8 月" (zh) / "August 2026" (en).
 *
 * Picks the pattern from [Locale.getDefault]: CJK locales get the compact
 * `yyyy 年 M 月` form; everything else uses the [java.text.DateFormat]
 * `MMMM yyyy` default.
 */
internal fun formatMonth(year: Int, month: Int): String {
    val locale = Locale.getDefault()
    val date = Date(epochDayOf(year, month, 1) * MILLIS_PER_DAY)
    val pattern = if (locale.language == "zh") "yyyy 年 M 月" else "MMMM yyyy"
    return SimpleDateFormat(pattern, locale).format(date)
}

/**
 * Locale-aware short weekday for [java.util.Calendar] day-of-week constants.
 * `dayOfWeek` is 1..7 with Sunday=1, Monday=2, ..., Saturday=7.
 *
 * zh → "日 一 二 三 四 五 六" (single char)
 * en → "S M T W T F S" via `TextStyle.NARROW`-style output (one letter).
 */
internal fun formatWeekdayShort(dayOfWeek: Int): String {
    val locale = Locale.getDefault()
    val cal = Calendar.getInstance().apply {
        set(Calendar.DAY_OF_WEEK, dayOfWeek)
    }
    // `EEEEE` = narrow form: single char in zh, single letter in en.
    return SimpleDateFormat("EEEEE", locale).format(cal.time)
}

/** Returns first day-of-week (1..7, Sunday=1) for current locale. */
internal fun firstDayOfWeek(): Int =
    Calendar.getInstance().firstDayOfWeek

/** Step year/month by [delta] months, normalizing overflows. */
internal fun stepMonth(year: Int, month: Int, delta: Int): Pair<Int, Int> {
    // Convert to absolute month count from year 0, then back via Math.floorDiv
    // / Math.floorMod so negative deltas (prev-year) come out correct.
    val abs = year * 12 + (month - 1) + delta
    val newYear = Math.floorDiv(abs, 12)
    val newMonth0 = Math.floorMod(abs, 12)
    return newYear to (newMonth0 + 1)
}