package com.kap1bala.icypower.data.i18n

import org.junit.Assert.assertEquals
import org.junit.Test

/**
 * Unit tests for [AppLocale.fromTag].
 *
 * `resolve(ctx, stored)` is intentionally NOT tested here — it depends on
 * `Context.resources.configuration`, which is an Android-framework surface
 * requiring Robolectric or an instrumentation test to cover properly. That
 * belongs in androidTest, not in `app/src/test`.
 */
class AppLocaleTest {

    @Test
    fun fromTag_emptyStringIsSystem() {
        // System uses the empty string as its persistence key by design
        // (see AppLocale.kt doc), so the absence of a key AND the explicit
        // "follow system" choice collapse to the same value.
        assertEquals(AppLocale.System, AppLocale.fromTag(""))
    }

    @Test
    fun fromTag_nullIsSystem() {
        // Pre-migration or never-set cases yield null from DataStore;
        // we treat those the same as the empty string.
        assertEquals(AppLocale.System, AppLocale.fromTag(null))
    }

    @Test
    fun fromTag_zhIsChinese() {
        assertEquals(AppLocale.Chinese, AppLocale.fromTag("zh"))
    }

    @Test
    fun fromTag_enIsEnglish() {
        assertEquals(AppLocale.English, AppLocale.fromTag("en"))
    }

    @Test
    fun fromTag_unknownFallsBackToSystem() {
        // A persisted value we don't recognise (e.g. legacy / corrupted)
        // shouldn't crash — default to System so the app still launches.
        assertEquals(AppLocale.System, AppLocale.fromTag("fr"))
        assertEquals(AppLocale.System, AppLocale.fromTag("garbage"))
    }

    @Test
    fun fromTag_isRoundTripStable() {
        // Each constant's `tag` must round-trip back to itself.
        for (locale in AppLocale.entries) {
            assertEquals(locale, AppLocale.fromTag(locale.tag))
        }
    }
}