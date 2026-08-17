package com.kap1bala.icypower.data.ha

import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.intOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.Interceptor
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.Response
import okhttp3.WebSocket
import okhttp3.WebSocketListener
import okio.ByteString
import java.io.IOException
import java.util.concurrent.atomic.AtomicBoolean
import java.util.concurrent.atomic.AtomicLong
import kotlin.coroutines.coroutineContext
import kotlin.math.min

/**
 * Real [HaClient] backed by OkHttp REST + WebSocket per `prompts/ha.md`.
 *
 * Responsibilities:
 *   - REST: probe / getStates / getState / getHistory (suspend, one-shot)
 *   - WebSocket: handshake → subscribe → emit state changes with reconnect
 *     (exponential backoff, capped at 60 s per `ha.md` §5.5)
 *
 * Auth:
 *   - REST requests carry `Authorization: Bearer <token>` via an OkHttp
 *     [Interceptor]; a 401 short-circuits to [HaAuthException].
 *   - WS sends the token in the auth phase; `{type:"auth_invalid"}` also
 *     throws [HaAuthException]. Neither is auto-retried — the UI must
 *     navigate to /settings/ha for token rotation.
 *
 * Threading:
 *   - All network IO + Flow production runs on [Dispatchers.IO].
 */
class OkHttpHaClient(
    baseUrl: String,
    private val token: String,
    private val okHttpClient: OkHttpClient,
    private val json: Json = DEFAULT_JSON,
) : HaClient {

    init {
        require(baseUrl.isNotBlank()) { "baseUrl must not be blank" }
        require(token.isNotBlank()) { "token must not be blank" }
    }

    /** REST base URL, trailing `/` trimmed. */
    private val restBase: String = baseUrl.trimEnd('/')

    /** HTTP→WS upgrade. */
    private val wsBase: String =
        if (restBase.startsWith("https://")) restBase.replaceFirst("https://", "wss://")
        else restBase.replaceFirst("http://", "ws://")

    /** Same OkHttp client but with `Authorization` + 401→[HaAuthException] applied. */
    private val authedClient: OkHttpClient by lazy {
        okHttpClient.newBuilder()
            .addInterceptor(AuthorizationInterceptor("Bearer $token"))
            .build()
    }

    // ─────────────────────── REST (suspend) ────────────────────────────

    override suspend fun probe(): Boolean = withContext(Dispatchers.IO) {
        // Probe via /api/states (not /api/). /api/ returns 200 anonymously,
        // so a successful probe there would NOT tell us the token is valid —
        // it would only confirm the URL is reachable. /api/states requires
        // authentication, so this single call distinguishes "URL up" from
        // "URL up + token valid". 401 surfaces as a thrown IOException via
        // the AuthorizationInterceptor and is caught by [runCatching].
        runCatching {
            authedClient.newCall(buildRequest("/api/states")).execute().use { it.isSuccessful }
        }.getOrElse { false }
    }

    override suspend fun getStates(): List<HaState> = withContext(Dispatchers.IO) {
        authedClient.newCall(buildRequest("/api/states")).execute().use { resp ->
            checkResponse(resp)
            val raw = resp.body?.string().orEmpty()
            json.decodeFromString(LIST_OF_HA_STATE_DTO, raw)
                .map { it.toDomain() }
        }
    }

    override suspend fun getState(entityId: String): HaState? = withContext(Dispatchers.IO) {
        val safeId = encodeEntityId(entityId)
        authedClient.newCall(buildRequest("/api/states/$safeId")).execute().use { resp ->
            when (resp.code) {
                404 -> null
                in 200..299 -> {
                    val raw = resp.body?.string().orEmpty()
                    json.decodeFromString(HaStateDto.serializer(), raw).toDomain()
                }
                else -> {
                    checkResponse(resp)
                    null
                }
            }
        }
    }

    override suspend fun getHistory(
        entityId: String,
        since: String,
    ): List<HaHistoryPoint> = withContext(Dispatchers.IO) {
        val safeId = encodeEntityId(entityId)
        val path = "/api/history/period/$since?filter_entity_id=$safeId&minimal_response"
        authedClient.newCall(buildRequest(path)).execute().use { resp ->
            checkResponse(resp)
            val raw = resp.body?.string().orEmpty()
            val wrapped = json.decodeFromString(LIST_OF_LIST_OF_HISTORY_DTO, raw)
            wrapped.flatten().map { it.toDomain() }
        }
    }

    // ─────────────────────── WebSocket ─────────────────────────────────

    override fun subscribeStateChanges(entityIds: List<String>): Flow<HaEvent> = flow {
        require(entityIds.isNotEmpty()) { "entityIds must not be empty" }
        var attempt = 0

        while (coroutineContext.isActive) {
            try {
                runWsSession(entityIds).collect { event ->
                    emit(event)
                    if (event is HaEvent.HaStateChange) attempt = 0  // reset on healthy traffic
                }
                attempt = 0
            } catch (e: CancellationException) {
                throw e
            } catch (e: HaAuthException) {
                // 401 / auth_invalid — don't loop forever on a bad token.
                emit(HaEvent.HaClientError(e))
                return@flow
            } catch (e: Exception) {
                emit(HaEvent.HaClientError(e))
            }

            if (!coroutineContext.isActive) return@flow

            val delayMs = min(1L shl attempt.coerceAtMost(6), 60_000L)
            delay(delayMs)
            attempt = if (attempt < 6) attempt + 1 else 6
        }
    }.flowOn(Dispatchers.IO)

    /**
     * One full WS session. Throws [HaAuthException] on `auth_invalid`;
     * any other disconnect propagates as `IOException`.
     */
    private fun runWsSession(entityIds: List<String>): Flow<HaEvent> = callbackFlow {
        val listener = HaWsListener(
            token = token,
            onEvent = { event ->
                trySend(event).also { /* drop on backpressure */ }
            },
            entityIdsWhitelist = entityIds.toSet(),
            json = json,
        )
        val request = Request.Builder().url("$wsBase/api/websocket").build()
        val ws = authedClient.newWebSocket(request, listener)

        // 30 s keepalive ping; 2 missed pongs → close + reconnect.
        val keepalive = listener.startKeepalive(this)
        awaitClose {
            keepalive.cancel()
            runCatching {
                ws.send("""{"id":-1,"type":"unsubscribe_message","subscription":-1}""")
            }
            runCatching { ws.close(WS_CLOSE_NORMAL, "icyPower client cancel") }
        }
    }

    // ─────────────────────── helpers ───────────────────────────────────

    private fun buildRequest(path: String): Request =
        Request.Builder().url("$restBase$path").get().build()

    companion object {
        private val DEFAULT_JSON = Json {
            ignoreUnknownKeys = true
            coerceInputValues = true
        }

        // kotlinx-serialization serializer instances (kept here so the file's
        // top-level functions don't need their own; they are stateless and
        // safe to share).
        private val LIST_OF_HA_STATE_DTO =
            kotlinx.serialization.builtins.ListSerializer(HaStateDto.serializer())
        private val LIST_OF_LIST_OF_HISTORY_DTO =
            kotlinx.serialization.builtins.ListSerializer(
                kotlinx.serialization.builtins.ListSerializer(HaHistoryEntryDto.serializer())
            )

        private const val WS_CLOSE_NORMAL = 1000
        private const val WS_CLOSE_PING_TIMEOUT = 4000
    }
}

