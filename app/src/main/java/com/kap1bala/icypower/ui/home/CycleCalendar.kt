package com.kap1bala.icypower.ui.home

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.kap1bala.icypower.R
import com.kap1bala.icypower.data.cycle.CycleDaySummary
import com.kap1bala.icypower.data.cycle.calendarDayOf
import com.kap1bala.icypower.data.cycle.epochDayOf
import com.kap1bala.icypower.data.cycle.epochDayOfFirstOfMonth
import com.kap1bala.icypower.data.cycle.epochDayOfLastOfMonth
import com.kap1bala.icypower.data.cycle.firstDayOfWeek
import com.kap1bala.icypower.data.cycle.formatMonth
import com.kap1bala.icypower.data.cycle.formatWeekdayShort
import com.kap1bala.icypower.ui.theme.IcyPowerTheme
import com.kap1bala.icypower.ui.theme.LocalDanger
import com.kap1bala.icypower.ui.theme.LocalPrimarySoft
import com.kap1bala.icypower.ui.theme.LocalSpacing
import com.kap1bala.icypower.ui.theme.LocalWarning
import java.util.Calendar

/**
 * Month calendar card showing how many devices need charging each day.
 *
 * Layout (`ui.md §8.2` card style):
 *   1. Header row: prev chevron · "2026 年 8 月" · next chevron · (jump-to-today if not current month)
 *   2. Weekday header: 7 narrow-form labels (locale-aware via [formatWeekdayShort]).
 *   3. 6×7 grid (42 cells) as plain `Column` of 6 `Row`s — see [DayGrid] for why
 *      we don't use `LazyVerticalGrid` here.
 *
 * Visual contract for each cell (`DayCell`):
 *   - leading/trailing padding cells: blank, fillMaxSize (square via the
 *     outer grid's aspectRatio(7/6)).
 *   - today: day number wrapped in `primary-soft` circle (so the user can spot "now" at a glance).
 *   - any device summary → dot below the number, colored by severity (red = danger/overdue, yellow = upcoming).
 *   - upcomingCount > 1 → count overlay next to the dot.
 *
 * Read-only (no click handler). This matches the overview-tab's "dashboard only" stance.
 */
@Composable
fun CycleCalendarCard(
    year: Int,
    month: Int,                       // 1..12
    today: Long,                      // epoch day in local TZ
    calendar: Map<Long, CycleDaySummary>,
    onPrev: () -> Unit,
    onNext: () -> Unit,
    onJumpToday: () -> Unit,
    currentYear: Int,
    currentMonth: Int,                // 1..12 — used to decide whether to render "Jump to today"
    modifier: Modifier = Modifier,
) {
    val spacing = LocalSpacing.current
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface,
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(spacing.md),
            verticalArrangement = Arrangement.spacedBy(spacing.xs),
        ) {
            CalendarHeader(
                year = year,
                month = month,
                showJumpToday = year != currentYear || month != currentMonth,
                onPrev = onPrev,
                onNext = onNext,
                onJumpToday = onJumpToday,
            )
            WeekdayHeader()
            DayGrid(
                year = year,
                month = month,
                today = today,
                calendar = calendar,
            )
        }
    }
}

// ─── Header ────────────────────────────────────────────────────────────────

@Composable
private fun CalendarHeader(
    year: Int,
    month: Int,
    showJumpToday: Boolean,
    onPrev: () -> Unit,
    onNext: () -> Unit,
    onJumpToday: () -> Unit,
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        IconButton(onClick = onPrev) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowLeft,
                contentDescription = stringResource(R.string.cycle_calendar_prev),
            )
        }
        Text(
            text = formatMonth(year, month),
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Medium,
        )
        IconButton(onClick = onNext) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = stringResource(R.string.cycle_calendar_next),
            )
        }
        if (showJumpToday) {
            TextButton(onClick = onJumpToday) {
                Text(stringResource(R.string.cycle_calendar_jump_today))
            }
        }
    }
}

// ─── Weekday header ────────────────────────────────────────────────────────

