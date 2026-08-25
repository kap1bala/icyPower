package com.kap1bala.icypower.ui.home

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertCountEquals
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import com.kap1bala.icypower.data.cycle.CycleOverviewStats
import com.kap1bala.icypower.ui.theme.IcyPowerTheme
import com.kap1bala.icypower.ui.theme.ThemeMode
import org.junit.Rule
import org.junit.Test

/**
 * Compose UI tests for [OverviewStripCard].
 *
 * The component is a pure read-only presentation surface (no VM, no I/O),
 * which keeps these tests focused on the visual contract:
 *  - 4 tiles always render with the correct counts and labels.
 *  - Clicking a tile is a no-op (it's a dashboard, not a navigation entry).
 *  - Counts render the value verbatim; labels come from `R.string`.
 */
class OverviewStripCardTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    @Test
    fun rendersAllFourTilesWithCountsAndLabels() {
        composeTestRule.setContent {
            IcyPowerTheme(themeMode = ThemeMode.Light) {
                OverviewStripCard(
                    stats = CycleOverviewStats(
                        overdueToday = 2,
                        dueTomorrow = 1,
                        dueInNext7 = 4,
                        dueInNext30 = 9,
                    ),
                )
            }
        }

        // Counts
        composeTestRule.onNodeWithText("2").assertIsDisplayed()
        composeTestRule.onNodeWithText("1").assertIsDisplayed()
        composeTestRule.onNodeWithText("4").assertIsDisplayed()
        composeTestRule.onNodeWithText("9").assertIsDisplayed()
        // Labels — all four labels visible at once.
        composeTestRule.onNodeWithText("今日已超期").assertIsDisplayed()
        composeTestRule.onNodeWithText("明天需充电").assertIsDisplayed()
        composeTestRule.onNodeWithText("未来 7 天需充电").assertIsDisplayed()
        composeTestRule.onNodeWithText("未来 30 天需充电").assertIsDisplayed()
    }

    @Test
    fun allZeroStats_rendersZeroCountsAndLabels() {
        composeTestRule.setContent {
            IcyPowerTheme(themeMode = ThemeMode.Light) {
                OverviewStripCard(stats = CycleOverviewStats.ZERO)
            }
        }

        // Zero counts. The four "0" labels collide — use the count of nodes
        // matching "0" to confirm all four tiles render their value.
        composeTestRule.onAllNodes(hasText("0")).assertCountEquals(4)
        composeTestRule.onNodeWithText("今日已超期").assertIsDisplayed()
        composeTestRule.onNodeWithText("未来 30 天需充电").assertIsDisplayed()
    }

    @Test
    fun largeCount_rendersWithoutTruncation() {
        // The tile uses `titleMedium` styling and we don't want a hard
        // width constraint to chop the number. Pin a >3-digit count and
        // confirm the string reaches the tree unchanged.
        composeTestRule.setContent {
            IcyPowerTheme(themeMode = ThemeMode.Light) {
                OverviewStripCard(
                    stats = CycleOverviewStats(0, 0, 0, dueInNext30 = 1234),
                )
            }
        }
        composeTestRule.onNodeWithText("1234").assertIsDisplayed()
    }
}