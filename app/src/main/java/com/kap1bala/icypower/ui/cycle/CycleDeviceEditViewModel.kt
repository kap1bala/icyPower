package com.kap1bala.icypower.ui.cycle

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import com.kap1bala.icypower.IcyPowerApp
import com.kap1bala.icypower.data.cycle.CycleDevice
import com.kap1bala.icypower.data.cycle.CycleDeviceRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.UUID

/**
 * State for the new/edit form.
 *
 * `cycleDaysText` and `lastChargedAtMillis` are strings/longs so the UI can
 * bind directly to text fields. Validation translates them to Int/Long at
 * save time — see [save].
 *
 * `isEditing` is true iff the user came in with an existing id; controls
 * whether the "删除" button is visible.
 */
data class CycleDeviceEditState(
    val isLoading: Boolean = true,
    val isEditing: Boolean = false,
    val id: String? = null,
    val createdAt: Long = 0L,
    val name: String = "",
    val category: String = "",
    val cycleDaysText: String = "30",
    val lastChargedAtMillis: Long = System.currentTimeMillis(),
    val note: String = "",
    val nameError: String? = null,
    val cycleDaysError: String? = null,
    val isSaving: Boolean = false,
    val saveDone: Boolean = false,
    val isDeleted: Boolean = false,
    val confirmDelete: Boolean = false,
)

class CycleDeviceEditViewModel(
    private val repo: CycleDeviceRepository,
    /** null when creating; existing device id when editing. */
    private val deviceId: String?,
) : ViewModel() {

    private val _state = MutableStateFlow(CycleDeviceEditState())
    val state: StateFlow<CycleDeviceEditState> = _state.asStateFlow()

    init {
        if (deviceId == null) {
            // New device — use defaults.
            _state.value = CycleDeviceEditState(isLoading = false, isEditing = false)
        } else {
            // Edit existing.
            viewModelScope.launch {
                val existing = repo.findById(deviceId)
                if (existing == null) {
                    // Device was deleted while user was navigating — bail out.
                    _state.update { it.copy(isLoading = false, saveDone = true) }
                } else {
                    _state.value = CycleDeviceEditState(
                        isLoading = false,
                        isEditing = true,
                        id = existing.id,
                        createdAt = existing.createdAt,
                        name = existing.name,
                        category = existing.category.orEmpty(),
                        cycleDaysText = existing.cycleDays.toString(),
                        lastChargedAtMillis = existing.lastChargedAt,
                        note = existing.note.orEmpty(),
                    )
                }
            }
        }
    }

    fun onNameChange(value: String) = _state.update {
        it.copy(name = value, nameError = null)
    }

    fun onCategoryChange(value: String) = _state.update {
        it.copy(category = value)
    }

    fun onCycleDaysChange(value: String) = _state.update {
        // Only keep digits; clamp visually at 4 chars to be safe.
        val sanitized = value.filter(Char::isDigit).take(4)
        it.copy(cycleDaysText = sanitized, cycleDaysError = null)
    }

    fun onLastChargedAtChange(millis: Long) = _state.update {
        it.copy(lastChargedAtMillis = millis)
    }

    fun onNoteChange(value: String) = _state.update {
        it.copy(note = value)
    }

    fun requestDelete() = _state.update { it.copy(confirmDelete = true) }
    fun cancelDelete() = _state.update { it.copy(confirmDelete = false) }

    fun delete() {
        val id = state.value.id ?: return
        viewModelScope.launch {
            repo.remove(id)
            _state.update { it.copy(confirmDelete = false, isDeleted = true) }
        }
    }

    fun save() {
        val current = state.value
        val name = current.name.trim()
        val cycleDays = current.cycleDaysText.toIntOrNull()

        val nameError = if (name.isEmpty()) "请填写设备名" else null
        val cycleDaysError = when {
            cycleDays == null -> "请填写数字"
            cycleDays <= 0 -> "周期必须大于 0"
            cycleDays > 3650 -> "周期过大（>10 年）"
            else -> null
        }
        if (nameError != null || cycleDaysError != null) {
            _state.update { it.copy(nameError = nameError, cycleDaysError = cycleDaysError) }
            return
        }

        _state.update { it.copy(isSaving = true) }
        viewModelScope.launch {
            val now = System.currentTimeMillis()
            val device = CycleDevice(
                id = current.id ?: UUID.randomUUID().toString(),
                name = name,
                category = current.category.trim().ifEmpty { null },
                cycleDays = cycleDays!!,
                lastChargedAt = current.lastChargedAtMillis,
                note = current.note.trim().ifEmpty { null },
                createdAt = if (current.isEditing) current.createdAt else now,
                updatedAt = now,
            )
            repo.upsert(device)
            _state.update { it.copy(isSaving = false, saveDone = true) }
        }
    }

    companion object {
        /**
         * Factory function that captures the optional device id from the
         * navigation argument. Called by [IcyPowerNavHost] when constructing
         * the edit screen.
         */
        fun factory(deviceId: String?): ViewModelProvider.Factory = viewModelFactory {
            initializer {
                val app = this[ViewModelProvider.AndroidViewModelFactory.APPLICATION_KEY] as IcyPowerApp
                CycleDeviceEditViewModel(app.cycleDeviceRepository, deviceId)
            }
        }

        // Used by the UI for the date picker display.
        fun formatDate(millis: Long): String =
            SimpleDateFormat("yyyy-MM-dd HH:mm", Locale.getDefault()).format(Date(millis))
    }
}
