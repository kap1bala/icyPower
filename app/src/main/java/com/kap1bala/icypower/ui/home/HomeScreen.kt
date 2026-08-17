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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import com.kap1bala.icypower.R
import com.kap1bala.icypower.ui.theme.LocalSpacing
import kotlinx.coroutines.launch

private const val TAB_OVERVIEW = 0
private const val TAB_CYCLE = 1
private const val TAB_HA = 2
private const val TAB_COUNT = 3

/**
 * Home screen — TopAppBar + swipeable TabRow + per-tab panel.
 *
 * **Single source of truth: `pagerState.currentPage`.**
 * The indicator binds directly to `pagerState.currentPage` (which reports
 * the last-visible page during scroll, so the indicator follows the
 * finger). Tab taps launch `pagerState.animateScrollToPage(target)` in a
 * coroutine scope — no separate `selectedTab` state, no bidirectional
 * sync, no feedback loop.
 *
 * Why not a separate `selectedTab` state?
 *   - Two `LaunchedEffect`s keeping `selectedTab` and `pagerState.currentPage`
 *     in sync fight each other: tap → `animateScrollToPage` starts → its
 *     intermediate `currentPage` reports mirror back into `selectedTab`,
 *     the second effect re-fires and cancels the animation mid-flight.
 *   - `pagerState` is `rememberSaveable` natively, so config changes still
 *     restore the last-visible tab.
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
    val pagerState = rememberPagerState(pageCount = { TAB_COUNT })
    val scope = rememberCoroutineScope()

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
            PrimaryTabRow(selectedTabIndex = pagerState.currentPage) {
                Tab(
                    selected = pagerState.currentPage == TAB_OVERVIEW,
                    onClick = { scope.launch { pagerState.animateScrollToPage(TAB_OVERVIEW) } },
                    text = { Text(stringResource(R.string.tab_overview)) },
                )
                Tab(
                    selected = pagerState.currentPage == TAB_CYCLE,
                    onClick = { scope.launch { pagerState.animateScrollToPage(TAB_CYCLE) } },
                    text = { Text(stringResource(R.string.tab_cycle_devices)) },
                )
                Tab(
                    selected = pagerState.currentPage == TAB_HA,
                    onClick = { scope.launch { pagerState.animateScrollToPage(TAB_HA) } },
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
