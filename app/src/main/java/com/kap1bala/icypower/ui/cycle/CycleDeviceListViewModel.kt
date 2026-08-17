package com.kap1bala.icypower.ui.cycle

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.kap1bala.icypower.IcyPowerApp
import com.kap1bala.icypower.data.cycle.CycleDeviceRepository
import com.kap1bala.icypower.data.cycle.CycleDeviceState
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

/**
 * Owns the sorted list of [CycleDeviceState] for the list screen and the
 * home screen's "周期设备" tab.
 *
 * Sort rule (feat.md §2.1 Tab A):
 *   - Severest first (Danger → Warning → None)
 *   - Within the same severity, most overdue first (largest daysSinceLastCharge)
 */
class CycleDeviceListViewModel(
    private val repo: CycleDeviceRepository,
) : ViewModel() {

    val devices: StateFlow<List<CycleDeviceState>> = repo.devices
        .map { list ->
            val now = System.currentTimeMillis()
            list.map { CycleDeviceState.from(it, now) }
                .sortedWith(
                    compareByDescending<CycleDeviceState> { it.severity.ordinal }
                        .thenByDescending { it.daysSinceLastCharge },
                )
        }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = emptyList(),
        )

    /** Invoked by the home card's "已充电" button. */
    fun markCharged(id: String) {
        viewModelScope.launch { repo.resetLastChargedAt(id) }
    }

    companion object {
        val Factory: ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val app = this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as IcyPowerApp
                CycleDeviceListViewModel(app.cycleDeviceRepository)
            }
        }
    }
}
