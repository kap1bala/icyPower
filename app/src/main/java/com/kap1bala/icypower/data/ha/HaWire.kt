package com.kap1bala.icypower.data.ha

import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

/**
 * Wire-format DTOs for Home Assistant REST responses.
 *
 * `attributes` is intentionally typed as `Map<String, JsonElement>` — HA's
 * schema for entity attributes is open (every integration adds its own keys),
 * so we deserialize opaquely and let [OkHttpHaClient] coerce each value to
 * a Kotlin primitive before stuffing it into [HaState.attributes].
 *
 * Kept internal — these types are an implementation detail of the OkHttp
 * client and should not leak outside `data.ha`.
 */
@Serializable
internal data class HaStateDto(
    val entity_id: String,
    val state: String,
    val last_updated: String,
    val attributes: Map<String, JsonElement> = emptyMap(),
)

/**
 * One history entry from `/api/history/period`. We only care about
 * [state], [last_changed], and the battery fields in [attributes] — the
 * rest is omitted to keep decoding cheap.
 */
@Serializable
internal data class HaHistoryEntryDto(
    val entity_id: String,
    val state: String,
    val last_changed: String,
    val attributes: Map<String, JsonElement> = emptyMap(),
)
