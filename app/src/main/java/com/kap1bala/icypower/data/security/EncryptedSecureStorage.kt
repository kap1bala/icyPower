package com.kap1bala.icypower.data.security

import android.content.Context
import android.content.SharedPreferences
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKey
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * [SecureStorage] backed by [EncryptedSharedPreferences].
 *
 * Cold-start cost: the first time this is constructed, Android KeyStore
 * generates a master key. Expect ~200–500 ms on first launch — that's why
 * the caller ([com.kap1bala.icypower.IcyPowerApp]) holds this as a `lazy`
 * and never touches it during [android.app.Application.onCreate] eager init.
 *
 * All methods offload to [Dispatchers.IO]; callers can invoke them from
 * any dispatcher.
 */
class EncryptedSecureStorage(
    context: Context,
) : SecureStorage {

    private val prefs: SharedPreferences by lazy {
        val masterKey = MasterKey.Builder(context)
            .setKeyScheme(MasterKey.KeyScheme.AES256_GCM)
            .build()

        EncryptedSharedPreferences.create(
            context,
            FILE_NAME,
            masterKey,
            EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
            EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM,
        )
    }

    override suspend fun getToken(): String? = withContext(Dispatchers.IO) {
        prefs.getString(KEY_TOKEN, null)?.takeIf { it.isNotEmpty() }
    }

    override suspend fun putToken(token: String) = withContext(Dispatchers.IO) {
        prefs.edit().putString(KEY_TOKEN, token).apply()
    }

    override suspend fun clearToken() = withContext(Dispatchers.IO) {
        prefs.edit().remove(KEY_TOKEN).apply()
    }

    private companion object {
        const val FILE_NAME = "icy_power_secure_prefs"
        const val KEY_TOKEN = "ha_llat"
    }
}
