package com.kap1bala.icypower.ui.ha

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.kap1bala.icypower.IcyPowerApp
import com.kap1bala.icypower.data.cycle.OverdueSeverity
import com.kap1bala.icypower.data.ha.HaClient
import com.kap1bala.icypower.data.ha.HaState
import com.kap1bala.icypower.data.ha.NoOpHaClient
import com.kap1bala.icypower.data.preferences.HaMonitorPreferences
import com.kap1bala.icypower.data.preferences.HaMonitoredDevicesRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Drives `/settings/ha/devices` — the per-entity toggle list.
 *
 * On first ever visit (no persisted selection yet), every HA entity with
 * a battery attribute is auto-enabled so the user lands on a populated
 * state. After that, the persisted set is the source of truth and the
 * WS event stream keeps `batteryPercent` / `severity` fresh on each row.
 *
 * `state.phase` reuses [HaPhase] so the same NotConfigured / Loading /
 * Empty / Error / Unauthorized story the home panel tells also applies
 * here.
 */
class HaDeviceSelectionViewModel(
    private val haClient: HaClient,
    private val repo: HaMonitoredDevicesRepository,
    private val monitorPrefs: HaMonitorPreferences,
) : ViewModel() {

    private val _state = MutableStateFlow(HaSelectionState())
    val state: StateFlow<HaSelectionState> = _state.asStateFlow()

    init {
        bootstrap()
    }

    fun toggle(entityId: String, enabled: Boolean) {
        viewModelScope.launch {
            repo.toggle(entityId, enabled)
            // Mirror to local state immediately so the switch flips
            // before the DataStore write fully settles.
            _state.update { s ->
                s.copy(
                    options = s.options.map { o ->
                        if (o.entityId == entityId) o.copy(isMonitored = enabled) else o
                    },
                )
            }
        }
    }

    fun selectAll() {
        viewModelScope.launch {
            val ids = _state.value.options
                .filter { it.batteryPercent != null }
                .map { it.entityId }
                .toSet()
            repo.replace(ids)
            _state.update { s -> s.copy(options = s.options.map { it.copy(isMonitored = true) }) }
        }
    }

    fun clearAll() {
        viewModelScope.launch {
            repo.replace(emptySet())
            _state.update { s -> s.copy(options = s.options.map { it.copy(isMonitored = false) }) }
        }
    }

    fun refresh() {
        _state.value = HaSelectionState()
        bootstrap()
    }

    // ────────────────────────────────────────────────────────────────────
    // Internals
    // ────────────────────────────────────────────────────────────────────

    private fun bootstrap() {
        viewModelScope.launch {
            if (haClient is NoOpHaClient) {
                _state.value = HaSelectionState(phase = HaPhase.NotConfigured)
                return@launch
            }
            _state.value = HaSelectionState(phase = HaPhase.Loading)

            val (warning, danger) = computeThresholdsOnce()
            val monitored = repo.snapshot()

            val statesResult = runCatching { haClient.getStates() }
            statesResult.fold(
                onSuccess = { states ->
                    handleBaseline(states, monitored, warning, danger)
                },
                onFailure = { e ->
                    when (e) {
                        is com.kap1bala.icypower.data.ha.HaAuthException ->
                            _state.value = HaSelectionState(phase = HaPhase.Unauthorized)
                        else ->
                            _state.value = HaSelectionState(
                                phase = HaPhase.Error,
                                errorMessage = e.message ?: e::class.simpleName,
                            )
                    }
                },
            )
        }
    }

    private suspend fun handleBaseline(
        states: List<HaState>,
        monitored: Set<String>,
        warning: Int,
        danger: Int,
    ) {
        val batteryStates = states.filter { it.batteryPercent() != null }
        val monitoredIds = monitored.ifEmpty {
            // First-run: pre-fill with every battery entity so the user
            // lands on a populated state. Mark initialised to keep the
            // auto-fill from re-triggering on subsequent visits.
            if (repo.snapshot().isEmpty()) {
                batteryStates.map { it.entityId }.toSet().also {
                    repo.replace(it)
                    repo.markInitialized()
                }
            } else emptySet()
        }

        val options = batteryStates.map { state ->
            val percent = state.batteryPercent()!!
            val name = state.attributes["friendly_name"]?.toString()?.takeIf { it.isNotBlank() }
                ?: state.entityId
            val area = state.attributes["area"]?.toString()?.takeIf { it.isNotBlank() }
            HaDeviceOption(
                entityId = state.entityId,
                name = name,
                area = area,
                batteryPercent = percent,
                severity = severityFor(percent, warning, danger),
                isMonitored = state.entityId in monitoredIds,
            )
        }.sortedBy { it.entityId }

        _state.value = HaSelectionState(
            phase = if (options.isEmpty()) HaPhase.Empty else HaPhase.Loaded,
            options = options,
        )
    }

    private fun severityFor(percent: Int, warning: Int, danger: Int): OverdueSeverity =
        when {
            percent < danger -> OverdueSeverity.Danger
            percent < warning -> OverdueSeverity.Warning
            else -> OverdueSeverity.None
        }

    /** Snapshot the threshold pair without holding the flow reference. */
    private suspend fun computeThresholdsOnce(): Pair<Int, Int> {
        val w = monitorPrefs.warningThreshold.first()
        val d = monitorPrefs.dangerThreshold.first()
        return w to d
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val app = this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as IcyPowerApp
                HaDeviceSelectionViewModel(
                    haClient = app.haClient,
                    repo = app.haMonitoredDevicesRepository,
                    monitorPrefs = app.haMonitorPreferences,
                )
            }
        }
    }
}

// ────────────────────────────────────────────────────────────────────
// Public types
// ────────────────────────────────────────────────────────────────────

data class HaDeviceOption(
    val entityId: String,
    val name: String,
    val area: String?,
    val batteryPercent: Int,
    val severity: OverdueSeverity,
    val isMonitored: Boolean,
)

data class HaSelectionState(
    val phase: HaPhase = HaPhase.Loading,
    val options: List<HaDeviceOption> = emptyList(),
    val errorMessage: String? = null,
)