// ─────────────────────── internal types ────────────────────────────────

/**
 * Surfaces auth failure as an [IOException] so callers can `catch (IOException)`
 * and inspect `cause as? HaAuthException` if they care.
 */
internal class HaAuthException(message: String) : IOException(message)

// ─────────────────────── REST interceptor ──────────────────────────────

private class AuthorizationInterceptor(
    private val authHeader: String,
) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val original = chain.request()
            .newBuilder()
            .header("Authorization", authHeader)
            .build()
        val response = chain.proceed(original)
        if (response.code == 401) {
            response.close()
            throw HaAuthException("HTTP 401 from HA")
        }
        return response
    }
}

/** Throws on non-success status codes. Caller must have already called `use { }`. */
private fun checkResponse(resp: Response) {
    if (!resp.isSuccessful) {
        throw IOException("HA REST ${resp.code} ${resp.message}")
    }
}

/** HA entity_ids allow letters/digits/`_` only; URL-encode anything outside. */
private fun encodeEntityId(id: String): String = java.net.URLEncoder.encode(id, "UTF-8")

// ─────────────────────── DTO → domain ─────────────────────────────────

internal fun JsonElement.toPrimitiveAny(): Any? = when (this) {
    is JsonPrimitive -> when {
        isString -> content
        booleanOrNull != null -> content == "true"
        intOrNull != null -> content.toInt()
        doubleOrNull != null -> content.toDouble()
        else -> content
    }
    else -> this.toString()
}

