package com.kap1bala.icypower.ui.settings

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.kap1bala.icypower.R

/**
 * Settings index (route /settings).
 *
 * List rows mirror ui.md §8.3 (`List.Item`):
 *   - 12dp vertical / 16dp horizontal padding (itemPadding default)
 *   - Title = titleMedium (16/24 Medium); subtitle = bodySmall onSurfaceVariant
 *   - Trailing chevron-right indicates "this row drills in"
 *
 * Currently exposes:
 *   - Appearance (theme mode picker)
 *   - Cycle devices (manual charge-cycle tracking)
 *
 * More entries (HA connection, alert rules, quiet hours, data management)
 * will land here in their own PRs — see feat.md §2.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsScreen(
    onBack: () -> Unit,
    onOpenAppearance: () -> Unit,
    onOpenCycleDevices: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.settings_title)) },
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
    ) { innerPadding ->
        // verticalScroll keeps the page usable once we add more entries
        // (HA connection, alert rules, quiet hours, data management).
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding)
                .verticalScroll(rememberScrollState()),
        ) {
            SettingsListItem(
                title = stringResource(R.string.settings_appearance),
                subtitle = stringResource(R.string.settings_appearance_subtitle),
                onClick = onOpenAppearance,
            )
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

            SettingsListItem(
                title = stringResource(R.string.settings_cycle_devices),
                subtitle = stringResource(R.string.settings_cycle_devices_subtitle),
                onClick = onOpenCycleDevices,
            )
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)
        }
    }
}

/**
 * One settings row.
 *
 * Pulled out so both `Appearance` and `Cycle devices` use identical antd-style
 * row visuals — avoids drift between rows. ui.md §8.3 governs the layout.
 *
 * Modifier note: [ListItem] is a Surface-backed composable. We previously
 * chained `fillMaxSize()` on it, which inside a plain Column propagates a
 * vertical Infinity constraint to children. The first row claimed that
 * Infinity height, the second collapsed to 0 and rendered as a thin line —
 * effectively invisible. `fillMaxWidth()` keeps the row at content height
 * while spanning screen width.
 */
@Composable
private fun SettingsListItem(
    title: String,
    subtitle: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
) {
    ListItem(
        headlineContent = {
            Text(
                text = title,
                style = MaterialTheme.typography.titleMedium,
            )
        },
        supportingContent = {
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        },
        trailingContent = {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.onSurfaceVariant,
                modifier = Modifier.padding(start = 8.dp),
            )
        },
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick),
        colors = ListItemDefaults.colors(containerColor = Color.Transparent),
    )
}

