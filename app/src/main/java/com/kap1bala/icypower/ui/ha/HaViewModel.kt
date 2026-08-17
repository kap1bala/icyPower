package com.kap1bala.icypower.ui.ha

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.kap1bala.icypower.IcyPowerApp
import com.kap1bala.icypower.data.cycle.OverdueSeverity
import com.kap1bala.icypower.data.ha.HaClient
import com.kap1bala.icypower.data.ha.HaEvent
import com.kap1bala.icypower.data.ha.HaState
import com.kap1bala.icypower.data.ha.NoOpHaClient
import com.kap1bala.icypower.data.preferences.HaMonitorPreferences
import com.kap1bala.icypower.data.ha.HaAuthException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import java.io.IOException

/**
 * Drives the Home Assistant devices panel on the home screen.
 *
 * State machine (matches `feat.md §3.2` + plan §3):
 *   NotConfigured → NoOpHaClient (no URL/token configured).
 *   Loading      → Baseline `getStates()` in flight.
 *   Loaded       → At least one monitored entity; cards sorted by
 *                  severity (Danger > Warning > None), then by entityId.
 *   Empty        → HA reachable but no entities with battery attributes.
 *   Error        → IOException (network / 5xx); `errorMessage` set.
 *   Unauthorized → HaAuthException (401 / auth_invalid); user is
 *                  routed to /settings/ha to rotate the token.
 *
 * Live updates arrive via WebSocket (`subscribeStateChanges`). The
 * [HaClient] handles reconnection internally (exponential backoff
 * capped at 60s); transient blips flip `isReconnecting` for the
 * duration of the outage without nuking the existing card list.
 */
