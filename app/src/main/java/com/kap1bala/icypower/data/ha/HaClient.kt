package com.kap1bala.icypower.data.ha

import kotlinx.coroutines.flow.Flow

/**
 * Minimal contract for talking to a Home Assistant instance.
 *
 * Mirrors `prompts/ha.md` §4–§5. Methods here are read-only — we never
 * invoke HA Services or write state.
 *
 * Battery-reading contract (ha.md §6):
 *   - `attributes.battery_level` first
 *   - then `attributes.battery`
 *   - then `state` (parseFloat)
 *   - if all three are absent / unparseable, the consumer should treat the
 *     entity as "no battery attribute" — see [HaState.hasBatteryReading].
 */
interface HaClient {
    /** Lightweight liveness check: returns true iff `/api/` returns 200. */
    suspend fun probe(): Boolean

    /** All entity states. Used at cold start to build the in-memory baseline. */
    suspend fun getStates(): List<HaState>

    /** Single entity lookup; returns null if HA replied 404. */
    suspend fun getState(entityId: String): HaState?

    /**
     * History samples for a single entity, oldest first.
     *
     * @param since ISO-8601 lower bound (inclusive). Caller decides the
     *              window — 7d / 30d / 90d as in feat.md §2.2.
     */
    suspend fun getHistory(entityId: String, since: String): List<HaHistoryPoint>

    /**
     * Hot stream of `state_changed` events for the given entities.
     *
     * Implementations must:
     *   - emit [HaStateChange] when new state arrives
     *   - emit [HaClientError] when the underlying connection drops
     *   - apply the reconnection backoff described in ha.md §5.5
     *   - close the flow cleanly when the collector cancels
     *
     * For the v1 stub, this returns an empty Flow that never emits.
     */
    fun subscribeStateChanges(entityIds: List<String>): Flow<HaEvent>
}

/** Event type for the hot state-change stream. */
sealed interface HaEvent {
    data class HaStateChange(val entityId: String, val state: HaState) : HaEvent
    data class HaClientError(val cause: Throwable) : HaEvent
}

/** Snapshot of an entity at a point in time. */
data class HaState(
    val entityId: String,
    /** Raw `state` string from HA — may be "85", "unknown", "unavailable", etc. */
    val state: String,
    /** ISO-8601 timestamp from HA. */
    val lastUpdated: String,
    /** Raw attributes object — kept as Map for v1 simplicity; consumers can
     *  extract `battery` / `battery_level` themselves per ha.md §6. */
    val attributes: Map<String, Any?>,
    /** Convenience: which area (room) the entity belongs to, if any. */
    val area: String? = null,
) {
    /**
     * Returns the battery percentage (0–100) per ha.md §6 priority, or null
     * when no battery attribute is exposed.
     */
    fun batteryPercent(): Int? {
        attributes["battery_level"]?.toString()?.toIntOrNull()?.let { return it }
        attributes["battery"]?.toString()?.toIntOrNull()?.let { return it }
        return state.toIntOrNull()
    }

    /** True iff HA reports this entity as unavailable / unknown / none. */
    val isUnavailable: Boolean
        get() = state in UNAVAILABLE_STATES
}

/** One sample from `/api/history/period`. */
data class HaHistoryPoint(
    /** ISO-8601 timestamp from HA. */
    val timestamp: String,
    /** Battery percentage at that timestamp (already resolved via §6). */
    val batteryPercent: Int?,
)

private val UNAVAILABLE_STATES = setOf("unknown", "unavailable", "none")
