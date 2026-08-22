package com.kap1bala.icypower.ui.cycle

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.kap1bala.icypower.IcyPowerApp
import com.kap1bala.icypower.data.cycle.CycleDeviceRepository
import com.kap1bala.icypower.data.cycle.CycleDeviceState
import com.kap1bala.icypower.data.cycle.CycleOverview
import com.kap1bala.icypower.data.cycle.OverdueSeverity
import com.kap1bala.icypower.data.cycle.computeOverview
import com.kap1bala.icypower.data.cycle.epochDayOf
import kotlinx.coroutines.channels.BufferOverflow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch

class CycleDeviceListViewModel(
    private val repo: CycleDeviceRepository,
    private val clock: () -> Long = System::currentTimeMillis,
) : ViewModel() {

    val devices: StateFlow<List<CycleDeviceState>> = repo.devices
        .map { list ->
            val now = clock()
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

    val overview: StateFlow<CycleOverview> = devices
        .map { states -> computeOverview(states, epochDayOf(clock())) }
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5_000),
            initialValue = CycleOverview.EMPTY,
        )

    private val _allClearEvents = MutableSharedFlow<AllClearEvent>(
        replay = 0,
        extraBufferCapacity = 1,
        onBufferOverflow = BufferOverflow.DROP_OLDEST,
    )
    val allClearEvents: SharedFlow<AllClearEvent> = _allClearEvents.asSharedFlow()

    fun markCharged(id: String) {
        val wasOverdue = devices.value.any { it.severity != OverdueSeverity.None }
        viewModelScope.launch {
            repo.resetLastChargedAt(id)
            val isOverdueNow = devices.value.any { it.severity != OverdueSeverity.None }
            if (wasOverdue && !isOverdueNow) {
                _allClearEvents.tryEmit(AllClearEvent.AllDevicesCharged)
            }
        }
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

sealed interface AllClearEvent {
    data object AllDevicesCharged : AllClearEvent
}
