package com.kap1bala.icypower.data.cycle

import kotlinx.serialization.Serializable

/**
 * A user-tracked device on a recurring charge cycle.
 *
 * Not bound to any HA entity — this is purely local, user-maintained data
 * for devices the user knows need periodic charging but that aren't exposed
 * to Home Assistant.
 *
 * All timestamps are epoch milliseconds (`System.currentTimeMillis()`).
 *
 * @property id Stable UUID generated at create time. Never re-used even after delete.
 * @property name User-visible device label.
 * @property category Optional grouping hint (e.g. "遥控器", "门锁"). UI may use it
 *                    for chip styling in a future iteration; v1 treats it as plain text.
 * @property cycleDays How often the device needs charging, in days.
 * @property lastChargedAt Epoch millis of the last charge. Set to `now()` by the
 *                         "已充电" button on the home card; editable in the
 *                         edit form for back-fill.
 * @property note Optional free-form note.
 */
@Serializable
data class CycleDevice(
    val id: String,
    val name: String,
    val category: String? = null,
    val cycleDays: Int,
    val lastChargedAt: Long,
    val note: String? = null,
    val createdAt: Long,
    val updatedAt: Long,
)

/** Overdue severity, used by UI to pick warning vs danger color. */
enum class OverdueSeverity {
    None,        // not yet due
    Warning,     // past cycleDays
    Danger,      // past 1.5x cycleDays
}

/** Derived state for a [CycleDevice] at a given moment. */
data class CycleDeviceState(
    val device: CycleDevice,
    /** Days since last charged. Negative means upcoming (future lastChargedAt, shouldn't happen). */
    val daysSinceLastCharge: Long,
    val severity: OverdueSeverity,
) {
    companion object {
        /** Severity threshold for "danger" — see feat.md §1.1. */
        const val DANGER_MULTIPLIER = 1.5

        fun from(device: CycleDevice, now: Long): CycleDeviceState {
            val millisPerDay = 24L * 60L * 60L * 1000L
            val elapsedDays = (now - device.lastChargedAt) / millisPerDay
            val severity = when {
                elapsedDays >= (device.cycleDays * DANGER_MULTIPLIER).toLong() -> OverdueSeverity.Danger
                elapsedDays >= device.cycleDays -> OverdueSeverity.Warning
                else -> OverdueSeverity.None
            }
            return CycleDeviceState(device, elapsedDays, severity)
        }
    }
}
