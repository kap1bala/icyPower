package com.kap1bala.icypower

import android.app.Application
import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.preferencesDataStore
import com.kap1bala.icypower.data.cycle.CycleDeviceRepository
import com.kap1bala.icypower.data.ha.HaClient
import com.kap1bala.icypower.data.ha.NoOpHaClient
import com.kap1bala.icypower.data.ha.OkHttpHaClient
import com.kap1bala.icypower.data.i18n.AppLocale
import com.kap1bala.icypower.data.i18n.LocalePreferences
import com.kap1bala.icypower.data.preferences.HaPreferences
import com.kap1bala.icypower.data.preferences.ThemePreferences
import com.kap1bala.icypower.data.security.EncryptedSecureStorage
import com.kap1bala.icypower.data.security.SecureStorage
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
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
 *   - [haClient] picks [OkHttpHaClient] iff both a base URL and a token
 *     are configured; otherwise [NoOpHaClient]. The selection runs a brief
 *     blocking read of the DataStore / EncryptedSharedPreferences — that
 *     cost is dominated by the KeyStore master-key generation that already
 *     gates `secureStorage`, so adding DataStore doesn't change cold-start
 *     materially.
 *   - No network calls happen during `onCreate()`.
 */
class IcyPowerApp : Application() {

    /** App-scoped OkHttp client. Single instance, shared across HA calls. */
    val okHttpClient: OkHttpClient by lazy {
        OkHttpClient.Builder()
            // Conservative timeouts: HA LAN queries are usually <1 s, but a
            // slow NAS or stuck DNS shouldn't block the UI indefinitely.
            .connectTimeout(java.time.Duration.ofSeconds(10))
            .readTimeout(java.time.Duration.ofSeconds(15))
            .writeTimeout(java.time.Duration.ofSeconds(15))
            .build()
    }

    /** Encrypted store for HA LLAT (see feat.md §3). */
    val secureStorage: SecureStorage by lazy {
        EncryptedSecureStorage(this)
    }

    /** Plain-text HA connection settings (base URL). Token still lives in [secureStorage]. */
    val haPreferences: HaPreferences by lazy {
        HaPreferences(applicationContext.icyPowerDataStore)
    }

    /** User-chosen display locale (System / Chinese / English). */
    val localePreferences: LocalePreferences by lazy {
        LocalePreferences(applicationContext.icyPowerDataStore)
    }

    /**
     * v1 HA client. Returns [OkHttpHaClient] iff both [HaPreferences.baseUrl]
     * and [SecureStorage.getToken] have non-blank values; [NoOpHaClient] otherwise.
     *
     * `runBlocking` here is deliberate: ViewModels consume [haClient]
     * synchronously through their factories and there's no clean way to
     * suspend inside their constructors. The cost is bounded — see the
     * class doc above.
     */
    val haClient: HaClient by lazy {
        val baseUrl = runBlocking { haPreferences.baseUrl.first() }
        val token = runBlocking { secureStorage.getToken() }
        if (!baseUrl.isNullOrBlank() && !token.isNullOrEmpty()) {
            OkHttpHaClient(
                baseUrl = baseUrl.trimEnd('/'),
                token = token,
                okHttpClient = okHttpClient,
            )
        } else {
            NoOpHaClient
        }
    }

    /** Theme preferences. */
    val themePreferences: ThemePreferences by lazy {
        ThemePreferences(applicationContext.icyPowerDataStore)
    }

    /** Repository for user-tracked charge-cycle devices. */
    val cycleDeviceRepository: CycleDeviceRepository by lazy {
        CycleDeviceRepository(applicationContext.icyPowerDataStore)
    }

    /**
     * Synchronous one-shot read of the persisted [AppLocale], used by
     * [com.kap1bala.icypower.MainActivity.attachBaseContext] which cannot
     * suspend. Bounded by the `runBlocking` that already gates [haClient].
     */
    fun initialLocale(): AppLocale = runBlocking {
        AppLocale.fromTag(localePreferences.tag.first())
    }

    override fun onCreate() {
        super.onCreate()
        // Intentionally empty: DataStore is created lazily via the property
        // delegate on first access, and we never want to do crypto work in
        // onCreate (would slow cold start).
    }
}
