package com.range.phoneLinuxer.viewModel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.range.phoneLinuxer.data.model.VirtualMachineSettings
import com.range.phoneLinuxer.data.repository.impl.VmSettingsRepositoryImpl
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class EmulatorViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = VmSettingsRepositoryImpl(application.applicationContext)

    private val _uiEvent = Channel<UiEvent>()
    val uiEvent = _uiEvent.receiveAsFlow()

    sealed class UiEvent {
        object SaveSuccess : UiEvent()
        object DeleteSuccess : UiEvent()
        data class Error(val message: String) : UiEvent()
    }

    val vms: StateFlow<List<VirtualMachineSettings>> = repository.findAllVms()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())

    fun saveVm(settings: VirtualMachineSettings) {
        viewModelScope.launch {
            try {
                repository.saveVm(settings)
                _uiEvent.send(UiEvent.SaveSuccess)
            } catch (e: Exception) {
                _uiEvent.send(UiEvent.Error(e.message ?: "Save operation failed"))
            }
        }
    }

    fun deleteVm(vmId: String) {
        viewModelScope.launch {
            try {
                repository.deleteVm(vmId)
                _uiEvent.send(UiEvent.DeleteSuccess)
            } catch (e: Exception) {
                _uiEvent.send(UiEvent.Error(e.message ?: "Could not delete Virtual Machine"))
            }
        }
    }
}