internal fun Map<String, JsonElement>.toAnyMap(): Map<String, Any?> =
    mapValues { (_, v) -> v.toPrimitiveAny() }

internal fun HaStateDto.toDomain(): HaState = HaState(
    entityId = entity_id,
    state = state,
    lastUpdated = last_updated,
    attributes = attributes.toAnyMap(),
    area = null,
)

internal fun HaHistoryEntryDto.toDomain(): HaHistoryPoint = HaHistoryPoint(
    timestamp = last_changed,
    batteryPercent = batteryPercentOrNull(),
)

/** ha.md §6 priority: `battery_level` → `battery` → `state` (parsed). */
private fun HaHistoryEntryDto.batteryPercentOrNull(): Int? {
    attributes["battery_level"]?.jsonPrimitive?.intOrNullSafe()?.let { return it }
    attributes["battery"]?.jsonPrimitive?.intOrNullSafe()?.let { return it }
    return state.trim().toIntOrNull()?.takeIf { it in 0..100 }
}

private fun JsonPrimitive.intOrNullSafe(): Int? = content.toIntOrNull()

// ─────────────────────── WebSocket listener ────────────────────────────

/**
 * Drives one HA WebSocket session:
 *
 * 1. `auth_required` received → send `{type:"auth",access_token}`
 * 2. `auth_ok`            → send baseline `{id:1,type:"get_states"}`
 * 3. baseline result       → send subscribe `{id:2,type:"subscribe_message",...}`
 * 4. `event` (id=2)        → filter entity_id whitelist → emit [HaStateChange]
 * 5. `pong` (any)          → reset the keepalive timer
 * 6. any failure / close  → propagate → outer flow reconnects (or short-
 *                            circuits on auth_invalid via [HaAuthException]).
 *
 * The ping cadence is `ha.md` §5.3: 30 s ping, two consecutive misses →
 * cancel the socket and let the outer flow reconnect.
 */
