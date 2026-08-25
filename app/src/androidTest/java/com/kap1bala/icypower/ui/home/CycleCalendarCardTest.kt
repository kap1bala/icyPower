package com.kap1bala.icypower.ui.home

import androidx.activity.ComponentActivity
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import com.kap1bala.icypower.data.cycle.CycleDaySummary
import com.kap1bala.icypower.data.cycle.epochDayOf
import com.kap1bala.icypower.ui.theme.IcyPowerTheme
import com.kap1bala.icypower.ui.theme.ThemeMode
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import java.util.Locale

/**
 * Compose UI tests for [CycleCalendarCard].
 *
 * The calendar is a read-only surface whose interactive bits are navigation
 * buttons (`onPrev` / `onNext` / `onJumpToday`). These tests exercise:
 *  - Month header renders the locale-formatted "yyyy 年 M 月" / "MMMM yyyy".
 *  - Prev / Next buttons fire their callbacks exactly once per click.
 *  - "Jump to today" button only appears when viewing a month other than
 *    the current one.
 *  - Today cell renders its day-of-month as text.
 *  - Locale is pinned to zh-CN to keep the assertion text deterministic
 *    regardless of host language.
 */
class CycleCalendarCardTest {

    @get:Rule
    val composeTestRule = createAndroidComposeRule<ComponentActivity>()

    private val savedLocale: Locale? = null

    @org.junit.Before
    fun pinLocale() {
        // Save and force zh-CN so the header text "2026 年 8 月" is stable.
        // (Default test locale is the JVM default — usually en-US, which
        // would format the header as "August 2026".)
        Locale.setDefault(Locale.SIMPLIFIED_CHINESE)
    }

    @Test
    fun header_rendersMonthInChineseFormat() {
        val today = epochDayOf(2026, 8, 17)
        composeTestRule.setContent {
            IcyPowerTheme(themeMode = ThemeMode.Light) {
                CycleCalendarCard(
                    year = 2026,
                    month = 8,
                    today = today,
                    calendar = emptyMap(),
                    onPrev = {},
                    onNext = {},
                    onJumpToday = {},
                    currentYear = 2026,
                    currentMonth = 8,
                )
            }
        }

        composeTestRule.onNodeWithText("2026 年 8 月").assertIsDisplayed()
    }

    @Test
    fun prevButton_invokesOnPrev() {
        var prevCount = 0
        composeTestRule.setContent {
            IcyPowerTheme(themeMode = ThemeMode.Light) {
                CycleCalendarCard(
                    year = 2026, month = 8,
                    today = epochDayOf(2026, 8, 17),
                    calendar = emptyMap(),
                    onPrev = { prevCount++ },
                    onNext = {},
                    onJumpToday = {},
                    currentYear = 2026, currentMonth = 8,
                )
            }
        }

        composeTestRule.onNodeWithContentDescription("上个月").performClick()
        assertEquals(1, prevCount)
    }

    @Test
    fun nextButton_invokesOnNext() {
        var nextCount = 0
        composeTestRule.setContent {
            IcyPowerTheme(themeMode = ThemeMode.Light) {
                CycleCalendarCard(
                    year = 2026, month = 8,
                    today = epochDayOf(2026, 8, 17),
                    calendar = emptyMap(),
                    onPrev = {},
                    onNext = { nextCount++ },
                    onJumpToday = {},
                    currentYear = 2026, currentMonth = 8,
                )
            }
        }

        composeTestRule.onNodeWithContentDescription("下个月").performClick()
        assertEquals(1, nextCount)
    }

    @Test
    fun jumpTodayButton_hiddenWhenViewingCurrentMonth() {
        composeTestRule.setContent {
            IcyPowerTheme(themeMode = ThemeMode.Light) {
                // currentYear/Month == year/month → no jump button.
                CycleCalendarCard(
                    year = 2026, month = 8,
                    today = epochDayOf(2026, 8, 17),
                    calendar = emptyMap(),
                    onPrev = {}, onNext = {}, onJumpToday = {},
                    currentYear = 2026, currentMonth = 8,
                )
            }
        }

        composeTestRule.onNodeWithText("跳转到今天").assertDoesNotExist()
    }

    @Test
    fun jumpTodayButton_visibleWhenViewingDifferentMonth() {
        var jumpCount = 0
        composeTestRule.setContent {
            IcyPowerTheme(themeMode = ThemeMode.Light) {
                CycleCalendarCard(
                    year = 2026, month = 7,  // viewing July
                    today = epochDayOf(2026, 8, 17),
                    calendar = emptyMap(),
                    onPrev = {}, onNext = {},
                    onJumpToday = { jumpCount++ },
                    currentYear = 2026, currentMonth = 8,  // current month = Aug
                )
            }
        }

        composeTestRule.onNodeWithText("跳转到今天").assertIsDisplayed()
        composeTestRule.onNodeWithText("跳转到今天").performClick()
        assertEquals(1, jumpCount)
    }

    @Test
    fun todayCell_rendersDayOfMonth() {
        // The current-day cell renders "17" as text. Other cells render
        // their own day-of-month numbers, so the assertion is "17 is
        // displayed" rather than "17 is the only number".
        composeTestRule.setContent {
            IcyPowerTheme(themeMode = ThemeMode.Light) {
                CycleCalendarCard(
                    year = 2026, month = 8,
                    today = epochDayOf(2026, 8, 17),
                    calendar = emptyMap(),
                    onPrev = {}, onNext = {}, onJumpToday = {},
                    currentYear = 2026, currentMonth = 8,
                )
            }
        }

        composeTestRule.onNodeWithText("17").assertIsDisplayed()
    }

    @Test
    fun dayWithUpcomingSummary_rendersCountOverlay() {
        // upcomingCount > 1 + no overdue/danger → show count overlay next
        // to the dot. Pin the count to "3" so we can assert the overlay
        // text appears.
        val day = epochDayOf(2026, 8, 20)
        composeTestRule.setContent {
            IcyPowerTheme(themeMode = ThemeMode.Light) {
                CycleCalendarCard(
                    year = 2026, month = 8,
                    today = epochDayOf(2026, 8, 17),
                    calendar = mapOf(
                        day to CycleDaySummary(
                            day = day,
                            hasOverdue = false,
                            hasDanger = false,
                            upcomingCount = 3,
                        ),
                    ),
                    onPrev = {}, onNext = {}, onJumpToday = {},
                    currentYear = 2026, currentMonth = 8,
                )
            }
        }

        composeTestRule.onNodeWithText("3").assertIsDisplayed()
    }
}