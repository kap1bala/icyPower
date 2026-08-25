package com.kap1bala.icypower.data.ha

import kotlinx.coroutines.runBlocking
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Before
import org.junit.Test
import java.io.IOException
import java.util.concurrent.TimeUnit

/**
 * Unit tests for [OkHttpHaClient] REST surface, backed by [MockWebServer].
 *
 * What we cover:
 *  - probe: 200 → true, 401 → false, 500 → false.
 *  - getStates: 200 → decoded list, 401 → IOException (via AuthorizationInterceptor).
 *  - getState: 200 → HaState, 404 → null, 401 → IOException.
 *  - getHistory: 200 → flattened history list, 401 → IOException.
 *  - Authorization header is `Bearer <token>`.
 *  - entity_id containing `.` is URL-encoded (e.g. `sensor.lock.battery`).
 *  - baseUrl trailing `/` is trimmed (no double-slash in request paths).
 *
 * What we DON'T cover here:
 *  - The WebSocket subscribeStateChanges path — its reconnect loop, auth
 *    phase, and entity-id whitelist filtering are exercised by an
 *    instrumentation/integration test if added later; mocking the WS
 *    hand-shake reliably requires a real HA server or a heavier test rig.
 */
class OkHttpHaClientTest {

    private lateinit var server: MockWebServer
    private lateinit var client: OkHttpHaClient
    private val token = "test-long-lived-access-token"

    @Before
    fun setUp() {
        server = MockWebServer().apply { start() }
        // MockWebServer serves http://; the client should auto-promote ws://
        // when it constructs the WS URL (verified indirectly via wsBase
        // behaviour; we don't open a WS in these tests).
        client = OkHttpHaClient(
            baseUrl = server.url("/").toString().trimEnd('/'),  // already no trailing /
            token = token,
            okHttpClient = OkHttpClient(),
        )
    }

    @After
    fun tearDown() {
        server.shutdown()
    }

    // ─── probe ───────────────────────────────────────────────────────────────

    @Test
    fun probe_200_returnsTrue() = runBlocking {
        server.enqueue(MockResponse().setResponseCode(200).setBody("[]"))
        assertTrue(client.probe())
    }

    @Test
    fun probe_401_returnsFalse() = runBlocking {
        // AuthorizationInterceptor short-circuits 401 → HaAuthException.
        // probe() wraps the call in runCatching, so it surfaces as false.
        server.enqueue(MockResponse().setResponseCode(401))
        assertEquals(false, client.probe())
    }

    @Test
    fun probe_500_returnsFalse() = runBlocking {
        // Network-level failure (not auth) — also swallowed by runCatching.
        server.enqueue(MockResponse().setResponseCode(500))
        assertEquals(false, client.probe())
    }

    @Test
    fun probe_sendsAuthorizationHeader() = runBlocking {
        server.enqueue(MockResponse().setResponseCode(200).setBody("[]"))
        client.probe()

        val recorded = server.takeRequest(1, TimeUnit.SECONDS)
        assertNotNull("probe must record a request", recorded)
        assertEquals("Bearer $token", recorded!!.getHeader("Authorization"))
    }

    // ─── getStates ───────────────────────────────────────────────────────────

    @Test
    fun getStates_200_decodesAllEntities() = runBlocking {
        val body = """
            [
              {"entity_id":"sensor.a","state":"on","last_updated":"2026-08-17T00:00:00+00:00","attributes":{"battery_level":85}},
              {"entity_id":"sensor.b","state":"off","last_updated":"2026-08-17T00:00:01+00:00","attributes":{}}
            ]
        """.trimIndent()
        server.enqueue(MockResponse().setResponseCode(200).setBody(body))

        val states = client.getStates()
        assertEquals(2, states.size)
        assertEquals("sensor.a", states[0].entityId)
        assertEquals(85, states[0].batteryPercent())
        assertEquals("sensor.b", states[1].entityId)
        assertNull(states[1].batteryPercent())
    }

    @Test
    fun getStates_401_throwsIOException() {
        server.enqueue(MockResponse().setResponseCode(401))
        val e = assertThrows(IOException::class.java) {
            runBlocking { client.getStates() }
        }
        // Cause chain carries the HaAuthException detail; just verify the
        // surface area is IOException per the contract.
        assertNotNull(e.message)
    }

    @Test
    fun getStates_malformedJson_throws() {
        server.enqueue(MockResponse().setResponseCode(200).setBody("not-json"))
        assertThrows(Exception::class.java) {
            runBlocking { client.getStates() }
        }
    }

    // ─── getState ────────────────────────────────────────────────────────────

    @Test
    fun getState_200_returnsDomainEntity() = runBlocking {
        val body = """
            {"entity_id":"sensor.front","state":"85","last_updated":"2026-08-17T00:00:00+00:00","attributes":{"battery_level":85}}
        """.trimIndent()
        server.enqueue(MockResponse().setResponseCode(200).setBody(body))

        val s = client.getState("sensor.front")
        assertNotNull(s)
        assertEquals("sensor.front", s!!.entityId)
        assertEquals(85, s.batteryPercent())
    }

    @Test
    fun getState_404_returnsNull() = runBlocking {
        server.enqueue(MockResponse().setResponseCode(404))
        assertNull(client.getState("sensor.missing"))
    }

    @Test
    fun getState_401_throwsIOException() {
        server.enqueue(MockResponse().setResponseCode(401))
        assertThrows(IOException::class.java) {
            runBlocking { client.getState("sensor.x") }
        }
    }

