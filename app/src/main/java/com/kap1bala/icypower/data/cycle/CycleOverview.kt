package com.kap1bala.icypower.data.cycle

/**
 * Aggregated dashboard data for the cycle-devices "概览" tab.
 *
 * Two surfaces consume this:
 *   - The 4-tile overview strip at the top (`CycleOverviewStats`).
 *   - The month calendar below (`calendar: Map<Long, CycleDaySummary>`).
 *
 * Pure-data; computed by [computeOverview] from a list of [CycleDeviceState]
 * and a single "today" epoch day. No system clock reads inside this file
 * so unit tests can pin `today` deterministically.
 */

/**
 * Tile counts for the overview strip.
 *
 * **Bucket semantics** (intentionally **inclusive** rather than exclusive —
 * `dueTomorrow` ⊂ `dueInNext7` ⊂ `dueInNext30` — so the larger tiles always
 * show ≥ the smaller ones, which reads more naturally).
 *
 * Bucketing is based on [nextChargeDateEpochDay] — the *calendar day* the
 * device next crosses its cycle threshold — compared to `today`, **not** on
 * `daysSinceLastCharge` (a floating-point delta from the current clock).
 * Using the natural day avoids mis-bucketing a device that crosses its
 * threshold later *today* (still < 24h away) into "tomorrow":
 *
 * | Field          | Condition on each device (severity == None unless noted)         |
 * | -------------- | --------------------------------------------------------------- |
 * | overdueToday   | severity ∈ {Warning, Danger}                                    |
 * | dueTomorrow    | nextChargeDateEpochDay == today + 1                             |
 * | dueInNext7     | today + 1 ≤ nextChargeDateEpochDay ≤ today + 7                  |
 * | dueInNext30    | today + 1 ≤ nextChargeDateEpochDay ≤ today + 30                 |
 *
 * Devices with `cycleDays == 0` are always overdue (handled by
 * [CycleDeviceState.from] → Danger), so they appear in [overdueToday] and
 * never in the upcoming buckets.
 */
data class CycleOverviewStats(
    val overdueToday: Int,
    val dueTomorrow: Int,
    val dueInNext7: Int,
    val dueInNext30: Int,
) {
    companion object {
        val ZERO = CycleOverviewStats(0, 0, 0, 0)
    }
}

/**
 * Per-day summary used by the month calendar.
 *
 * Only built for days that have at least one device; the calendar renders
 * empty cells for days not in the map.
 *
 * - [hasOverdue] / [hasDanger] are meaningful only for `day == today`
 *   (overdue devices pin to today; see [nextChargeDateEpochDay]). Both flags
 *   live on every summary to keep the data model uniform.
 * - [upcomingCount] counts devices whose `nextChargeDateEpochDay == day` and
 *   which are not currently overdue (severity == None).
 */
data class CycleDaySummary(
    val day: Long,
    val hasOverdue: Boolean,
    val hasDanger: Boolean,
    val upcomingCount: Int,
) {
    /** True when this day has any visual marker (overdue, danger, or upcoming). */
    val hasAny: Boolean
        get() = hasOverdue || hasDanger || upcomingCount > 0
}

/** Bundled result: stat strip + sparse calendar map keyed by epoch day. */
data class CycleOverview(
    val stats: CycleOverviewStats,
    /** Sparse: days absent from this map render as empty cells. */
    val calendar: Map<Long, CycleDaySummary>,
) {
    companion object {
        val EMPTY = CycleOverview(CycleOverviewStats.ZERO, emptyMap())
    }
}

// ─── Pure helpers ──────────────────────────────────────────────────────────

/**
 * Days remaining until the next scheduled charge.
 *
 *   delta = cycleDays - daysSinceLastCharge
 *
 * With non-negative `cycleDays`, this is non-negative whenever
 * `daysSinceLastCharge` is non-negative — both natural inputs. Clock skew
 * (future `lastChargedAt`) just makes the result larger (the device's next
 * charge is even further away), which is the correct behavior.
 */
fun daysUntilDue(state: CycleDeviceState): Long =
    state.device.cycleDays.toLong() - state.daysSinceLastCharge

/**
 * The local day on which this device should next be charged.
 *
 * | Current severity | Returned day                                                  |
 * | ---------------- | ------------------------------------------------------------- |
 * | None             | `epochDayOf(lastChargedAt) + cycleDays`                       |
 * | Warning / Danger | `today` (overdue devices pin to "today"; see plan §2)         |
 *
 * Devices with `cycleDays <= 0` return `today`.
 */
fun nextChargeDateEpochDay(state: CycleDeviceState, today: Long): Long {
    val sev = state.severity
    if (sev != OverdueSeverity.None) return today
    val baseDay = epochDayOf(state.device.lastChargedAt)
    return baseDay + state.device.cycleDays.toLong()
}

// ─── Top-level aggregation ──────────────────────────────────────────────────

/**
 * Build a [CycleOverview] from the full list of cycle device states.
 *
 * - Empty input → all-zero stats and empty calendar.
 * - Each device contributes to exactly one calendar entry (its
 *   `nextChargeDateEpochDay`).
 * - `today` is supplied explicitly so tests can pin the date.
 */
fun computeOverview(
    states: List<CycleDeviceState>,
    today: Long,
): CycleOverview {
    if (states.isEmpty()) return CycleOverview.EMPTY

    var overdueToday = 0
    var dueTomorrow = 0
    var dueInNext7 = 0
    var dueInNext30 = 0

    // Mutable accumulator for the sparse calendar map. We build a
    // List<MutableSummary> in epoch-day order then materialize to Map at the
    // end — easier to compose flags + counts than to wrestle with Map
    // mutation while iterating.
    data class Acc(
        var hasOverdue: Boolean = false,
        var hasDanger: Boolean = false,
        var upcomingCount: Int = 0,
    )
    val byDay = HashMap<Long, Acc>()

    for (state in states) {
        val sev = state.severity
        if (sev != OverdueSeverity.None) {
            overdueToday++
            val acc = byDay.getOrPut(today) { Acc() }
            acc.hasOverdue = true
            if (sev == OverdueSeverity.Danger) acc.hasDanger = true
            continue
        }

        // Bucket by the calendar day the device next crosses its cycle
        // threshold (nextChargeDateEpochDay) — see the class doc for why
        // this is correct where `daysSinceLastCharge` alone is not.
        val day = nextChargeDateEpochDay(state, today)
        val ahead = day - today
        when (ahead) {
            1L -> dueTomorrow++          // exactly tomorrow
        }
        if (ahead in 1L..7L) dueInNext7++
        if (ahead in 1L..30L) dueInNext30++

        val acc = byDay.getOrPut(day) { Acc() }
        acc.upcomingCount++
    }

    val calendar = byDay.mapValues { (day, acc) ->
        CycleDaySummary(
            day = day,
            hasOverdue = acc.hasOverdue,
            hasDanger = acc.hasDanger,
            upcomingCount = acc.upcomingCount,
        )
    }

    return CycleOverview(
        stats = CycleOverviewStats(
            overdueToday = overdueToday,
            dueTomorrow = dueTomorrow,
            dueInNext7 = dueInNext7,
            dueInNext30 = dueInNext30,
        ),
        calendar = calendar,
    )
}