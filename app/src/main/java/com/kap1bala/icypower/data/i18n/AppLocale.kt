package com.kap1bala.icypower.data.i18n

import android.content.Context
import java.util.Locale

/**
 * The user-facing app locale, persisted to DataStore.
 *
 * - [System] follows the device locale (resolved at Activity-create time
 *   via [resolve]; empty persistence key).
 * - [Chinese] and [English] are explicit overrides; resource resolution
 *   follows `res/values-zh/strings.xml` and `res/values-en/strings.xml`
 *   respectively when these are selected.
 *
 * Storage key shape: `""` for [System] so that the absence of a key and
 * the explicit "follow system" setting are indistinguishable to callers —
 * both fall back to the device locale.
 */
enum class AppLocale(val tag: String) {
    /** Follow the OS locale. Empty persistence key by design. */
    System(""),

    Chinese("zh"),

    English("en"),
    ;

    companion object {
        fun fromTag(tag: String?): AppLocale =
            entries.firstOrNull { it.tag == tag } ?: System

        /**
         * Resolve the persisted preference to a concrete [Locale].
         *
         * - For explicit choices ([Chinese] / [English]) we trust the tag.
         * - For [System] we read the active Configuration's primary locale;
         *   if it can't be determined (e.g. a stripped-down image without
         *   any locale pack), fall back to [Locale.ENGLISH].
         */
        fun resolve(ctx: Context, stored: AppLocale): Locale {
            if (stored != System) return Locale.forLanguageTag(stored.tag)
            val cfg = ctx.resources.configuration
            val platformLocale = if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.N) {
                cfg.locales[0]
            } else {
                @Suppress("DEPRECATION")
                cfg.locale
            }
            return platformLocale?.let { Locale.forLanguageTag(it.toLanguageTag()) }
                ?: Locale.ENGLISH
        }
    }
}
