package com.kap1bala.icypower.ui.home

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.kap1bala.icypower.R
import com.kap1bala.icypower.data.cycle.CycleDeviceState
import com.kap1bala.icypower.data.cycle.OverdueSeverity
import com.kap1bala.icypower.ui.cycle.CycleDeviceListViewModel
import com.kap1bala.icypower.ui.cycle.formatDisplayDate
import com.kap1bala.icypower.ui.cycle.formatRelativeDays
import com.kap1bala.icypower.ui.theme.LocalDanger
import com.kap1bala.icypower.ui.theme.LocalRadius
import com.kap1bala.icypower.ui.theme.LocalSpacing
import com.kap1bala.icypower.ui.theme.LocalWarning

/**
 * Home screen — Tab A "周期设备" panel.
 *
 * Cards follow ui.md §8.2 ("Card / 在 Layer 1 页面上的卡片"):
 *   - containerColor = surface (Layer 2)
 *   - 1dp border from outlineVariant
 *   - radius = `radius.md` (8dp)
 *   - inner padding 16dp; intra-card field spacing 8dp
 *
 * "已充电" button is the row's primary action (single CTA per card),
 * styled as a filled [Button] in the brand primary — see ui.md §8.1
 * "1 primary button + n secondary buttons".
 *
 * Empty state uses an [Empty]-style column with a primary CTA pointing
 * to the settings screen (ui.md §8.5: optimistic guidance + clear action).
 *
 * Reuses [CycleDeviceListViewModel] (also used by the settings list) so
 * the DataStore-backed list renders identically and stays in sync.
 */
@Composable
fun HomeCycleDevicesPanel(
    contentPadding: PaddingValues,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: CycleDeviceListViewModel = viewModel(factory = CycleDeviceListViewModel.Factory),
) {
    val devices by viewModel.devices.collectAsStateWithLifecycle()
    val spacing = LocalSpacing.current

    if (devices.isEmpty()) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .padding(contentPadding),
            contentAlignment = Alignment.Center,
        ) {
            Column(
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.spacedBy(spacing.lg),
            ) {
                Text(
                    text = stringResource(R.string.home_empty_cycle),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                Button(onClick = onOpenSettings) {
                    Text(stringResource(R.string.settings_cycle_devices))
                }
            }
        }
        return
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            start = spacing.md,
            end = spacing.md,
            top = contentPadding.calculateTopPadding() + spacing.md,
            bottom = contentPadding.calculateBottomPadding() + spacing.xl,
        ),
        verticalArrangement = Arrangement.spacedBy(spacing.md),
    ) {
        items(items = devices, key = { it.device.id }) { state ->
            HomeCycleDeviceCard(
                state = state,
                onMarkCharged = { viewModel.markCharged(state.device.id) },
            )
        }
    }
}

@Composable
private fun HomeCycleDeviceCard(
    state: CycleDeviceState,
    onMarkCharged: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val spacing = LocalSpacing.current
    val radius = LocalRadius.current

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = MaterialTheme.shapes.medium,        // = radius.md (8dp)
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface,
        ),
        elevation = CardDefaults.cardElevation(
            defaultElevation = 0.dp,                  // border gives separation
        ),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Column(
            modifier = Modifier.padding(spacing.md),
            verticalArrangement = Arrangement.spacedBy(spacing.xs),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = state.device.name,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.weight(1f, fill = false),
                )
                if (state.severity != OverdueSeverity.None) {
                    OverdueBadge(state.severity)
                }
            }
            val category = state.device.category
            if (!category.isNullOrBlank()) {
                Text(
                    text = category,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Text(
                text = stringResource(
                    R.string.cycle_row_meta,
                    state.device.cycleDays,
                    formatRelativeDays(state.daysSinceLastCharge),
                    formatDisplayDate(state.device.lastChargedAt),
                ),
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Button(
                onClick = onMarkCharged,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(stringResource(R.string.home_card_mark_charged))
            }
        }
    }
}

/**
 * Status badge per ui.md §8.4. Uses *soft* container for the chip background
 * (low visual weight) and the strong color for icon + label (high contrast).
 * Must keep color + icon + text triple redundancy (feat.md §5.9 / WCAG).
 */
@Composable
private fun OverdueBadge(severity: OverdueSeverity) {
    val spacing = LocalSpacing.current
    val isDanger = severity == OverdueSeverity.Danger
    val accent: Color = if (isDanger) LocalDanger.current else LocalWarning.current
    val container: Color = if (isDanger)
        com.kap1bala.icypower.ui.theme.LocalDangerSoft.current
    else
        com.kap1bala.icypower.ui.theme.LocalWarningSoft.current

    Surface(
        shape = MaterialTheme.shapes.extraSmall,    // = radius.xs (4dp)
        color = container,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = spacing.xs, vertical = spacing.xxs),
            horizontalArrangement = Arrangement.spacedBy(spacing.xxs),
        ) {
            Icon(
                imageVector = Icons.Filled.Warning,
                contentDescription = null, // text below conveys same meaning
                tint = accent,
                modifier = Modifier.size(12.dp),
            )
            Text(
                text = stringResource(
                    if (isDanger) R.string.home_badge_severely_overdue
                    else R.string.home_badge_overdue,
                ),
                style = MaterialTheme.typography.labelSmall,
                color = accent,
            )
        }
    }
}
