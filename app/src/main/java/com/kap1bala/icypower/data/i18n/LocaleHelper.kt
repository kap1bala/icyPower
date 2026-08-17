package com.kap1bala.icypower.data.i18n

import android.content.Context
import android.content.res.Configuration
import java.util.Locale

/**
 * Wraps a base [Context] with the given [locale] applied at the
 * Configuration level, so `R.string.*` lookups go through the right
 * resource bundle (`res/values-en`, `res/values-zh`, …).
 *
 * Why this exists:
 *   - MainActivity uses [Configuration.setLocale] + [Context.createConfigurationContext]
 *     (pure Android API), so we don't have to bring in `appcompat` /
 *     `AppCompatDelegate.setApplicationLocales`.
 *   - The wrapped context is what MainActivity passes to `super.attachBaseContext`
 *     — every subsequent `R.string.*` resolution in the Activity (and its
 *     Compose tree) then sees the chosen locale.
 *
 * Caveat: only the Locale field is touched. Setting `Locale.setDefault` is
 * left to the caller if needed; this helper is intentionally minimal.
 */
fun wrap(base: Context, locale: Locale): Context {
    val cfg = Configuration(base.resources.configuration)
    cfg.setLocale(locale)
    return base.createConfigurationContext(cfg)
}