/**
 * 7 narrow weekday labels in the locale's first day-of-week order.
 *
 * Re-evaluated on each recomposition (cheap — labels are tiny strings). If
 * we ever introduce user-toggleable language at runtime we'd memoize on
 * `Locale.getDefault()`.
 */
@Composable
private fun WeekdayHeader() {
    val firstDow = firstDayOfWeek()              // 1..7, Sun=1..Sat=7
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween,
    ) {
        for (offset in 0 until 7) {
            val dow = ((firstDow - 1 + offset) % 7) + 1
            Text(
                text = formatWeekdayShort(dow),
                modifier = Modifier.weight(1f),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
        }
    }
}

// ─── Day grid ──────────────────────────────────────────────────────────────

/**
 * Build the 42-cell month grid.
 *
 * **Why plain `Column` + `Row`s instead of `LazyVerticalGrid`?**
 *   Nested `LazyVerticalGrid` (even with `userScrollEnabled = false`) inside
 *   a `LazyColumn` is a known Compose measurement trap: the outer LazyColumn
 *   measures items with `Constraints.Infinity` height, and the inner grid's
 *   subcompose fails. Crashes the app at first composition.
 *   For a fixed 42-cell surface a plain `Column { Row { 7 cells } } × 6`
 *   measures cleanly.
 *
 * **Why one `aspectRatio(7/6)` on the outer Column, none per-cell?**
 *   Putting `aspectRatio(1f)` on each of the 42 cells forces 42 separate
 *   measure passes per layout — slow on a tab-swap. Instead, derive the
 *   whole grid's height from its width in a single pass: aspectRatio(7/6)
 *   gives `height = width * 6/7`, then 6 equally-weighted Rows split that
 *   height, and 7 equally-weighted Boxes per Row split the row width. Each
 *   cell becomes (W/7, W/7) — a perfect square — without re-measuring.
 */
@Composable
private fun DayGrid(
    year: Int,
    month: Int,
    today: Long,
    calendar: Map<Long, CycleDaySummary>,
) {
    // Memoize the structural arithmetic — these are recomputed on every
    // recomposition otherwise, and `calendarDayOf` allocates a Calendar +
    // does a millis roundtrip per call.
    val firstDow = remember { firstDayOfWeek() }
    val gridLayout = remember(year, month) {
        val firstDay = epochDayOfFirstOfMonth(year, month)
        val lastDay = epochDayOfLastOfMonth(year, month)
        val (y0, m0, d0) = calendarDayOf(firstDay)
        val cal = Calendar.getInstance().apply {
            clear()
            set(y0, m0 - 1, d0)
        }
        val firstDowOfMonth = cal.get(Calendar.DAY_OF_WEEK)
        val leadingEmpty = ((firstDowOfMonth - firstDow) + 7) % 7
        GridLayout(firstDay = firstDay, lastDay = lastDay, leadingEmpty = leadingEmpty)
    }

    Column(modifier = Modifier
        .fillMaxWidth()
        .aspectRatio(7f / 6f)
    ) {
        for (rowIdx in 0 until 6) {
            Row(modifier = Modifier
                .fillMaxWidth()
                .weight(1f)
            ) {
                for (colIdx in 0 until 7) {
                    val idx = rowIdx * 7 + colIdx
                    val dayOffset = idx - gridLayout.leadingEmpty
                    Box(modifier = Modifier
                        .weight(1f)
                        .fillMaxHeight()
                    ) {
                        if (dayOffset < 0 || gridLayout.firstDay + dayOffset > gridLayout.lastDay) {
                            BlankDayCell()
                        } else {
                            val day = gridLayout.firstDay + dayOffset
                            DayCell(
                                day = day,
                                isToday = day == today,
                                summary = calendar[day],
                            )
                        }
                    }
                }
            }
        }
    }
}

private data class GridLayout(
    val firstDay: Long,
    val lastDay: Long,
    val leadingEmpty: Int,
)

@Composable
private fun BlankDayCell() {
    Box(modifier = Modifier.fillMaxSize())
}

/**
 * One actual day cell.
 *
 * Composition:
 *   - Aspect-ratio-1 Box, day number centered.
 *   - If today: day number gets a `primary-soft` circular background.
 *   - If a [summary] exists: a colored dot (danger > overdue > upcoming) sits
 *     below the number. When `upcomingCount > 1` we add a tiny count overlay.
 *
 * No click target — overview is read-only (see file doc).
 */
