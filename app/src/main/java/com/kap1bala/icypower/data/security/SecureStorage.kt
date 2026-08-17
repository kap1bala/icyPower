package com.kap1bala.icypower.data.security

/**
 * Secure, locally encrypted storage for small secrets (currently: HA LLAT).
 *
 * Contract:
 *   - All methods are `suspend` so implementations are free to offload to
 *     [kotlinx.coroutines.Dispatchers.IO] internally. Callers don't need
 *     to wrap.
 *   - Storage is local-only. Tokens never leave the device.
 *   - `getToken()` returns `null` when no token has been stored yet —
 *     distinguishes "never set" from "set to empty string".
 */
interface SecureStorage {
    suspend fun getToken(): String?
    suspend fun putToken(token: String)
    suspend fun clearToken()
}
