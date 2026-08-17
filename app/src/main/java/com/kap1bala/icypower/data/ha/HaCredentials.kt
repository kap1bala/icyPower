package com.kap1bala.icypower.data.ha

/**
 * Pair of credentials required to talk to a Home Assistant instance.
 *
 * - [baseUrl] is the user-facing URL (e.g. `http://homeassistant.local:8123`),
 *   trailing `/` trimmed. Stored in plain DataStore.
 * - [token] is the Long-Lived Access Token. Stored encrypted via
 *   [com.kap1bala.icypower.data.security.SecureStorage].
 *
 * Validation (e.g. URL format, token non-blank) is the caller's
 * responsibility — this is just a transport container.
 */
data class HaCredentials(
    val baseUrl: String,
    val token: String,
) {
    /** True iff both fields are non-blank. */
    val isComplete: Boolean
        get() = baseUrl.isNotBlank() && token.isNotBlank()
}