@Composable
private fun DayCell(
    day: Long,
    isToday: Boolean,
    summary: CycleDaySummary?,
) {
    val (_, _, dayOfMonth) = calendarDayOf(day)

    val spacing = LocalSpacing.current
    // Size comes from the parent Box (already a perfect square via the
    // outer Column's aspectRatio(7/6) and the cell's weight/fillMaxHeight
    // chain). No aspectRatio here — that would trigger a per-cell measure
    // pass and add up across the 42 cells.
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(spacing.xxs),
        ) {
            // Today badge: a circular primary-soft background behind the number.
            val numberBgModifier = if (isToday) {
                Modifier
                    .clip(CircleShape)
                    .background(LocalPrimarySoft.current)
                    .padding(horizontal = spacing.xs, vertical = spacing.xxs)
            } else {
                Modifier
            }
            Text(
                text = dayOfMonth.toString(),
                style = MaterialTheme.typography.bodyMedium,
                fontWeight = if (isToday) FontWeight.SemiBold else FontWeight.Normal,
                color = MaterialTheme.colorScheme.onSurface,
                modifier = numberBgModifier,
            )
            if (summary != null && summary.hasAny) {
                Dot(summary = summary)
            }
        }
    }
}

/**
 * The colored dot below the day number. Carries an optional count overlay.
 */
@Composable
private fun Dot(summary: CycleDaySummary) {
    val accent: Color = when {
        summary.hasDanger -> LocalDanger.current
        summary.hasOverdue -> LocalDanger.current
        else -> LocalWarning.current
    }
    val showCount = !summary.hasOverdue && !summary.hasDanger &&
        summary.upcomingCount > 1

    Row(
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(2.dp),
    ) {
        Box(
            modifier = Modifier
                .size(6.dp)
                .clip(CircleShape)
                .background(accent),
        )
        if (showCount) {
            Text(
                text = summary.upcomingCount.toString(),
                style = MaterialTheme.typography.labelSmall,
                color = accent,
            )
        }
    }
}

// ─── Previews ──────────────────────────────────────────────────────────────

@Preview(showBackground = true, name = "Calendar · light · current month")
@Composable
private fun CalendarPreviewLight() {
    val (y, m, _) = calendarDayOf(epochDayOfFirstOfMonth(2026, 8))
    val today = epochDayOf(2026, 8, 17)
    IcyPowerTheme(themeMode = com.kap1bala.icypower.ui.theme.ThemeMode.Light) {
        CycleCalendarCard(
            year = y,
            month = m,
            today = today,
            calendar = mapOf(
                today to CycleDaySummary(day = today, hasOverdue = true, hasDanger = false, upcomingCount = 0),
                epochDayOf(2026, 8, 20) to CycleDaySummary(
                    day = epochDayOf(2026, 8, 20),
                    hasOverdue = false,
                    hasDanger = false,
                    upcomingCount = 3,
                ),
                epochDayOf(2026, 8, 25) to CycleDaySummary(
                    day = epochDayOf(2026, 8, 25),
                    hasOverdue = false,
                    hasDanger = true,
                    upcomingCount = 0,
                ),
            ),
            onPrev = {},
            onNext = {},
            onJumpToday = {},
            currentYear = y,
            currentMonth = m,
            modifier = Modifier.padding(16.dp),
        )
    }
}

@Preview(showBackground = true, name = "Calendar · dark")
@Composable
private fun CalendarPreviewDark() {
    val (y, m, _) = calendarDayOf(epochDayOfFirstOfMonth(2026, 8))
    IcyPowerTheme(themeMode = com.kap1bala.icypower.ui.theme.ThemeMode.Dark) {
        CycleCalendarCard(
            year = y,
            month = m,
            today = epochDayOf(2026, 8, 17),
            calendar = emptyMap(),
            onPrev = {},
            onNext = {},
            onJumpToday = {},
            currentYear = y,
            currentMonth = m,
            modifier = Modifier.padding(16.dp),
        )
    }
}