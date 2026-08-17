package com.kap1bala.icypower

import android.app.Application
import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore
import com.kap1bala.icypower.data.ha.HaClient
import com.kap1bala.icypower.data.ha.NoOpHaClient
import com.kap1bala.icypower.data.preferences.ThemePreferences
import com.kap1bala.icypower.data.security.EncryptedSecureStorage
import com.kap1bala.icypower.data.security.SecureStorage
import okhttp3.OkHttpClient

// Top-level DataStore extension property. `preferencesDataStore` is a
// property delegate factory — declaring it at file scope (not inside a
// companion) is required so it can lazily capture `applicationContext`.
private val Context.icyPowerDataStore: DataStore<Preferences>
        by preferencesDataStore(name = "icy_power_preferences")

/**
 * Application root and ad-hoc dependency container.
 *
 * We deliberately avoid Hilt / Koin in v1 — the dependency graph is small
 * and a lazy-property container is enough. If it grows past ~10 entries,
 * reconsider.
 *
 * Important invariants:
 *   - [okHttpClient] is lazily built (the first HA call may take >100 ms
 *     to set up connection pools).
 *   - [secureStorage] is lazily built — first touch generates a master key
 *     in Android KeyStore (200–500 ms one-time cost).
 *   - No network calls happen during `onCreate()`.
 */
class IcyPowerApp : Application() {

    /** App-scoped OkHttp client. Single instance, shared across HA calls. */
    val okHttpClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            // Tuned for HA's LAN deployment; revisit if we add timeouts.
            .build()
    }

    /** Encrypted store for HA LLAT (see feat.md §3). */
    val secureStorage: SecureStorage by lazy {
        EncryptedSecureStorage(this)
    }

    /** v1 HA client is a no-op until PR #3 lands a real OkHttp implementation. */
    val haClient: HaClient by lazy { NoOpHaClient }

    /** Theme preferences. */
    val themePreferences: ThemePreferences by lazy {
        ThemePreferences(applicationContext.icyPowerDataStore)
    }

    override fun onCreate() {
        super.onCreate()
        // Intentionally empty: DataStore is created lazily via the property
        // delegate on first access, and we never want to do crypto work in
        // onCreate (would slow cold start).
    }
}
