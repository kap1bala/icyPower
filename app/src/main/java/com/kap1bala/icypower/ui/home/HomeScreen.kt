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
 * Home screen — TopAppBar + TabRow + empty-state placeholders.
 *
 * The cards proper (device name, last charged, current level) are deferred
 * to a follow-up PR; this v1 only proves the navigation skeleton, theme
 * switching, and persistence plumbing end-to-end.
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

            // Divider between TabRow and the content area. PrimaryTabRow has
            // its own internal indicator, but the seam between TabRow and
            // content still benefits from an explicit outlineVariant line in
            // dark mode (both share colorScheme.surface otherwise).
            HorizontalDivider(color = MaterialTheme.colorScheme.outlineVariant)

            Box(
                modifier = Modifier.fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) {
                Text(
                    text = stringResource(
                        if (selectedTab == TAB_CYCLE) R.string.home_empty_cycle
                        else R.string.home_empty_ha,
                    ),
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}
