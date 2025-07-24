package com.example.remote.shutdown.viewmodel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.example.remote.shutdown.common.UserPreferences
import com.example.remote.shutdown.model.Device
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch

class MainViewModel(private val prefs: UserPreferences) : ViewModel() {

    // StateFlow para exponer el estado a la UI
    private val _switchState = MutableStateFlow(false)
    val switchState: StateFlow<Boolean> = _switchState.asStateFlow()

    private val _computerIp = MutableStateFlow("")
    val computerIp: StateFlow<String> = _computerIp.asStateFlow()

    init {
        // Escucha cambios desde DataStore
        viewModelScope.launch {
            prefs.switchFlow.collect { value ->
                _switchState.value = value
            }
            prefs.computerIp.collect { value ->
                _computerIp.value = value
            }
        }
    }

    fun onSwitchChanged(checked: Boolean) {
        viewModelScope.launch {
            prefs.saveSwitchState(checked)
        }
    }
}
