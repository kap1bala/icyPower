package com.kap1bala.icypower.data.ha

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * Unit tests for the internal wire-format DTOs in [HaWire] and their
 * DTO → domain conversions in [OkHttpHaClient].
 *
 * `internal` types are visible to tests in the same Gradle module, so we
 * can poke them directly without exposing them in the production API.
 */
class HaWireTest {

    private val json = Json {
        ignoreUnknownKeys = true
        coerceInputValues = true
    }

    // ─── HaStateDto decoding ─────────────────────────────────────────────────

    @Test
    fun stateDto_decodesMinimalPayload() {
        val raw = """
            {
              "entity_id": "sensor.temp",
              "state": "21.5",
              "last_updated": "2026-08-17T12:00:00+00:00",
              "attributes": {}
            }
        """.trimIndent()

        val dto = json.decodeFromString(HaStateDto.serializer(), raw)
        assertEquals("sensor.temp", dto.entity_id)
        assertEquals("21.5", dto.state)
        assertEquals("2026-08-17T12:00:00+00:00", dto.last_updated)
        assertTrue(dto.attributes.isEmpty())
    }

    @Test
    fun stateDto_decodesAttributesWithMixedTypes() {
        // HA attributes can be number, boolean, string, nested object, array.
        // We deserialize them opaquely as JsonElement and coerce in
        // toPrimitiveAny().
        val raw = """
            {
              "entity_id": "sensor.mixed",
              "state": "on",
              "last_updated": "2026-08-17T12:00:00+00:00",
              "attributes": {
                "battery_level": 85,
                "battery_low": false,
                "friendly_name": "Front Door",
                "nested": {"a": 1},
                "list": [1, 2, 3]
              }
            }
        """.trimIndent()

        val dto = json.decodeFromString(HaStateDto.serializer(), raw)
        assertEquals(85, dto.attributes["battery_level"]?.jsonPrimitive?.intOrNull)
        assertEquals(false, dto.attributes["battery_low"]?.jsonPrimitive?.content?.toBoolean())
        assertEquals("Front Door", dto.attributes["friendly_name"]?.jsonPrimitive?.content)
        // nested + list survive as JsonElement (kept opaque)
        assertNotNull(dto.attributes["nested"])
        assertNotNull(dto.attributes["list"])
    }

    @Test
    fun stateDto_ignoresUnknownKeys() {
        // Forward-compat: HA integrations add new fields; we must not crash.
        val raw = """
            {
              "entity_id": "sensor.future",
              "state": "ok",
              "last_updated": "2026-08-17T12:00:00+00:00",
              "attributes": {},
              "context": {"id": "abc", "parent_id": null},
              "new_field_we_dont_know_about": 42
            }
        """.trimIndent()

        val dto = json.decodeFromString(HaStateDto.serializer(), raw)
        assertEquals("sensor.future", dto.entity_id)
    }

    @Test
    fun stateDto_defaultsAttributesWhenAbsent() {
        // Some HA endpoints (rare) omit the attributes key entirely; the
        // DTO defaults to an empty map.
        val raw = """
            {
              "entity_id": "sensor.bare",
              "state": "off",
              "last_updated": "2026-08-17T12:00:00+00:00"
            }
        """.trimIndent()

        val dto = json.decodeFromString(HaStateDto.serializer(), raw)
        assertTrue(dto.attributes.isEmpty())
    }

    // ─── toDomain ────────────────────────────────────────────────────────────

    @Test
    fun toDomain_preservesIdentityAndCoercesAttributeValues() {
        val raw = """
            {
              "entity_id": "sensor.front_door",
              "state": "85",
              "last_updated": "2026-08-17T12:00:00+00:00",
              "attributes": {
                "battery_level": 85,
                "battery_low": false,
                "unit_of_measurement": "%"
              }
            }
        """.trimIndent()

        val dto = json.decodeFromString(HaStateDto.serializer(), raw)
        val domain = dto.toDomain()

        assertEquals("sensor.front_door", domain.entityId)
        assertEquals("85", domain.state)
        assertEquals("2026-08-17T12:00:00+00:00", domain.lastUpdated)
        // Int attribute coerced to Int; Boolean stays Boolean; String stays String.
        assertEquals(85, domain.attributes["battery_level"])
        assertEquals(false, domain.attributes["battery_low"])
        assertEquals("%", domain.attributes["unit_of_measurement"])
    }

    @Test
    fun toDomain_keepsUnknownJsonShapeAsStringFallback() {
        // A nested-object attribute (HA does this for some integrations)
        // cannot be coerced to a Kotlin primitive — the converter falls
        // back to its `toString()` representation.
        val nestedJson = """{"nested":{"k":"v"}}"""
        val dto = HaStateDto(
            entity_id = "sensor.nested",
            state = "ok",
            last_updated = "2026-08-17T12:00:00+00:00",
            attributes = mapOf("nested" to kotlinx.serialization.json.Json.parseToJsonElement(nestedJson)),
        )
        val domain = dto.toDomain()

        // Nested objects survive as a non-null toString form (whatever the
        // serializer prints) — pinned here so we notice if the fallback
        // strategy changes.
        val nestedAttr = domain.attributes["nested"]
        assertNotNull(nestedAttr)
        assertTrue("nested attribute should be string-coerced or fallback",
            nestedAttr is String)
    }