class HaViewModel(
    private val haClient: HaClient,
    private val monitorPrefs: HaMonitorPreferences,
) : ViewModel() {

    private val _state = MutableStateFlow(HaDevicesState())
    val state: StateFlow<HaDevicesState> = _state.asStateFlow()

    init {
        bootstrap()
    }

    /**
     * Initial fetch + WS subscription. Kept small so [init] can call it
     * directly; split only if the call site grows beyond a handful of
     * distinct stages.
     */
    private fun bootstrap() {
        viewModelScope.launch {
            if (haClient is NoOpHaClient) {
                _state.value = HaDevicesState(phase = HaPhase.NotConfigured)
                return@launch
            }
            _state.value = HaDevicesState(phase = HaPhase.Loading)

            val (warning, danger) = combine(
                monitorPrefs.warningThreshold,
                monitorPrefs.dangerThreshold,
            ) { w, d -> w to d }.first()

            // Baseline
            val baseline = runCatching { haClient.getStates() }
            baseline.fold(
                onSuccess = { states ->
                    handleBaseline(states, warning, danger)
                },
                onFailure = { e ->
                    when (e) {
                        is HaAuthException ->
                            _state.value = HaDevicesState(phase = HaPhase.Unauthorized)
                        else ->
                            _state.value = HaDevicesState(
                                phase = HaPhase.Error,
                                errorMessage = e.message ?: e::class.simpleName,
                            )
                    }
                },
            )

            // Subscribe to live updates. Skip subscription if we don't
            // have any monitored entities — saves a WS connection.
            val monitoredIds = _state.value.devices.map { it.entityId }
            if (monitoredIds.isNotEmpty()) {
                haClient.subscribeStateChanges(monitoredIds)
                    .onEach { event -> handleWsEvent(event, warning, danger) }
                    .launchIn(viewModelScope)
            }
        }
    }

    fun refresh() {
        // Reset to Loading and re-run bootstrap. Cheap enough at v1
        // scale (≤ a few hundred entities) that we don't bother with a
        // separate "incremental refresh" path.
        _state.value = HaDevicesState(phase = HaPhase.Loading)
        bootstrap()
    }

    private fun handleBaseline(states: List<HaState>, warning: Int, danger: Int) {
        val cards = states.mapNotNull { it.toMonitoredCardOrNull(warning, danger) }
            .sortedWith(severityThenId())
        _state.value = HaDevicesState(
            phase = if (cards.isEmpty()) HaPhase.Empty else HaPhase.Loaded,
            devices = cards,
        )
    }

    private fun handleWsEvent(event: HaEvent, warning: Int, danger: Int) {
        when (event) {
            is HaEvent.HaStateChange -> {
                val current = _state.value
                // If we hadn't loaded anything yet, just keep the current
                // phase — the WS event is incidental; the baseline tells
                // us what to render.
                val updated = current.devices.toMutableList()
                val idx = updated.indexOfFirst { it.entityId == event.entityId }
                val newCard = event.state.toMonitoredCardOrNull(warning, danger)
                if (newCard == null) {
                    if (idx >= 0) updated.removeAt(idx)
                } else if (idx >= 0) {
                    updated[idx] = newCard
                } else {
                    updated.add(newCard)
                }
                _state.value = current.copy(
                    phase = HaPhase.Loaded,
                    devices = updated.sortedWith(severityThenId()),
                    isReconnecting = false,
                )
            }
            is HaEvent.HaClientError -> {
                // A live WS error after we've loaded: keep cards, flag
                // "reconnecting" so the user knows the live data may be
                // stale. A transient blip won't be mistaken for "Empty".
                val current = _state.value
                if (event.cause is HaAuthException) {
                    _state.value = current.copy(phase = HaPhase.Unauthorized)
                } else if (current.devices.isNotEmpty()) {
                    _state.value = current.copy(
                        isReconnecting = true,
                        errorMessage = event.cause.message ?: event.cause::class.simpleName,
                    )
                } else {
                    _state.value = HaDevicesState(
                        phase = HaPhase.Error,
                        errorMessage = event.cause.message ?: event.cause::class.simpleName,
                    )
                }
            }
        }
    }

    private fun HaState.toMonitoredCardOrNull(warning: Int, danger: Int): HaDeviceCard? {
        val percent = batteryPercent() ?: return null  // ha.md §6: skip non-battery entities
        val name = attributes["friendly_name"]?.toString()?.takeIf { it.isNotBlank() }
            ?: entityId
        val areaStr = attributes["area"]?.toString()?.takeIf { it.isNotBlank() }
        val severity = when {
            percent < danger -> OverdueSeverity.Danger
            percent < warning -> OverdueSeverity.Warning
            else -> OverdueSeverity.None
        }
        return HaDeviceCard(
            entityId = entityId,
            name = name,
            area = areaStr,
            batteryPercent = percent,
            severity = severity,
            lastUpdated = lastUpdated,
        )
    }

    /** Severity (Danger > Warning > None), then entityId for stable order. */
    private fun severityThenId(): Comparator<HaDeviceCard> =
        compareByDescending<HaDeviceCard> { it.severity.ordinal }
            .thenBy { it.entityId }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val app = this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as IcyPowerApp
                HaViewModel(
                    haClient = app.haClient,
                    monitorPrefs = app.haMonitorPreferences,
                )
            }
        }
    }
}

// ────────────────────────────────────────────────────────────────────
// Public types
// ────────────────────────────────────────────────────────────────────

enum class HaPhase {
    /** NoOpHaClient — URL/token not configured. */
    NotConfigured,
    /** Initial baseline in flight. */
    Loading,
    /** HA reachable, at least one monitored entity. */
    Loaded,
    /** HA reachable, but no entities have a battery attribute. */
    Empty,
    /** IOException or 5xx. */
    Error,
    /** 401 / auth_invalid — token is bad. */
    Unauthorized,
}

data class HaDeviceCard(
    val entityId: String,
    val name: String,
    val area: String?,
    val batteryPercent: Int,
    val severity: OverdueSeverity,
    val lastUpdated: String,
)

data class HaDevicesState(
    val phase: HaPhase = HaPhase.Loading,
    val devices: List<HaDeviceCard> = emptyList(),
    val errorMessage: String? = null,
    val isReconnecting: Boolean = false,
)