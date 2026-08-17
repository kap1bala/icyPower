package com.kap1bala.icypower.ui.cycle

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.kap1bala.icypower.R
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Shared formatting helpers for cycle-device UI surfaces.
 *
 * Both [CycleDeviceListScreen] and the home-screen card render the same
 * "距上次充电 N 天 / YYYY-MM-DD" line, so the formatting lives here rather
 * than being duplicated in each Composable.
 *
 * The date picker dialog inside [CycleDeviceEditScreen] uses a richer
 * "yyyy-MM-dd HH:mm" format intentionally — that's defined on
 * [com.kap1bala.icypower.ui.cycle.CycleDeviceEditViewModel] itself.
 */

/**
 * "今天" / "Yesterday" / "N 天前" — locale-aware string for the
 * days-since-last-charge tagline.
 *
 * Composable so it can pull from `R.string.*`; `days` is a `Long` because
 * [CycleDeviceState.daysSinceLastCharge] is computed from epoch millis and
 * can be negative in pathological (clock-skewed) cases.
 *
 * Negative or zero → "今天" / "Today".
 * Exactly one      → "1 天前" / "1 day ago" (keeps the visual rhythm of
 *                    the original wording — not "Yesterday").
 * Greater than one → "%1$d 天前" / "%1$d days ago".
 */
@Composable
internal fun formatRelativeDays(days: Long): String = when {
    days <= 0 -> stringResource(R.string.cycle_rel_today)
    days == 1L -> stringResource(R.string.cycle_rel_one_day_ago)
    else -> stringResource(R.string.cycle_rel_days_ago, days)
}

private val displayDateFormatter = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

internal fun formatDisplayDate(millis: Long): String =
    displayDateFormatter.format(Date(millis))
