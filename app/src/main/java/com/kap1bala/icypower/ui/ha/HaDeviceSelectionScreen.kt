package com.kap1bala.icypower.ui.ha

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Clear
import androidx.compose.material.icons.filled.Info
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.kap1bala.icypower.R
import com.kap1bala.icypower.data.cycle.OverdueSeverity
import com.kap1bala.icypower.ui.theme.LocalDanger
import com.kap1bala.icypower.ui.theme.LocalRadius
import com.kap1bala.icypower.ui.theme.LocalSpacing
import com.kap1bala.icypower.ui.theme.LocalWarning

/**
 * `/settings/ha/devices` — pick which HA entities to track.
 *
 * The visual rhythm follows [HomeHaPanel] (antd-styled list rows with
 * surface + 1dp outlineVariant) so the two screens look like siblings.
 *
 * First-run auto-fill lives in [HaDeviceSelectionViewModel.bootstrap].
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HaDeviceSelectionScreen(
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
    viewModel: HaDeviceSelectionViewModel = viewModel(factory = HaDeviceSelectionViewModel.Factory),
) {
    val state by viewModel.state.collectAsStateWithLifecycle()

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.ha_devices_title)) },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.cd_back),
                        )
                    }
                },
                actions = {
                    TextButton(
                        onClick = viewModel::selectAll,
                    ) {
                        Text(stringResource(R.string.ha_devices_select_all))
                    }
                    TextButton(
                        onClick = viewModel::clearAll,
                    ) {
                        Text(stringResource(R.string.ha_devices_clear_all))
                    }
                },
            )
        },
    ) { innerPadding ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        ) {
            when (state.phase) {
                HaPhase.NotConfigured -> CenteredIcon(
                    icon = Icons.Filled.Info,
                    tint = MaterialTheme.colorScheme.primary,
                    title = stringResource(R.string.home_ha_not_configured_title),
                    body = stringResource(R.string.home_ha_not_configured_desc),
                )
                HaPhase.Loading -> CenteredLoading()
                HaPhase.Error -> CenteredIcon(
                    icon = Icons.Filled.Clear,
                    tint = LocalDanger.current,
                    title = stringResource(R.string.home_ha_error_title),
                    body = state.errorMessage ?: stringResource(R.string.home_ha_error_desc),
                )
                HaPhase.Unauthorized -> CenteredIcon(
                    icon = Icons.Filled.Info,
                    tint = LocalWarning.current,
                    title = stringResource(R.string.home_ha_unauthorized_title),
                    body = stringResource(R.string.home_ha_unauthorized_desc),
                )
                HaPhase.Empty -> CenteredIcon(
                    icon = Icons.Filled.Info,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    title = stringResource(R.string.home_ha_empty_title),
                    body = stringResource(R.string.home_ha_empty_desc),
                )
                HaPhase.Loaded -> OptionsList(
                    options = state.options,
                    onToggle = viewModel::toggle,
                )
            }
        }
    }
}

// ────────────────────────────────────────────────────────────────────
// Loaded: the actual list
// ────────────────────────────────────────────────────────────────────

@Composable
private fun OptionsList(
    options: List<HaDeviceOption>,
    onToggle: (entityId: String, enabled: Boolean) -> Unit,
) {
    val spacing = LocalSpacing.current
    Column(modifier = Modifier.fillMaxSize()) {
        Text(
            text = stringResource(R.string.ha_devices_subtitle),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            modifier = Modifier.padding(horizontal = spacing.md, vertical = spacing.sm),
        )
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(
                start = spacing.md,
                end = spacing.md,
                top = spacing.xs,
                bottom = spacing.xl,
            ),
            verticalArrangement = Arrangement.spacedBy(spacing.sm),
        ) {
            items(items = options, key = { it.entityId }) { option ->
                OptionRow(option = option, onToggle = onToggle)
            }
        }
    }
}

@Composable
private fun OptionRow(
    option: HaDeviceOption,
    onToggle: (entityId: String, enabled: Boolean) -> Unit,
) {
    val radius = LocalRadius.current
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(radius.md),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface,
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
        border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(2.dp),
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = option.name,
                        style = MaterialTheme.typography.titleMedium,
                        color = MaterialTheme.colorScheme.onSurface,
                        modifier = Modifier.weight(1f, fill = false),
                    )
                    if (option.severity != OverdueSeverity.None) {
                        Spacer(modifier = Modifier.size(6.dp))
                        SeverityDot(severity = option.severity)
                    }
                }
                Text(
                    text = buildString {
                        if (!option.area.isNullOrBlank()) append(option.area).append(" · ")
                        append(option.batteryPercent).append('%')
                    },
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
            Switch(
                checked = option.isMonitored,
                onCheckedChange = { onToggle(option.entityId, it) },
            )
        }
    }
}

@Composable
private fun SeverityDot(severity: OverdueSeverity) {
    val color: Color = when (severity) {
        OverdueSeverity.Danger -> LocalDanger.current
        OverdueSeverity.Warning -> LocalWarning.current
        OverdueSeverity.None -> MaterialTheme.colorScheme.onSurfaceVariant
    }
    Box(
        modifier = Modifier
            .size(8.dp)
            .background(color, CircleShape),
    )
}

// ────────────────────────────────────────────────────────────────────
// Shared state panes (Loading / Error / Empty / NotConfigured / Unauthorized)
// ────────────────────────────────────────────────────────────────────

@Composable
private fun CenteredIcon(
    icon: ImageVector,
    tint: Color,
    title: String,
    body: String,
) {
    val spacing = LocalSpacing.current
    Box(
        modifier = Modifier
            .fillMaxSize()
            .padding(horizontal = spacing.lg),
        contentAlignment = Alignment.Center,
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = tint,
                modifier = Modifier.size(48.dp),
            )
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
                modifier = Modifier.padding(top = spacing.md),
            )
            Text(
                text = body,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                textAlign = TextAlign.Center,
                modifier = Modifier.padding(top = spacing.xs),
            )
        }
    }
}

@Composable
private fun CenteredLoading() {
    val spacing = LocalSpacing.current
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(spacing.lg),
        verticalArrangement = Arrangement.spacedBy(spacing.md),
    ) {
        repeat(3) {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(8.dp),
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surface,
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 0.dp),
                border = BorderStroke(1.dp, MaterialTheme.colorScheme.outlineVariant),
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(60.dp)
                        .background(MaterialTheme.colorScheme.outlineVariant),
                )
            }
        }
    }
}