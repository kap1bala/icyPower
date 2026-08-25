package com.kap1bala.icypower.data.i18n

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TemporaryFolder
import java.io.File

/**
 * Unit tests for [LocalePreferences].
 *
 * Pinned behaviours:
 *  - Empty store → `tag` emits `null` (not yet decided).
 *  - `setLocale` persists the locale's tag verbatim, including the empty
 *    string for `AppLocale.System`.
 *  - Reading after write reflects the latest value (no caching surprises).
 */
class LocalePreferencesTest {

    @get:Rule
    val tempFolder = TemporaryFolder()

    private lateinit var dataStoreFile: File
    private lateinit var scope: CoroutineScope
    private lateinit var dataStore: DataStore<Preferences>
    private lateinit var prefs: LocalePreferences

    @Before
    fun setUp() {
        dataStoreFile = tempFolder.newFile("locale.preferences_pb")
        scope = CoroutineScope(Job())
        dataStore = PreferenceDataStoreFactory.create(scope = scope) { dataStoreFile }
        prefs = LocalePreferences(dataStore)
    }

    @After
    fun tearDown() {
        runBlocking { scope.coroutineContext[Job]?.cancelAndJoin() }
    }

    @Test
    fun tag_nullWhenNeverPersisted() = runBlocking {
        assertNull(prefs.tag.first())
    }

    @Test
    fun setLocale_persistsChineseTag() = runBlocking {
        prefs.setLocale(AppLocale.Chinese)
        assertEquals("zh", prefs.tag.first())
    }

    @Test
    fun setLocale_persistsEnglishTag() = runBlocking {
        prefs.setLocale(AppLocale.English)
        assertEquals("en", prefs.tag.first())
    }

    @Test
    fun setLocale_persistsSystemAsEmptyString() = runBlocking {
        prefs.setLocale(AppLocale.Chinese)
        // Round-trip back to System — must persist the empty tag (not null).
        prefs.setLocale(AppLocale.System)
        assertEquals("", prefs.tag.first())
    }

    @Test
    fun setLocale_overwritesPreviousValue() = runBlocking {
        prefs.setLocale(AppLocale.Chinese)
        prefs.setLocale(AppLocale.English)
        assertEquals("en", prefs.tag.first())
    }
}