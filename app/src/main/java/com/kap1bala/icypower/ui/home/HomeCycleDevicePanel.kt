package com.kap1bala.icypower.ui.home

import android.widget.Toast
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
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.IntSize
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.kap1bala.icypower.R
import com.kap1bala.icypower.data.cycle.CycleDeviceState
import com.kap1bala.icypower.data.cycle.OverdueSeverity
import com.kap1bala.icypower.ui.cycle.AllClearEvent
import com.kap1bala.icypower.ui.cycle.ConfettiOverlay
import com.kap1bala.icypower.ui.cycle.ConfettiState
import com.kap1bala.icypower.ui.cycle.CycleDeviceListViewModel
import com.kap1bala.icypower.ui.cycle.formatDisplayDate
import com.kap1bala.icypower.ui.cycle.formatRelativeDays
import com.kap1bala.icypower.ui.cycle.isChargedToday
import com.kap1bala.icypower.ui.theme.LocalDanger
import com.kap1bala.icypower.ui.theme.LocalSpacing
import com.kap1bala.icypower.ui.theme.LocalSuccess
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
 * Today's charged behaviour:
 *   - Devices whose [CycleDevice.lastChargedAt] falls on today's calendar
 *     day get a small "今日已充电" green chip next to the title and
 *     have their "已充电" button disabled — pressing again would be a
 *     no-op anyway, and the disabled state gives the user immediate
 *     visual feedback that the action is done.
 *
 * All-clear celebration:
 *   - When the last overdue device gets [CycleDeviceListViewModel.markCharged],
 *     the view model emits an [AllClearEvent] on a one-shot SharedFlow.
 *     The panel collects that, fires [Toast] and emits a [ConfettiState]
 *     burst. The state survives re-composition and re-emit (the user
 *     might re-open the home tab and trigger again).
 *
 * Reuses [CycleDeviceListViewModel] (also used by the settings list) so
 * the DataStore-backed list renders identically and stays in sync.
 */
@Composable
fun HomeCycleDevicesPanel(
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: CycleDeviceListViewModel = viewModel(factory = CycleDeviceListViewModel.Factory),
) {
    val devices by viewModel.devices.collectAsStateWithLifecycle()
    val spacing = LocalSpacing.current
    val context = LocalContext.current
    val confetti = remember { ConfettiState() }
    var confettiSize by remember { mutableStateOf(IntSize.Zero) }
    // Confetti palette resolved at the Composable scope so emit() can
    // stay non-Composable. The colors come from antd tokens (LocalSuccess
    // / LocalWarning) + the project's chart series — all theme-aware so
    // dark mode flips automatically.
    val confettiPalette = listOf(
        LocalSuccess.current,
        LocalWarning.current,
        com.kap1bala.icypower.ui.theme.ChartSeries1,
        com.kap1bala.icypower.ui.theme.ChartSeries2,
        com.kap1bala.icypower.ui.theme.ChartSeries3,
        com.kap1bala.icypower.ui.theme.ChartSeries4,
    )

    // One-shot celebration: AllDevicesCharged → Toast + confetti burst.
    LaunchedEffect(viewModel) {
        viewModel.allClearEvents.collect { event ->
            when (event) {
                is AllClearEvent.AllDevicesCharged -> {
                    Toast.makeText(
                        context,
                        context.getString(R.string.cycle_all_clear_toast),
                        Toast.LENGTH_SHORT,
                    ).show()
                    if (confettiSize != IntSize.Zero) {
                        confetti.emit(confettiSize, confettiPalette, perSide = 60)
                    } else {
                        // Canvas not measured yet (very fast first tap).
                        // Defer to the next frame when size becomes known;
                        // simplest: just retry with IntSize.Zero and let
                        // the user see particles spawn from origin (0,0)
                        // for one frame, then settle. In practice the
                        // overlay's first LaunchedEffect pass runs before
                        // the click handler reaches this branch.
                        confetti.particles.clear()
                    }
                }
            }
        }
    }

    if (devices.isEmpty()) {
        Box(
            modifier = modifier.fillMaxSize(),
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
        ConfettiOverlay(state = confetti, modifier = Modifier.fillMaxSize())
        return
    }

    Box(modifier = modifier.fillMaxSize()) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            // The hosting HomeScreen Column already consumed Scaffold.innerPadding
            // (top + bottom system insets) — so the list can render full-bleed
            // and only needs its own design rhythm.
            contentPadding = PaddingValues(
                start = spacing.md,
                end = spacing.md,
                top = spacing.md,
                bottom = spacing.xl,
            ),
            verticalArrangement = Arrangement.spacedBy(spacing.md),
        ) {
            items(items = devices, key = { it.device.id }) { state ->
                HomeCycleDeviceCard(
                    state = state,
                    isChargedToday = isChargedToday(state.device.lastChargedAt),
                    onMarkCharged = { viewModel.markCharged(state.device.id) },
                )
            }
        }
        // Confetti overlay sits on top of the LazyColumn. The cards
        // retain input — the overlay has no `pointerInput` modifier, so
        // taps fall through to the cards beneath.
        ConfettiOverlay(
            state = confetti,
            modifier = Modifier.fillMaxSize(),
            onSize = { confettiSize = it },
        )
    }
}

@Composable
private fun HomeCycleDeviceCard(
    state: CycleDeviceState,
    isChargedToday: Boolean,
    onMarkCharged: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val spacing = LocalSpacing.current

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
                // Right side of the title row: overdue badge OR
                // "charged today" chip. Never both — when the user has
                // just charged the device, severity drops to None and
                // the chip replaces the badge.
                when {
                    isChargedToday -> ChargedTodayChip()
                    state.severity != OverdueSeverity.None -> OverdueBadge(state.severity)
                    else -> Unit  // no badge — fresh device, no ceremony
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
                // Material3 renders `enabled = false` as onSurface @ 38% —
                // a tonal greying that matches the antd 暗色 / 弱化 pattern
                // (feat.md §10). We don't customise the color.
                enabled = !isChargedToday,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(stringResource(R.string.home_card_mark_charged))
            }
        }
    }
}

/**
 * "今日已充电" green chip — placed in the title row when the device
 * was charged today. Mirrors the [OverdueBadge] visual language
 * (soft container + icon + text) so the eye picks up "status changed"
 * without re-anchoring to a new corner of the card.
 */
@Composable
private fun ChargedTodayChip() {
    val spacing = LocalSpacing.current
    Surface(
        shape = MaterialTheme.shapes.extraSmall,
        color = com.kap1bala.icypower.ui.theme.LocalSuccessSoft.current,
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = spacing.xs, vertical = spacing.xxs),
            horizontalArrangement = Arrangement.spacedBy(spacing.xxs),
        ) {
            Icon(
                imageVector = Icons.Filled.Check,
                contentDescription = stringResource(R.string.cycle_charged_today_cd),
                tint = LocalSuccess.current,
                modifier = Modifier.size(12.dp),
            )
            Text(
                text = stringResource(R.string.cycle_charged_today),
                style = MaterialTheme.typography.labelSmall,
                color = LocalSuccess.current,
            )
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