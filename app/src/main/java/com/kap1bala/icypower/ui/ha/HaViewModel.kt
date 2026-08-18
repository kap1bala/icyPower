package com.kap1bala.icypower.ui.ha

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.kap1bala.icypower.IcyPowerApp
import com.kap1bala.icypower.data.cycle.OverdueSeverity
import com.kap1bala.icypower.data.ha.HaAuthException
import com.kap1bala.icypower.data.ha.HaClient
import com.kap1bala.icypower.data.ha.HaEvent
import com.kap1bala.icypower.data.ha.HaState
import com.kap1bala.icypower.data.ha.NoOpHaClient
import com.kap1bala.icypower.data.preferences.HaMonitorPreferences
import com.kap1bala.icypower.data.preferences.HaMonitoredDevicesRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collect
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.launch
import java.io.IOException
import kotlinx.coroutines.Job

/**
 * Drives the Home Assistant devices panel on the home screen.
 *
 * State machine (matches `feat.md §3.2`):
 *   NotConfigured → NoOpHaClient (no URL/token configured).
 *   Loading      → Baseline `getStates()` in flight.
 *   Loaded       → Cards built from the user-picked subset of HA entities.
 *   Empty        → Either HA has no battery entities at all, OR the user
 *                  has unchecked every battery entity (the UI prompts
 *                  them to pick via /settings/ha/devices).
 *   Error        → IOException (network / 5xx); `errorMessage` set.
 *   Unauthorized → HaAuthException (401 / auth_invalid); user is routed
 *                  to /settings/ha to rotate the token.
 *
 * Selection model:
 *   - `HaMonitoredDevicesRepository` is the source of truth for which
 *     entities to display.
 *   - The view model pulls `monitoredIds.first()` on bootstrap, filters
 *     the HA baseline through that set, and uses the same set as the
 *     WS `subscribeStateChanges` whitelist. WS events for entities the
 *     user later un-picks are filtered server-side and never delivered
 *     to this ViewModel — by the time the user re-opens the device
 *     list and re-picks, the next `refresh()` rebuilds the cards.
 *
 * Live updates arrive via WebSocket (`subscribeStateChanges`). The
 * [HaClient] handles reconnection internally (exponential backoff
 * capped at 60s); transient blips flip `isReconnecting` for the
 * duration of the outage without nuking the existing card list.
 */
class HaViewModel(
    private val haClient: HaClient,
    private val monitorPrefs: HaMonitorPreferences,
    private val monitoredRepo: HaMonitoredDevicesRepository,
) : ViewModel() {

    private val _state = MutableStateFlow(HaDevicesState())
    val state: StateFlow<HaDevicesState> = _state.asStateFlow()

    /**
     * Long-lived baseline + WS subscription. Reacts to every change in
     * [HaMonitoredDevicesRepository.monitoredIds] — when the user flips
     * a switch in /settings/ha/devices, the home panel re-fetches the
     * baseline and resubscribes the WS without an app restart.
     *
     * Stored as a [Job] so [refresh] can cancel and replace it.
     */
    private var bootstrapJob: Job? = null

    init {
        startMonitoring()
    }

    fun refresh() {
        bootstrapJob?.cancel()
        _state.value = HaDevicesState(phase = HaPhase.Loading)
        startMonitoring()
    }

    private fun startMonitoring() {
        bootstrapJob = viewModelScope.launch {
            if (haClient is NoOpHaClient) {
                _state.value = HaDevicesState(phase = HaPhase.NotConfigured)
                return@launch
            }

            // Thresholds change rarely; one snapshot is fine.
            val (warning, danger) = combine(
                monitorPrefs.warningThreshold,
                monitorPrefs.dangerThreshold,
            ) { w, d -> w to d }.first()

            // The interesting loop: every emit of `monitoredIds`
            // (initial value + each toggle on /settings/ha/devices) re-runs
            // the baseline and (re-)subscribes the WS. `collectLatest`
            // cancels the in-flight WS subscription when a new value
            // arrives, so we never have two open WS collectors.
            monitoredRepo.monitoredIds.collectLatest { monitored ->
                if (_state.value.phase !in setOf(
                        HaPhase.NotConfigured, HaPhase.Unauthorized, HaPhase.Error,
                    )
                ) {
                    // Don't flash a Loading state for an Error/Unauthorized
                    // session — the user is busy fixing the URL/token and
                    // would be confused by a "loading" overlay.
                }
                val baseline = runCatching { haClient.getStates() }
                baseline.fold(
                    onSuccess = { states ->
                        handleBaseline(states, monitored, warning, danger)
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
                        return@collectLatest
                    },
                )

                if (monitored.isNotEmpty()) {
                    haClient.subscribeStateChanges(monitored.toList())
                        .onEach { event -> handleWsEvent(event, monitored, warning, danger) }
                        .collect()
                }
            }
        }
    }

    private fun handleBaseline(
        states: List<HaState>,
        monitored: Set<String>,
        warning: Int,
        danger: Int,
    ) {
        val cards = states
            .filter { it.entityId in monitored }
            .mapNotNull { it.toMonitoredCardOrNull(warning, danger) }
            .sortedWith(severityThenId())
        val anyBattery = states.any { it.batteryPercent() != null }
        val phase = if (cards.isNotEmpty()) HaPhase.Loaded else HaPhase.Empty
        _state.value = HaDevicesState(
            phase = phase,
            devices = cards,
            hasAnyBatteryEntity = anyBattery,
        )
    }

    private fun handleWsEvent(
        event: HaEvent,
        monitored: Set<String>,
        warning: Int,
        danger: Int,
    ) {
        when (event) {
            is HaEvent.HaStateChange -> {
                if (event.entityId !in monitored) return  // user later unpicked it
                val current = _state.value
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
                        hasAnyBatteryEntity = current.hasAnyBatteryEntity,
                    )
                }
            }
        }
    }

    private fun HaState.toMonitoredCardOrNull(warning: Int, danger: Int): HaDeviceCard? {
        val percent = batteryPercent() ?: return null
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
                    monitoredRepo = app.haMonitoredDevicesRepository,
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
    /** HA reachable, at least one user-picked monitored entity. */
    Loaded,
    /** HA reachable, but no cards are user-monitored.
     *  Either HA has no battery entities OR the user unchecked everything. */
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
    /** True iff HA returned ≥1 entity with a battery attribute. Used by the
     *  home panel's Empty state to decide between "pick devices" (user
     *  unchecked all) and "no devices in HA" (HA itself has none). */
    val hasAnyBatteryEntity: Boolean = false,
)