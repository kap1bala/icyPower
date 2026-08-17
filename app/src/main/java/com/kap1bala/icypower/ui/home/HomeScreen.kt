package com.kap1bala.icypower.ui.home

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.PrimaryTabRow
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Tab
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import com.kap1bala.icypower.R

private const val TAB_CYCLE = 0
private const val TAB_HA = 1

/**
 * Home screen — TopAppBar + TabRow + per-tab panel.
 *
 * Padding strategy:
 *   - The outer [Column] consumes [Scaffold]'s `innerPadding` so the TabRow
 *     sits flush under the TopAppBar and the Box doesn't extend past the
 *     gesture / navigation bar.
 *   - Inside the Box, panels render full-bleed. They must **not** re-apply
 *     `innerPadding` — that was the source of a duplicate top-spacing bug
 *     where the first cycle-device card had a TopAppBar-height sized gap
 *     above it.
 *
 * - Tab A "周期设备": [HomeCycleDevicesPanel] — wires to [com.kap1bala.icypower.ui.cycle.CycleDeviceListViewModel]
 *   (shared with the settings-list screen; same DataStore, one truth).
 * - Tab B "HA 设备": still a no-op placeholder. The real HA client
 *   (OkHttp REST + WS per `prompts/ha.md`) is planned for a follow-up PR.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var selectedTab by rememberSaveable { mutableIntStateOf(TAB_CYCLE) }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text(stringResource(R.string.nav_home)) },
                actions = {
                    IconButton(onClick = onOpenSettings) {
                        Icon(
                            imageVector = Icons.Filled.Settings,
                            contentDescription = stringResource(R.string.cd_settings),
                        )
                    }
                },
            )
        },
    ) { innerPadding ->
        androidx.compose.foundation.layout.Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(innerPadding),
        ) {
            PrimaryTabRow(selectedTabIndex = selectedTab) {
                Tab(
                    selected = selectedTab == TAB_CYCLE,
                    onClick = { selectedTab = TAB_CYCLE },
                    text = { Text(stringResource(R.string.tab_cycle_devices)) },
                )
                Tab(
                    selected = selectedTab == TAB_HA,
                    onClick = { selectedTab = TAB_HA },
                    text = { Text(stringResource(R.string.tab_ha_devices)) },
                )
            }

            // PrimaryTabRow has its own indicator; outlineVariant line keeps
            // the seam between TabRow and content legible in dark mode where
            // both default to colorScheme.surface.
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

            Box(modifier = Modifier.fillMaxSize()) {
                when (selectedTab) {
                    TAB_CYCLE -> HomeCycleDevicesPanel(
                        onOpenSettings = onOpenSettings,
                    )
                    TAB_HA -> EmptyHaPanel(
                        modifier = Modifier.fillMaxSize(),
                    )
                }
            }
        }
    }
}

@Composable
private fun EmptyHaPanel(modifier: Modifier = Modifier) {
    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = stringResource(R.string.home_empty_ha),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}
