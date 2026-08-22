package com.kap1bala.icypower.ui.settings

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.widget.Toast
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.kap1bala.icypower.IcyPowerApp
import com.kap1bala.icypower.R
import com.kap1bala.icypower.data.ha.HaClient
import com.kap1bala.icypower.data.preferences.HaMonitorPreferences
import com.kap1bala.icypower.data.preferences.HaPreferences
import com.kap1bala.icypower.data.security.SecureStorage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.IOException
import java.net.SocketTimeoutException
import java.net.UnknownHostException
import javax.net.ssl.SSLException

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
    private val monitorPrefs: HaMonitorPreferences,
) : ViewModel() {

    private val _state = MutableStateFlow(HaSettingsState())
    val state: StateFlow<HaSettingsState> = _state.asStateFlow()

    init {
        // Hydrate the form with the currently-persisted URL **and** token.
        // The token comes back masked (PasswordVisualTransformation) — the
        // user only sees it unmasked after tapping "显示". Keeping the
        // value in `tokenDraft` means an unchanged Save won't re-write it
        // to disk (compare against `savedToken`), and gives us the raw
        // value for the copy-to-clipboard button.
        viewModelScope.launch {
            val savedUrl = prefs.baseUrl.first().orEmpty()
            val savedToken = secureStorage.getToken()?.takeIf { it.isNotEmpty() }
            val warning = monitorPrefs.warningThreshold.first()
            val danger = monitorPrefs.dangerThreshold.first()
            _state.value = _state.value.copy(
                baseUrl = savedUrl,
                tokenDraft = savedToken.orEmpty(),
                savedToken = savedToken,
                warningThreshold = warning,
                dangerThreshold = danger,
            )
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
     * Move the "中电量（预警）" threshold. Enforces `danger < warning`
     * by nudging [dangerThreshold] down if the user slides warning below it.
     */
    fun onWarningThresholdChange(value: Int) {
        val clamped = value.coerceIn(MIN_THRESHOLD, MAX_THRESHOLD)
        val s = _state.value
        _state.value = s.copy(
            warningThreshold = clamped,
            dangerThreshold = if (s.dangerThreshold >= clamped)
                (clamped - 1).coerceAtLeast(MIN_THRESHOLD)
            else
                s.dangerThreshold,
        )
    }

    /**
     * Move the "低电量（危险）" threshold. Enforces `danger < warning`
     * by nudging [warningThreshold] up if the user slides danger above it.
     */
    fun onDangerThresholdChange(value: Int) {
        val clamped = value.coerceIn(MIN_THRESHOLD, MAX_THRESHOLD)
        val s = _state.value
        _state.value = s.copy(
            dangerThreshold = clamped,
            warningThreshold = if (s.warningThreshold <= clamped)
                (clamped + 1).coerceAtMost(MAX_THRESHOLD)
            else
                s.warningThreshold,
        )
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
                errorReason = null,
            )
            return
        }
        _state.value = current.copy(
            phase = Phase.Testing,
            urlError = false,
            statusMessage = null,
            errorReason = null,
        )
        viewModelScope.launch {
            val result = withContext(Dispatchers.IO) { runCatching { haClient.probe() } }
            val phase = _state.value.phase
            if (phase != Phase.Testing) return@launch  // state was reset
            result.fold(
                onSuccess = { ok ->
                    _state.value = _state.value.copy(
                        phase = Phase.Idle,
                        statusMessage = if (ok) Status.Ok else Status.Failed,
                        errorReason = if (ok) null else "✗ Server reachable but /api/states failed",
                    )
                },
                onFailure = { e ->
                    _state.value = _state.value.copy(
                        phase = Phase.Idle,
                        statusMessage = Status.Failed,
                        errorReason = decodeReason(e),
                    )
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
            // Only re-write the encrypted token when it actually changed
            // (skipping an unchanged value avoids a pointless KeyStore /
            // SharedPreferences write on every save).
            if (token.isNotEmpty() && token != current.savedToken) {
                secureStorage.putToken(token)
            }
            // Persist the low/mid battery standards alongside the connection
            // (the sliders guarantee danger < warning, so setThresholds'
            // require() can't trip).
            monitorPrefs.setThresholds(current.warningThreshold, current.dangerThreshold)
            _state.value = _state.value.copy(
                phase = Phase.Idle,
                savedToken = token.ifEmpty { current.savedToken },
            )
            // Rebuild the app-scoped HA client from the freshly persisted
            // credentials BEFORE recreating — otherwise the home panel's
            // next bootstrap would still read the stale (often NoOp) client.
            (context.applicationContext as IcyPowerApp).refreshHaClient()
            // Confirm the save even though we immediately recreate() —
            // Toast is Activity-independent so it survives the restart.
            Toast.makeText(
                context,
                context.getString(R.string.ha_status_saved),
                Toast.LENGTH_SHORT,
            ).show()
            context.findActivity()?.recreate()
        }
    }

    /**
     * Clear **only** the token — leave the URL alone. The user gets a
     * dedicated "仅清除 Token" button instead of being forced to wipe
     * the whole connection (URL + token) when they only want to rotate
     * the credential.
     */
    fun clearTokenAndRecreate(context: Context) {
        _state.value = _state.value.copy(phase = Phase.Saving)
        viewModelScope.launch {
            secureStorage.clearToken()
            _state.value = _state.value.copy(
                phase = Phase.Idle,
                savedToken = null,
                tokenDraft = "",
                statusMessage = Status.TokenCleared,
                errorReason = null,
            )
            (context.applicationContext as IcyPowerApp).refreshHaClient()
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
            (context.applicationContext as IcyPowerApp).refreshHaClient()
            context.findActivity()?.recreate()
        }
    }

    companion object {
        const val MIN_THRESHOLD = 1
        const val MAX_THRESHOLD = 100
        const val DEFAULT_WARNING = 20
        const val DEFAULT_DANGER = 10

        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val app = this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as IcyPowerApp
                HaViewModel(
                    prefs = app.haPreferences,
                    secureStorage = app.secureStorage,
                    haClient = app.haClient,
                    monitorPrefs = app.haMonitorPreferences,
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
    /** Human-readable explanation of the most recent probe failure (null if not failed). */
    val errorReason: String? = null,
    /** The token as last written to disk — used to skip redundant writes. */
    val savedToken: String? = null,
    /** Battery level below this → Warning (yellow). Configurable on-screen. */
    val warningThreshold: Int = HaViewModel.DEFAULT_WARNING,
    /** Battery level below this → Danger (red). Configurable on-screen. */
    val dangerThreshold: Int = HaViewModel.DEFAULT_DANGER,
)

enum class Phase { Idle, Testing, Saving }

/**
 * User-facing status — null means "no message yet".
 * Distinct from [Phase] which describes *what the system is doing*;
 * [Status] describes *what was just observed*.
 */
enum class Status { Ok, Failed, Cleared, TokenCleared }

/**
 * Translate a probe failure into a one-liner that's actionable for a
 * non-technical user. Order matters — the more specific match wins.
 *
 * The canonical case we care about: OkHttp's
 *   `SSLException: Unable to parse TLS packet header`
 * means the client tried plain HTTP but the server is speaking HTTPS
 * (or vice versa). That's almost always the URL scheme being wrong.
 */
private fun decodeReason(e: Throwable): String = when (e) {
    is SSLException -> "协议不匹配 — 客户端发了 HTTP 但服务器是 HTTPS（或反之）。把 URL 改成 https:// 试试（HA 自 2021 默认是 HTTPS）。"
    is SocketTimeoutException -> "连接超时 — 检查网络是否可达、HA 服务器是否在线。"
    is UnknownHostException -> "无法解析主机名 — 检查 URL 是否拼写正确。"
    else -> {
        // Generic — still useful when the framework bubbles up an
        // unexpected exception we didn't think to map here.
        val msg = e.message
        if (msg.isNullOrBlank()) e::class.simpleName ?: "未知错误" else msg
    }
}

/** Walk up a [Context]'s wrapper chain to find the host [Activity]. */
private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}