package com.range.phoneLinuxer.viewModel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.range.phoneLinuxer.data.enums.VmState
import com.range.phoneLinuxer.data.executor.EmulatorExecutor
import com.range.phoneLinuxer.data.model.VirtualMachineSettings
import com.range.phoneLinuxer.data.model.buildFullCommand
import com.range.phoneLinuxer.data.repository.impl.VmSettingsRepositoryImpl
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import timber.log.Timber

class EmulatorViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = VmSettingsRepositoryImpl(application.applicationContext)
    private val executor = EmulatorExecutor(application)

    private val _vmLogs = MutableStateFlow<Map<String, List<String>>>(emptyMap())
    val vmLogs: StateFlow<Map<String, List<String>>> = _vmLogs.asStateFlow()

    private val _editingVm = MutableStateFlow<VirtualMachineSettings?>(null)
    val editingVm: StateFlow<VirtualMachineSettings?> = _editingVm.asStateFlow()

    private val _uiEvent = Channel<UiEvent>()
    val uiEvent = _uiEvent.receiveAsFlow()

    sealed class UiEvent {
        object SaveSuccess : UiEvent()
        object DeleteSuccess : UiEvent()
        data class NavigateToEmulator(val vmId: String) : UiEvent()
        data class Error(val message: String) : UiEvent()
    }

    val vms: StateFlow<List<VirtualMachineSettings>> = repository.findAllVms()
        .stateIn(
            scope = viewModelScope,
            started = SharingStarted.WhileSubscribed(5000),
            initialValue = emptyList()
        )

    fun toggleVmState(settings: VirtualMachineSettings) {
        viewModelScope.launch {
            if (executor.isAlive(settings.id)) {
                stopVm(settings.id)
            } else {
                startVm(settings)
            }
        }
    }

    fun startVm(settings: VirtualMachineSettings) {
        viewModelScope.launch {
            try {
                updateVmState(settings.id, VmState.STARTING)

                clearLogs(settings.id)
                appendLog(settings.id, "--- Starting QEMU Engine ---")

                val fullCommand = settings.buildFullCommand()

                viewModelScope.launch {
                    executor.getLogStream(settings.id).collect { logLine ->
                        appendLog(settings.id, logLine)
                    }
                }

                val result = executor.executeCommand(
                    vmId = settings.id,
                    fullCommand = fullCommand
                )

                result.onSuccess { pid ->
                    updateVmState(settings.id, VmState.RUNNING)
                    Timber.i("VM ${settings.vmName} started. PID: $pid")

                    _uiEvent.send(UiEvent.NavigateToEmulator(settings.id))
                }.onFailure { e ->
                    updateVmState(settings.id, VmState.ERROR)
                    appendLog(settings.id, "LAUNCH ERROR: ${e.message}")
                    _uiEvent.send(UiEvent.Error("Launch failed: ${e.message}"))
                }

            } catch (e: Exception) {
                updateVmState(settings.id, VmState.ERROR)
                appendLog(settings.id, "SYSTEM ERROR: ${e.message}")
                _uiEvent.send(UiEvent.Error("System failure: ${e.message}"))
            }
        }
    }

    private fun appendLog(vmId: String, line: String) {
        _vmLogs.update { currentMap ->
            val currentList = currentMap[vmId] ?: emptyList()
            val newList = (currentList + line).takeLast(500)
            currentMap + (vmId to newList)
        }
    }

    private fun clearLogs(vmId: String) {
        _vmLogs.update { it + (vmId to emptyList()) }
    }

    fun stopVm(vmId: String) {
        viewModelScope.launch {
            try {
                updateVmState(vmId, VmState.STOPPING)
                appendLog(vmId, "--- Terminating VM ---")

                executor.killProcess(vmId)

                updateVmState(vmId, VmState.INACTIVE)
                Timber.i("VM $vmId stopped.")
            } catch (e: Exception) {
                _uiEvent.send(UiEvent.Error("Stop error: ${e.message}"))
            }
        }
    }

    private suspend fun updateVmState(vmId: String, newState: VmState) {
        vms.value.find { it.id == vmId }?.let { vm ->
            repository.saveVm(vm.copy(state = newState))
        }
    }

    fun saveVm(settings: VirtualMachineSettings) {
        viewModelScope.launch {
            try {
                repository.saveVm(settings)
                _uiEvent.send(UiEvent.SaveSuccess)
                _editingVm.value = null
            } catch (e: Exception) {
                _uiEvent.send(UiEvent.Error(e.message ?: "Save failed"))
            }
        }
    }

    fun deleteVm(vmId: String) {
        viewModelScope.launch {
            try {
                if (executor.isAlive(vmId)) {
                    executor.killProcess(vmId)
                }
                repository.deleteVm(vmId)
                _uiEvent.send(UiEvent.DeleteSuccess)
            } catch (e: Exception) {
                _uiEvent.send(UiEvent.Error(e.message ?: "Delete failed"))
            }
        }
    }

    override fun onCleared() {
        super.onCleared()
        executor.killAll()
    }

    fun setEditingVm(vm: VirtualMachineSettings?) {
        _editingVm.value = vm
    }

    fun loadVmForEditing(id: String) {
        viewModelScope.launch {
            val vm = vms.value.find { it.id == id }
            _editingVm.emit(vm)
        }
    }
}