package com.example.remote.shutdown.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.remote.shutdown.data.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class MainViewModel(application: Application) : AndroidViewModel(application) {

    private val repository = DeviceRepository(application)

    val devices: MutableStateFlow<List<Device>> = MutableStateFlow(emptyList())

    init {
        loadDevices()
    }

    fun loadDevices() {
        viewModelScope.launch {
            devices.value = repository.getDevices()
        }
    }

    fun addDevice(device: Device) {
        viewModelScope.launch {
            repository.addDevice(device)
            loadDevices()
        }
    }

    fun removeDevice(device: Device) {
        viewModelScope.launch {
            repository.removeDevice(device)
            loadDevices()
        }
    }

    fun sendShutdownCommand(device: Device, delay: Int, unit: String, onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            val success = repository.sendShutdown(device, delay, unit)
            onResult(success)
        }
    }

    fun wakeOnLan(device: Device, mac: String, onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            val success = repository.sendWoL(device, mac)
            onResult(success)
        }
    }
}
