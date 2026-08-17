package com.kap1bala.icypower.ui.home

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.FilledTonalButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
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
import com.kap1bala.icypower.ui.theme.LocalWarning

/**
 * Home screen — Tab A "周期设备" panel.
 *
 * Reuses [CycleDeviceListViewModel] (also used by the settings-screen list)
 * so the same DataStore-backed list renders consistently across both surfaces.
 */
@Composable
fun HomeCycleDevicesPanel(
    contentPadding: PaddingValues,
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: CycleDeviceListViewModel = viewModel(factory = CycleDeviceListViewModel.Factory),
) {
    val devices by viewModel.devices.collectAsStateWithLifecycle()

    if (devices.isEmpty()) {
        Box(
            modifier = modifier
                .fillMaxSize()
                .padding(contentPadding),
            contentAlignment = Alignment.Center,
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    text = stringResource(R.string.home_empty_cycle),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
                FilledTonalButton(
                    onClick = onOpenSettings,
                    modifier = Modifier.padding(top = 16.dp),
                ) {
                    Text(stringResource(R.string.settings_cycle_devices))
                }
            }
        }
        return
    }

    LazyColumn(
        modifier = modifier.fillMaxSize(),
        contentPadding = PaddingValues(
            start = 16.dp,
            end = 16.dp,
            top = contentPadding.calculateTopPadding() + 16.dp,
            bottom = contentPadding.calculateBottomPadding() + 24.dp,
        ),
        verticalArrangement = Arrangement.spacedBy(16.dp),
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
    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant,
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp),
    ) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
            ) {
                Text(
                    text = state.device.name,
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.weight(1f, fill = false),
                )
                if (state.severity != OverdueSeverity.None) {
                    val isDanger = state.severity == OverdueSeverity.Danger
                    Text(
                        text = stringResource(
                            if (isDanger) R.string.home_badge_severely_overdue
                            else R.string.home_badge_overdue,
                        ),
                        style = MaterialTheme.typography.labelSmall,
                        color = if (isDanger) LocalDanger.current else LocalWarning.current,
                    )
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
            FilledTonalButton(
                onClick = onMarkCharged,
                modifier = Modifier.fillMaxWidth(),
            ) {
                Text(stringResource(R.string.home_card_mark_charged))
            }
        }
    }
}
