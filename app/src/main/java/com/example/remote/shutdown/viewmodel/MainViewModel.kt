package com.example.remote.shutdown.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.remote.shutdown.data.*
import com.example.remote.shutdown.network.NetworkUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit

class MainViewModel(application: Application) : AndroidViewModel(application) {

    // Device Repository and actions
    private val repository = DeviceRepository(application)

    private val _devices = MutableStateFlow<List<Device>>(emptyList())
    val devices = _devices.asStateFlow()

    private val _statusMap = MutableStateFlow<Map<String, Boolean>>(emptyMap())
    val statusMap = _statusMap.asStateFlow()

    val routerIps: List<String> by lazy {
        loadRouterIps()
    }

    private fun loadRouterIps(): List<String> {
        return try {
            val context = getApplication<Application>()
            context.assets.open("router_ips.txt")
                .bufferedReader()
                .readLines()
                .filter { it.isNotBlank() }
        } catch (e: Exception) {
            e.printStackTrace()
            emptyList()
        }
    }

    init {
        loadDevices()
    }

    fun loadDevices() {
        viewModelScope.launch {
            _devices.value = repository.getDevices()
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

    fun refreshStatuses() {
        viewModelScope.launch(Dispatchers.IO) {
            val current = _devices.value
            val newStatuses = current.associate { device ->
                device.ip to NetworkUtils.isPcOnline(device.ip)
            }
            _statusMap.emit(newStatuses)
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

    // Shutdown settings

    private val settingsRepo = SettingsRepository(application)

    val shutdownDelay = settingsRepo.shutdownDelayFlow.stateIn(viewModelScope,
        SharingStarted.WhileSubscribed(5_000), 15)

    val shutdownUnit = settingsRepo.shutdownUnitFlow.stateIn(viewModelScope,
        SharingStarted.WhileSubscribed(5_000), TimeUnit.SECONDS)

    fun changeDelay(newDelay: Int) {
        viewModelScope.launch {
            settingsRepo.saveShutdownDelay(newDelay)
        }
    }

    fun changeUnit(newUnit: TimeUnit) {
        viewModelScope.launch {
            settingsRepo.saveShutdownUnit(newUnit)
        }
    }
}
