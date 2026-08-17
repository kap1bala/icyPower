package com.kap1bala.icypower.ui.home

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.kap1bala.icypower.R
import com.kap1bala.icypower.data.cycle.CycleOverviewStats
import com.kap1bala.icypower.ui.theme.IcyPowerTheme
import com.kap1bala.icypower.ui.theme.LocalDanger
import com.kap1bala.icypower.ui.theme.LocalSpacing
import com.kap1bala.icypower.ui.theme.LocalWarning

/**
 * 4-tile overview strip rendered at the top of the "概览" tab.
 *
 * Layout: one [Row] with 4 equally-weighted [StatTile]s, separated by
 * `spacing.xs`. Each tile is **read-only** (no ripple, no click handler) —
 * the overview tab is a dashboard, not a navigation surface. To drill in,
 * users switch to the "周期设备" tab.
 *
 * Visual contract:
 *   - Card follows `ui.md §8.2` (surface container, 0 elevation, 1dp
 *     outlineVariant border, `shapes.medium` radius, `spacing.md` padding).
 *   - Severity-color the count text. `count == 0` drops the count color to
 *     `onSurfaceVariant` so empty buckets don't shout.
 *   - Color + label gives a triple redundancy (count number, label text,
 *     accent color) per `ui.md §9.3` — even with color-blind users.
 */
@Composable
fun OverviewStripCard(
    stats: CycleOverviewStats,
    modifier: Modifier = Modifier,
) {
    val spacing = LocalSpacing.current
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,        // radius.md (8dp)
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface,
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(spacing.md),
            horizontalArrangement = Arrangement.spacedBy(spacing.xs),
        ) {
            StatTile(
                count = stats.overdueToday,
                label = stringResource(R.string.cycle_overview_overdue_today),
                accent = LocalDanger.current,
                modifier = Modifier.weight(1f),
            )
            StatTile(
                count = stats.dueTomorrow,
                label = stringResource(R.string.cycle_overview_due_tomorrow),
                accent = LocalWarning.current,
                modifier = Modifier.weight(1f),
            )
            StatTile(
                count = stats.dueInNext7,
                label = stringResource(R.string.cycle_overview_due_7),
                accent = LocalWarning.current,
                modifier = Modifier.weight(1f),
            )
            StatTile(
                count = stats.dueInNext30,
                label = stringResource(R.string.cycle_overview_due_30),
                accent = LocalWarning.current,
                modifier = Modifier.weight(1f),
            )
        }
    }
}

/**
 * Single stat tile inside [OverviewStripCard].
 *
 * Vertical Column: bold count on top, small label below. When the count is
 * zero we drop the accent color to `onSurfaceVariant` so the empty bucket
 * doesn't visually compete with non-empty ones.
 */
@Composable
private fun StatTile(
    count: Int,
    label: String,
    accent: Color,
    modifier: Modifier = Modifier,
) {
    val spacing = LocalSpacing.current
    val numberColor = if (count == 0) MaterialTheme.colorScheme.onSurfaceVariant else accent
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(spacing.xxs),
    ) {
        Text(
            text = count.toString(),
            style = MaterialTheme.typography.titleMedium,
            color = numberColor,
            fontWeight = FontWeight.SemiBold,
            textAlign = TextAlign.Center,
        )
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}

// ─── Previews ──────────────────────────────────────────────────────────────

@Preview(showBackground = true, name = "OverviewStrip · light · non-zero")
@Composable
private fun OverviewStripPreview() {
    IcyPowerTheme(themeMode = com.kap1bala.icypower.ui.theme.ThemeMode.Light) {
        OverviewStripCard(
            stats = CycleOverviewStats(
                overdueToday = 2,
                dueTomorrow = 1,
                dueInNext7 = 4,
                dueInNext30 = 9,
            ),
            modifier = Modifier.padding(16.dp),
        )
    }
}

@Preview(showBackground = true, name = "OverviewStrip · dark · all zero")
@Composable
private fun OverviewStripZeroPreview() {
    IcyPowerTheme(themeMode = com.kap1bala.icypower.ui.theme.ThemeMode.Dark) {
        OverviewStripCard(
            stats = CycleOverviewStats.ZERO,
            modifier = Modifier.padding(16.dp),
        )
    }
}