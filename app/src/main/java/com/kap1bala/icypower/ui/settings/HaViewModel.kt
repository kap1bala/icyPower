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
import com.kap1bala.icypower.data.ha.HaClient
import com.kap1bala.icypower.data.preferences.HaPreferences
import com.kap1bala.icypower.data.security.SecureStorage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Owns the Home Assistant connection form state and persists changes.
 *
 * Storage split (matches the rest of the project):
 *   - Base URL is plain-text in [HaPreferences] (DataStore).
 *   - Long-Lived Access Token is encrypted via [SecureStorage]
 *     (EncryptedSharedPreferences).
 *
 * Both [saveAndRecreate] and [clearAllAndRecreate] ask the host Activity
 * to `recreate()` after the writes settle. Recreation re-walks
 * [MainActivity.attachBaseContext], which re-evaluates [IcyPowerApp.haClient]
 * against the freshly persisted credentials — so a brand-new
 * [OkHttpHaClient] (or [NoOpHaClient] when both fields are blank) is
 * constructed on the next Activity attach. The lazy-property pattern in
 * [IcyPowerApp] doesn't need to be refactored for this to work.
 */
class HaViewModel(
    private val prefs: HaPreferences,
    private val secureStorage: SecureStorage,
    private val haClient: HaClient,
) : ViewModel() {

    private val _state = MutableStateFlow(HaSettingsState())
    val state: StateFlow<HaSettingsState> = _state.asStateFlow()

    init {
        // Hydrate the form with the currently-persisted URL. The token is
        // never re-loaded into the form (security: token stays in
        // EncryptedSharedPreferences and we never round-trip it back
        // through Compose state after the user enters it).
        viewModelScope.launch {
            val savedUrl = prefs.baseUrl.first().orEmpty()
            _state.value = _state.value.copy(baseUrl = savedUrl)
        }
    }

    fun onBaseUrlChange(value: String) {
        _state.value = _state.value.copy(baseUrl = value, statusMessage = null)
    }

    fun onTokenDraftChange(value: String) {
        _state.value = _state.value.copy(tokenDraft = value, statusMessage = null)
    }

    fun onToggleShowToken() {
        _state.value = _state.value.copy(showToken = !_state.value.showToken)
    }

    /**
     * Probe the configured URL + draft token without persisting anything.
     * If the URL field is empty / blank the probe is skipped and the
     * form's isError flag is set on the URL field.
     *
     * The probe actually lives on [haClient] — which is the *currently*
     * configured client (possibly NoOp if no credentials were set). For a
     * tighter test we'd construct a transient OkHttpHaClient from the
     * in-flight form values; that's a future polish.
     */
    fun testConnection() {
        val current = _state.value
        if (current.baseUrl.isBlank()) {
            _state.value = current.copy(
                urlError = true,
                statusMessage = null,
            )
            return
        }
        _state.value = current.copy(
            phase = Phase.Testing,
            urlError = false,
            statusMessage = null,
        )
        viewModelScope.launch {
            val result = withContext(Dispatchers.IO) { runCatching { haClient.probe() } }
            val phase = _state.value.phase
            if (phase != Phase.Testing) return@launch  // state was reset
            _state.value = _state.value.copy(
                phase = Phase.Idle,
                statusMessage = result.getOrDefault(false).let { ok ->
                    if (ok) Status.Ok else Status.Failed
                },
            )
        }
    }

    fun saveAndRecreate(context: Context) {
        val current = _state.value
        if (current.baseUrl.isBlank()) {
            _state.value = current.copy(urlError = true)
            return
        }
        _state.value = current.copy(phase = Phase.Saving, urlError = false)
        viewModelScope.launch {
            prefs.setBaseUrl(current.baseUrl.trim())
            val token = current.tokenDraft.trim()
            if (token.isNotEmpty()) {
                secureStorage.putToken(token)
            }
            // Wipe the draft so it doesn't sit in memory waiting for a
            // process death / screen rotation.
            _state.value = _state.value.copy(
                phase = Phase.Idle,
                tokenDraft = "",
            )
            context.findActivity()?.recreate()
        }
    }

    fun clearAllAndRecreate(context: Context) {
        _state.value = _state.value.copy(phase = Phase.Saving)
        viewModelScope.launch {
            prefs.clear()
            secureStorage.clearToken()
            _state.value = _state.value.copy(
                phase = Phase.Idle,
                baseUrl = "",
                tokenDraft = "",
                statusMessage = Status.Cleared,
            )
            context.findActivity()?.recreate()
        }
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val app = this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as IcyPowerApp
                HaViewModel(
                    prefs = app.haPreferences,
                    secureStorage = app.secureStorage,
                    haClient = app.haClient,
                )
            }
        }
    }
}

/** Discriminated state for the form. */
data class HaSettingsState(
    val baseUrl: String = "",
    val tokenDraft: String = "",
    val showToken: Boolean = false,
    val urlError: Boolean = false,
    val phase: Phase = Phase.Idle,
    val statusMessage: Status? = null,
)

enum class Phase { Idle, Testing, Saving }

/**
 * User-facing status — null means "no message yet".
 * Distinct from [Phase] which describes *what the system is doing*;
 * [Status] describes *what was just observed*.
 */
enum class Status { Ok, Failed, Cleared }

/** Walk up a [Context]'s wrapper chain to find the host [Activity]. */
private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}