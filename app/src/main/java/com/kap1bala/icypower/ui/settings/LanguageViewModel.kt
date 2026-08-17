package com.kap1bala.icypower.ui.settings

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.kap1bala.icypower.IcyPowerApp
import com.kap1bala.icypower.data.i18n.AppLocale
import com.kap1bala.icypower.data.i18n.LocalePreferences
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * Owns the user's [AppLocale] choice.
 *
 * Reads come straight from [LocalePreferences] (DataStore). Writes call
 * [LocalePreferences.setLocale] and then trigger an `Activity.recreate()`
 * so the new Configuration is applied immediately — the next
 * [com.kap1bala.icypower.MainActivity.attachBaseContext] call will pick
 * up the freshly-stored tag.
 */
class LanguageViewModel(
    private val prefs: LocalePreferences,
) : ViewModel() {

    val locale: StateFlow<AppLocale> = prefs.tag
        .let { tagFlow ->
            kotlinx.coroutines.flow.flow {
                tagFlow.collect { emit(AppLocale.fromTag(it)) }
            }
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = AppLocale.System,
        )

    /**
     * Persist the new locale and ask the host Activity to recreate itself.
     * The new view tree will load `res/values-<locale>/strings.xml`.
     *
     * The recreate **must** happen *after* [prefs.setLocale] returns —
     * [MainActivity.attachBaseContext] reads the persisted tag via
     * `runBlocking { tag.first() }` to seed the Configuration; if we
     * recreate before the write commits, the new Activity attaches with
     * the stale (empty / previous) tag and the UI silently reverts.
     */
    fun setLocale(context: Context, locale: AppLocale) {
        viewModelScope.launch {
            prefs.setLocale(locale)
            context.findActivity()?.recreate()
        }
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val app = this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as IcyPowerApp
                LanguageViewModel(app.localePreferences)
            }
        }
    }
}

/** Walk up a [Context]'s wrapper chain looking for an [Activity]. */
private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}
