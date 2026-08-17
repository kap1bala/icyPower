package com.kap1bala.icypower.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.Button
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.kap1bala.icypower.R
import com.kap1bala.icypower.data.cycle.calendarDayOf
import com.kap1bala.icypower.data.cycle.stepMonth
import com.kap1bala.icypower.data.cycle.todayEpochDay
import com.kap1bala.icypower.ui.cycle.CycleDeviceListViewModel
import com.kap1bala.icypower.ui.theme.LocalSpacing

/**
 * Top-level panel for the "概览" tab (Tab 0 on home screen).
 *
 * Composes:
 *   - [OverviewStripCard] — 4 stat tiles (read-only).
 *   - [CycleCalendarCard] — month grid with per-day dots; owns month-nav state.
 *   - Empty state — when the user has zero cycle devices, both widgets would
 *     be empty and meaningless; show an optimistic guide pointing to the
 *     settings page instead.
 *
 * Read-only contract: the overview tab never mutates user data. All filter /
 * drill-in interactions would happen in the "周期设备" tab. (See the plan file
 * `woolly-dazzling-llama.md` §6 rationale.)
 *
 * Padding contract (matches `HomeCycleDevicesPanel`):
 *   - We do NOT consume `Scaffold.innerPadding` — `HomeScreen`'s outer
 *     `Column.padding(innerPadding)` already did, so the LazyColumn renders
 *     full-bleed and only adds its own design rhythm.
 */
@Composable
fun HomeOverviewPanel(
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: CycleDeviceListViewModel = viewModel(factory = CycleDeviceListViewModel.Factory),
) {
    val overview by viewModel.overview.collectAsStateWithLifecycle()
    val spacing = LocalSpacing.current

    // "Today" is captured once for this composition. (For v1 we don't tick
    // through midnight — the next recomposition driven by repository updates
    // will refresh everything.)
    val today = remember { todayEpochDay() }
    val (currentYear, currentMonth, _) = remember(today) { calendarDayOf(today) }

    // Month navigation state. Initialized to current month. `rememberSaveable`
    // so configuration changes (rotation) don't reset the user's month view.
    var viewYear by rememberSaveable { mutableIntStateOf(currentYear) }
    var viewMonth by rememberSaveable { mutableIntStateOf(currentMonth) }

    val isEmpty = overview.stats.overdueToday == 0 &&
        overview.stats.dueTomorrow == 0 &&
        overview.stats.dueInNext7 == 0 &&
        overview.stats.dueInNext30 == 0 &&
        overview.calendar.isEmpty()

    if (isEmpty) {
        OverviewEmpty(modifier = modifier, onOpenSettings = onOpenSettings)
        return
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            start = spacing.md,
            end = spacing.md,
            top = spacing.md,
            bottom = spacing.xl,
        ),
        verticalArrangement = Arrangement.spacedBy(spacing.md),
    ) {
        item(key = "stats") {
            OverviewStripCard(stats = overview.stats)
        }
        item(key = "calendar") {
            CycleCalendarCard(
                year = viewYear,
                month = viewMonth,
                today = today,
                calendar = overview.calendar,
                onPrev = {
                    val (y, m) = stepMonth(viewYear, viewMonth, -1)
                    viewYear = y
                    viewMonth = m
                },
                onNext = {
                    val (y, m) = stepMonth(viewYear, viewMonth, 1)
                    viewYear = y
                    viewMonth = m
                },
                onJumpToday = {
                    viewYear = currentYear
                    viewMonth = currentMonth
                },
                currentYear = currentYear,
                currentMonth = currentMonth,
            )
        }
    }
}

@Composable
private fun OverviewEmpty(
    modifier: Modifier,
    onOpenSettings: () -> Unit,
) {
    val spacing = LocalSpacing.current
    Box(
        modifier = modifier
            .fillMaxSize()
            .padding(horizontal = spacing.lg),
        contentAlignment = Alignment.Center,
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(spacing.lg),
        ) {
            Text(
                text = stringResource(R.string.cycle_overview_empty),
                style = MaterialTheme.typography.bodyLarge,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
            )
            Button(onClick = onOpenSettings) {
                Text(stringResource(R.string.settings_cycle_devices))
            }
        }
    }
}