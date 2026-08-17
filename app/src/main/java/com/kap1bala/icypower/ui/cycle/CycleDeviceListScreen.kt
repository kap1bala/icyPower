package com.kap1bala.icypower.ui.cycle

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
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
import com.kap1bala.icypower.ui.theme.LocalDanger
import com.kap1bala.icypower.ui.theme.LocalWarning

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
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.align(Alignment.Center),
                )
            } else {
                Column(modifier = Modifier.fillMaxSize()) {
                    devices.forEach { state ->
                        CycleDeviceRow(
                            state = state,
                            onClick = { onEdit(state.device.id) },
                        )
                        HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
                    }
                }
            }
        }
    }
}

@Composable
private fun CycleDeviceRow(
    state: CycleDeviceState,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(4.dp),
    ) {
        Text(
            text = state.device.name,
            style = MaterialTheme.typography.titleMedium,
        )
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
        if (state.severity != OverdueSeverity.None) {
            Text(
                text = stringResource(
                    if (state.severity == OverdueSeverity.Danger) R.string.cycle_badge_danger
                    else R.string.cycle_badge_warning,
                ),
                style = MaterialTheme.typography.labelSmall,
                color = if (state.severity == OverdueSeverity.Danger)
                    LocalDanger.current
                else
                    LocalWarning.current,
            )
        }
    }
}
