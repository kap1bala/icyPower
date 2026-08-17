package com.kap1bala.icypower.ui.cycle

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
internal fun formatRelativeDays(days: Long): String = when {
    days <= 0 -> "今天"
    days == 1L -> "1 天前"
    else -> "$days 天前"
}

private val displayDateFormatter = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())

internal fun formatDisplayDate(millis: Long): String =
    displayDateFormatter.format(Date(millis))