private class HaWsListener(
    private val token: String,
    private val onEvent: (HaEvent) -> Unit,
    private val entityIdsWhitelist: Set<String>,
    private val json: Json,
) : WebSocketListener() {

    private var webSocket: WebSocket? = null
    private val subscribed = AtomicBoolean(false)

    // Keepalive
    private val keepaliveRunning = AtomicBoolean(true)
    private val lastPongAtMs = AtomicLong(System.currentTimeMillis())
    private val pingCounter = AtomicLong(1)

    override fun onOpen(webSocket: WebSocket, response: Response) {
        this.webSocket = webSocket
        // Server sends `auth_required` first; we react in onMessage.
    }

    override fun onMessage(webSocket: WebSocket, text: String) {
        val obj = parseMessage(text) ?: return
        when (obj["type"]?.asString()) {
            "auth_required" -> webSocket.send(authPayload())
            "auth" -> {
                // Server may send `auth_ok` or `auth_invalid` here.
                // (handled below by the `auth_ok` / `auth_invalid` branch)
            }
            "auth_ok" -> webSocket.send(baselineRequest())
            "auth_invalid" -> throw HaAuthException("HA sent auth_invalid")
            "result" -> {
                val id = obj["id"]?.asLongOrNull()
                if (id == 1L) {
                    // Baseline fetched; issue subscription next.
                    webSocket.send(subscribePayload())
                } else if (id == 2L) {
                    subscribed.set(true)
                }
                // id == -1 (unsubscribe_all) is ack'd and ignored.
            }
            "event" -> handleStateChangeEvent(obj)
            "pong" -> lastPongAtMs.set(System.currentTimeMillis())
            "ping" -> {
                val id = obj["id"]?.asLongOrNull()
                webSocket.send(pongPayload(id))
            }
        }
    }

    override fun onMessage(webSocket: WebSocket, bytes: ByteString) {
        // HA WS uses text frames only; ignore unexpected binary messages.
    }

    override fun onFailure(webSocket: WebSocket, t: Throwable, response: Response?) {
        keepaliveRunning.set(false)
        onEvent(HaEvent.HaClientError(t))
    }

    override fun onClosed(webSocket: WebSocket, code: Int, reason: String) {
        keepaliveRunning.set(false)
    }

    override fun onClosing(webSocket: WebSocket, code: Int, reason: String) {
        webSocket.close(code, reason)
    }

    /**
     * Keepalive ping coroutine. Pings every 30 s; if we haven't seen a `pong`
     * in 60 s (two consecutive pings without a reply), close the socket.
     */
    fun startKeepalive(scope: CoroutineScope) = scope.launch {
        while (keepaliveRunning.get() && isActive) {
            kotlinx.coroutines.delay(30_000)
            val now = System.currentTimeMillis()
            if (now - lastPongAtMs.get() > 60_000) {
                keepaliveRunning.set(false)
                webSocket?.close(4_000, "ping timeout")
                return@launch
            }
            val id = pingCounter.getAndIncrement()
            webSocket?.send("""{"id":$id,"type":"ping"}""")
        }
    }

    // ─── helpers ────────────────────────────────────────────────────────

    private fun authPayload(): String =
        """{"type":"auth","access_token":${json.encodeToString(JsonPrimitive.serializer(), JsonPrimitive(token))}}"""

    private fun baselineRequest(): String = """{"id":1,"type":"get_states"}"""

    private fun subscribePayload(): String =
        """{"id":2,"type":"subscribe_message","event_type":"state_changed"}"""

    private fun pongPayload(id: Long?): String =
        if (id == null) """{"type":"pong"}""" else """{"id":$id,"type":"pong"}"""

    private fun parseMessage(text: String): JsonObject? = try {
        json.parseToJsonElement(text).jsonObject
    } catch (_: Exception) {
        null
    }

    private fun handleStateChangeEvent(envelope: JsonObject) {
        val event = envelope["event"]?.asJsonObjectOrNull() ?: return
        if (event["event_type"]?.asString() != "state_changed") return
        val newState = event["data"]
            ?.asJsonObjectOrNull()
            ?.get("new_state")
            ?.asJsonObjectOrNull()
            ?: return
        val entityId = newState["entity_id"]?.asString() ?: return
        if (entityId !in entityIdsWhitelist) return

        // Try to deserialise via the same DTO as REST; if HA sent a slightly
        // different shape, drop the event rather than throwing into the
        // reconnect loop.
        val dto = runCatching {
            json.decodeFromJsonElement(HaStateDto.serializer(), newState)
        }.getOrNull() ?: return
        onEvent(HaEvent.HaStateChange(entityId = entityId, state = dto.toDomain()))
    }
}

// ─────────────────────── JsonElement accessors ─────────────────────────

private fun JsonElement.asString(): String? = (this as? JsonPrimitive)?.contentOrNull

private fun JsonElement.asLongOrNull(): Long? = (this as? JsonPrimitive)?.content?.toLongOrNull()

private fun JsonElement.asJsonObjectOrNull(): JsonObject? = this as? JsonObject
