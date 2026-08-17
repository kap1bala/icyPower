package com.kap1bala.icypower.ui.home

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import com.kap1bala.icypower.R
import com.kap1bala.icypower.ui.theme.LocalSpacing

private const val TAB_OVERVIEW = 0
private const val TAB_CYCLE = 1
private const val TAB_HA = 2
private const val TAB_COUNT = 3

/**
 * Home screen — TopAppBar + swipeable TabRow + per-tab panel.
 *
 * Tab state model:
 *   - [selectedTab] is the source of truth for the TabRow.
 *   - [pagerState] drives a [HorizontalPager] that hosts the three panels.
 *   - The two are bidirectionally synced via [LaunchedEffect] — swiping
 *     the pager updates `selectedTab`; tapping a Tab animates the pager
 *     to that page.
 *
 * Padding strategy:
 *   - The outer [Column] consumes [Scaffold]'s `innerPadding` so the TabRow
 *     sits flush under the TopAppBar and the pager doesn't extend past the
 *     gesture / navigation bar.
 *   - The pager pages render full-bleed — they must NOT re-apply
 *     `innerPadding` (we hit that bug earlier; see commit 800ca96).
 *
 * - Tab 0 "概览" ([HomeOverviewPanel]): stats strip + month calendar (read-only).
 * - Tab 1 "周期设备" ([HomeCycleDevicesPanel]): per-device cards with "已充电" action.
 * - Tab 2 "HA 设备": still a no-op placeholder. The real HA client
 *   (OkHttp REST + WS per `prompts/ha.md`) is wired in IcyPowerApp
 *   but the UI layer for it is planned for the next PR.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun HomeScreen(
    onOpenSettings: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var selectedTab by rememberSaveable { mutableIntStateOf(TAB_OVERVIEW) }

    val pagerState = rememberPagerState(
        initialPage = selectedTab.coerceIn(0, TAB_COUNT - 1),
        pageCount = { TAB_COUNT },
    )

    // swipe → drive the indicator. Only sync AFTER the scroll settles; during
    // an animation from 0→2 `currentPage` briefly reports 1, and mirroring
    // that into selectedTab would cancel `animateScrollToPage(2)` mid-flight
    // and leave the pager stuck at page 1.
    LaunchedEffect(pagerState.currentPage) {
        if (!pagerState.isScrollInProgress && pagerState.currentPage != selectedTab) {
            selectedTab = pagerState.currentPage
        }
    }
    // tap a tab → animate the pager; guard against feedback loops
    LaunchedEffect(selectedTab) {
        if (!pagerState.isScrollInProgress && pagerState.currentPage != selectedTab) {
            pagerState.animateScrollToPage(selectedTab)
        }
    }

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
                    selected = selectedTab == TAB_OVERVIEW,
                    onClick = { selectedTab = TAB_OVERVIEW },
                    text = { Text(stringResource(R.string.tab_overview)) },
                )
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

            HorizontalPager(
                state = pagerState,
                modifier = Modifier.fillMaxSize(),
            ) { page ->
                when (page) {
                    TAB_OVERVIEW -> HomeOverviewPanel(
                        onOpenSettings = onOpenSettings,
                        modifier = Modifier.fillMaxSize(),
                    )
                    TAB_CYCLE -> HomeCycleDevicesPanel(
                        onOpenSettings = onOpenSettings,
                    )
                    TAB_HA -> EmptyHaPanel(modifier = Modifier.fillMaxSize())
                }
            }
        }
    }
}

@Composable
private fun EmptyHaPanel(modifier: Modifier = Modifier) {
    val spacing = LocalSpacing.current
    Box(
        modifier = modifier.padding(horizontal = spacing.lg),
        contentAlignment = Alignment.Center,
    ) {
        Text(
            text = stringResource(R.string.home_empty_ha),
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            textAlign = TextAlign.Center,
        )
    }
}