    // ─── HaHistoryEntryDto + battery priority (ha.md §6) ─────────────────────

    @Test
    fun historyEntry_prefersBatteryLevelOverBattery() {
        val raw = """
            {
              "entity_id": "sensor.lock",
              "state": "85",
              "last_changed": "2026-08-17T00:00:00+00:00",
              "attributes": {
                "battery_level": 85,
                "battery": 42
              }
            }
        """.trimIndent()
        val dto = json.decodeFromString(HaHistoryEntryDto.serializer(), raw)
        assertEquals(85, dto.toDomain().batteryPercent)
    }

    @Test
    fun historyEntry_fallsBackToBattery() {
        val raw = """
            {
              "entity_id": "sensor.lock",
              "state": "85",
              "last_changed": "2026-08-17T00:00:00+00:00",
              "attributes": { "battery": 42 }
            }
        """.trimIndent()
        val dto = json.decodeFromString(HaHistoryEntryDto.serializer(), raw)
        assertEquals(42, dto.toDomain().batteryPercent)
    }

    @Test
    fun historyEntry_fallsBackToStateAsInt() {
        val raw = """
            {
              "entity_id": "sensor.lock",
              "state": "73",
              "last_changed": "2026-08-17T00:00:00+00:00",
              "attributes": {}
            }
        """.trimIndent()
        val dto = json.decodeFromString(HaHistoryEntryDto.serializer(), raw)
        assertEquals(73, dto.toDomain().batteryPercent)
    }

    @Test
    fun historyEntry_returnsNullWhenNothingResolves() {
        val raw = """
            {
              "entity_id": "sensor.lock",
              "state": "unknown",
              "last_changed": "2026-08-17T00:00:00+00:00",
              "attributes": {}
            }
        """.trimIndent()
        val dto = json.decodeFromString(HaHistoryEntryDto.serializer(), raw)
        assertNull(dto.toDomain().batteryPercent)
    }

    @Test
    fun historyEntry_preservesTimestamp() {
        val raw = """
            {
              "entity_id": "sensor.lock",
              "state": "85",
              "last_changed": "2026-08-17T12:34:56+00:00",
              "attributes": { "battery_level": 85 }
            }
        """.trimIndent()
        val dto = json.decodeFromString(HaHistoryEntryDto.serializer(), raw)
        assertEquals("2026-08-17T12:34:56+00:00", dto.toDomain().timestamp)
    }

    // ─── JsonElement primitives ──────────────────────────────────────────────

    @Test
    fun toPrimitiveAny_intCoercesToInt() {
        val el = JsonPrimitive(42)
        assertEquals(42, el.toPrimitiveAny())
    }

    @Test
    fun toPrimitiveAny_doubleCoercesToDouble() {
        val el = JsonPrimitive(42.5)
        assertEquals(42.5, el.toPrimitiveAny())
    }

    @Test
    fun toPrimitiveAny_booleanCoercesToBoolean() {
        assertEquals(true, JsonPrimitive(true).toPrimitiveAny())
        assertEquals(false, JsonPrimitive(false).toPrimitiveAny())
    }

    @Test
    fun toPrimitiveAny_stringStaysString() {
        assertEquals("hello", JsonPrimitive("hello").toPrimitiveAny())
    }

    @Test
    fun toPrimitiveAny_objectFallsBackToString() {
        val obj = buildJsonObject { put("a", JsonPrimitive(1)) }
        // Not a JsonPrimitive → falls back to toString().
        val result = obj.toPrimitiveAny()
        assertNotNull(result)
        assertTrue(result is String)
        assertEquals(obj.toString(), result)
    }

    @Test
    fun toAnyMap_coercesAllEntries() {
        val map = mapOf(
            "i" to JsonPrimitive(1),
            "s" to JsonPrimitive("x"),
            "b" to JsonPrimitive(false),
        )
        val out = map.toAnyMap()
        assertEquals(1, out["i"])
        assertEquals("x", out["s"])
        assertEquals(false, out["b"])
    }

    @Test
    fun toAnyMap_emptyMapReturnsEmptyMap() {
        assertTrue(emptyMap<String, kotlinx.serialization.json.JsonElement>().toAnyMap().isEmpty())
    }

    // ─── isUnavailable interaction with domain state ─────────────────────────

    @Test
    fun isUnavailable_viaDomain_falseForNumericState() {
        val raw = """
            {
              "entity_id": "sensor.x",
              "state": "85",
              "last_updated": "2026-08-17T00:00:00+00:00",
              "attributes": {}
            }
        """.trimIndent()
        val domain = json.decodeFromString(HaStateDto.serializer(), raw).toDomain()
        assertFalse(domain.isUnavailable)
    }
}