    @Test
    fun getState_entityIdWithSpace_isUrlEncoded() = runBlocking {
        // `java.net.URLEncoder.encode` (form-encoding) maps space → `+`
        // and leaves dots alone, so use a space to verify the encoding
        // pass runs at all. The literal space must NOT appear in the wire
        // path; `+` is the on-the-wire representation.
        server.enqueue(MockResponse().setResponseCode(200).setBody(
            """{"entity_id":"sensor.lock battery","state":"42","last_updated":"2026-08-17T00:00:00+00:00","attributes":{}}"""
        ))
        client.getState("sensor.lock battery")

        val recorded = server.takeRequest(1, TimeUnit.SECONDS)
        assertNotNull(recorded)
        val rawPath = recorded!!.path
        assertTrue(
            "space must not survive unencoded in path: $rawPath",
            !rawPath!!.contains(" "),
        )
        assertTrue(
            "encoded space should appear as '+': $rawPath",
            rawPath.contains("lock+battery"),
        )
    }

    // ─── getHistory ──────────────────────────────────────────────────────────

    @Test
    fun getHistory_200_flattensNestedArrays() = runBlocking {
        // /api/history/period returns `[[entry, entry, ...]]` — outer list
        // keyed by entity_id (always length 1 here), inner list of samples.
        val body = """
            [
              [
                {"entity_id":"sensor.lock","state":"85","last_changed":"2026-08-10T00:00:00+00:00","attributes":{"battery_level":85}},
                {"entity_id":"sensor.lock","state":"80","last_changed":"2026-08-11T00:00:00+00:00","attributes":{"battery_level":80}}
              ]
            ]
        """.trimIndent()
        server.enqueue(MockResponse().setResponseCode(200).setBody(body))

        val points = client.getHistory("sensor.lock", since = "2026-08-10T00:00:00Z")
        assertEquals(2, points.size)
        assertEquals(85, points[0].batteryPercent)
        assertEquals(80, points[1].batteryPercent)
        assertEquals("2026-08-10T00:00:00+00:00", points[0].timestamp)
    }

    @Test
    fun getHistory_emptyOuterArray_returnsEmptyList() = runBlocking {
        // HA returns `[]` when no history matches — outer list empty.
        server.enqueue(MockResponse().setResponseCode(200).setBody("[]"))
        val points = client.getHistory("sensor.x", since = "2026-08-10T00:00:00Z")
        assertTrue(points.isEmpty())
    }

    @Test
    fun getHistory_401_throwsIOException() {
        server.enqueue(MockResponse().setResponseCode(401))
        assertThrows(IOException::class.java) {
            runBlocking { client.getHistory("sensor.x", since = "2026-08-10T00:00:00Z") }
        }
    }

    @Test
    fun getHistory_includesSinceTimestampAndMinimalResponseFlag() = runBlocking {
        server.enqueue(MockResponse().setResponseCode(200).setBody("[]"))
        client.getHistory("sensor.x", since = "2026-08-10T00:00:00Z")

        val recorded = server.takeRequest(1, TimeUnit.SECONDS)
        assertNotNull(recorded)
        val rawPath = recorded!!.path
        assertTrue(
            "since param should appear in path: $rawPath",
            rawPath!!.contains("2026-08-10T00:00:00Z"),
        )
        assertTrue(
            "path should include minimal_response flag: $rawPath",
            rawPath.contains("minimal_response"),
        )
    }

    // ─── url / header contracts ──────────────────────────────────────────────

    @Test
    fun requestPath_doesNotContainDoubleSlash() = runBlocking {
        // baseUrl may include a trailing slash (e.g. http://h:8123/) and the
        // client must trim it; otherwise request paths would start with `//`.
        // Reconstruct with explicit trailing slash to verify trimming.
        val withSlash = OkHttpHaClient(
            baseUrl = server.url("/").toString() + "/",  // force a trailing /
            token = token,
            okHttpClient = OkHttpClient(),
        )
        server.enqueue(MockResponse().setResponseCode(200).setBody("[]"))
        withSlash.probe()

        val recorded = server.takeRequest(1, TimeUnit.SECONDS)
        assertNotNull(recorded)
        // The recorded path lives at /api/states; the "host" prefix that
        // MockWebServer shows is "/". Concatenating the two should NOT
        // produce a "///" or "//api" sequence at the join — i.e. the
        // baseUrl trailing / was trimmed before concatenation.
        val rawPath = recorded!!.path
        assertTrue(
            "path must not contain '//api': $rawPath",
            !rawPath!!.startsWith("//api"),
        )
    }

    @Test
    fun request_carriesBearerTokenHeader() = runBlocking {
        server.enqueue(MockResponse().setResponseCode(200).setBody("[]"))
        client.getStates()
        val recorded = server.takeRequest(1, TimeUnit.SECONDS)
        assertNotNull(recorded)
        assertEquals("Bearer $token", recorded!!.getHeader("Authorization"))
    }

    @Test
    fun construction_rejectsBlankToken() {
        assertThrows(IllegalArgumentException::class.java) {
            OkHttpHaClient(
                baseUrl = server.url("/").toString(),
                token = "  ",
                okHttpClient = OkHttpClient(),
            )
        }
    }

    @Test
    fun construction_rejectsBlankBaseUrl() {
        assertThrows(IllegalArgumentException::class.java) {
            OkHttpHaClient(
                baseUrl = "",
                token = token,
                okHttpClient = OkHttpClient(),
            )
        }
    }

    @Test
    fun getStates_recordsCorrectPath() = runBlocking {
        server.enqueue(MockResponse().setResponseCode(200).setBody("[]"))
        try {
            client.getStates()
        } catch (e: Exception) {
            fail("unexpected: $e")
        }
        val recorded = server.takeRequest(1, TimeUnit.SECONDS)
        assertNotNull(recorded)
        assertEquals("/api/states", recorded!!.path)
    }
}