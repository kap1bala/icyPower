package com.kap1bala.icypower.ui.cycle

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
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
import com.kap1bala.icypower.ui.theme.LocalDanger
import com.kap1bala.icypower.ui.theme.LocalDangerSoft
import com.kap1bala.icypower.ui.theme.LocalSpacing
import com.kap1bala.icypower.ui.theme.LocalWarning
import com.kap1bala.icypower.ui.theme.LocalWarningSoft

/**
 * Settings → 周期设备 list (route /settings/cycle).
 *
 * Rows follow Ant `List` token (ui.md §8.3):
 *   - ListItem with `itemPadding` = 12dp vertical / 16dp horizontal
 *   - Title = titleMedium (16/24 Medium), description = bodySmall onSurfaceVariant
 *   - Trailing icon = chevron-right indicating "drill in"
 *   - Status badge on the right of the meta line when overdue
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CycleDeviceListScreen(
    onBack: () -> Unit,
    onAdd: () -> Unit,
    onEdit: (id: String) -> Unit,
    modifier: Modifier = Modifier,
    viewModel: CycleDeviceListViewModel = viewModel(factory = CycleDeviceListViewModel.Factory),
) {
    val devices by viewModel.devices.collectAsStateWithLifecycle()
    val spacing = LocalSpacing.current

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.cycle_list_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.cd_back),
                        )
                    }
                },
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = onAdd) {
                Icon(
                    imageVector = Icons.Filled.Add,
                    contentDescription = stringResource(R.string.cd_cycle_add),
                )
            }
        },
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        ) {
            if (devices.isEmpty()) {
                Text(
                    text = stringResource(R.string.cycle_list_empty),
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.align(Alignment.Center),
                )
            } else {
                LazyColumn(
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(bottom = spacing.xxl),
                ) {
                    items(items = devices, key = { it.device.id }) { state ->
                        CycleDeviceRow(state = state, onClick = { onEdit(state.device.id) })
                        HorizontalDivider(
                            color = MaterialTheme.colorScheme.outlineVariant,
                            modifier = Modifier.padding(start = spacing.md),
                        )
                    }
                }
            }
        }
    }
}

/**
 * Single device row in the cycle-device list.
 *
 * Mirrors Ant `List.Item`:
 *   - 12dp vertical / 16dp horizontal padding (itemPadding)
 *   - Title (`titleMedium`) + supporting meta (`bodySmall`) + optional status badge
 *   - Trailing chevron-right signals "this row drills in"
 */
@Composable
private fun CycleDeviceRow(
    state: CycleDeviceState,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    ListItem(
        headlineContent = {
            Text(
                text = state.device.name,
                style = MaterialTheme.typography.titleMedium,
            )
        },
        supportingContent = {
            val category = state.device.category
            Text(
                text = buildString {
                    if (!category.isNullOrBlank()) append(category).append(" · ")
                    append(
                        stringResource(
                            R.string.cycle_row_meta,
                            state.device.cycleDays,
                            formatRelativeDays(state.daysSinceLastCharge),
                            formatDisplayDate(state.device.lastChargedAt),
                        ),
                    )
                },
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        },
        trailingContent = {
            if (state.severity != OverdueSeverity.None) {
                OverdueBadge(severity = state.severity)
            } else {
                Icon(
                    imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(20.dp),
                )
            }
        },
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
    )
}

/** Compact overdue/danger badge matching the home-card variant. */
@Composable
private fun OverdueBadge(severity: OverdueSeverity) {
    val spacing = LocalSpacing.current
    val isDanger = severity == OverdueSeverity.Danger
    val accent: Color = if (isDanger) LocalDanger.current else LocalWarning.current
    val container: Color = if (isDanger) LocalDangerSoft.current else LocalWarningSoft.current

    Surface(
        shape = MaterialTheme.shapes.extraSmall,
        color = container,
    ) {
        androidx.compose.foundation.layout.Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.padding(horizontal = spacing.xs, vertical = spacing.xxs),
            horizontalArrangement = Arrangement.spacedBy(spacing.xxs),
        ) {
            Icon(
                imageVector = Icons.Filled.Warning,
                contentDescription = null,
                tint = accent,
                modifier = Modifier.size(12.dp),
            )
            Text(
                text = stringResource(
                    if (isDanger) R.string.cycle_badge_danger
                    else R.string.cycle_badge_warning,
                ),
                style = MaterialTheme.typography.labelSmall,
                color = accent,
            )
        }
    }
}
