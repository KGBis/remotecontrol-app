package com.example.remote.shutdown.viewmodel

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.remote.shutdown.data.Device
import com.example.remote.shutdown.data.DeviceStatus
import com.example.remote.shutdown.network.NetworkActions
import com.example.remote.shutdown.network.NetworkScanner
import com.example.remote.shutdown.network.NetworkScanner.deviceStatus
import com.example.remote.shutdown.repository.DeviceRepository
import com.example.remote.shutdown.repository.SettingsRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.temporal.ChronoUnit

class MainViewModel(application: Application) : AndroidViewModel(application) {
    // Device Repository and actions
    private val repository = DeviceRepository(application)

    // Device list
    private val _devices = MutableStateFlow<List<Device>>(emptyList())
    val devices = _devices.asStateFlow()

    // Device status
    private val _deviceStatusMap = MutableStateFlow<Map<String, DeviceStatus>>(emptyMap())
    val deviceStatusMap = _deviceStatusMap.asStateFlow()

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

    // Class initializer. Load list of stored devices
    init {
        loadDevices()
    }

    fun loadDevices() {
        viewModelScope.launch {
            _devices.value = repository.getDevices()
        }
    }

    fun addDevice(device: Device) {
        device.normalize()
        viewModelScope.launch {
            repository.addDevice(device)
            loadDevices()
        }
    }

    fun updateDevice(original: Device, updated: Device) {
        updated.normalize()
        viewModelScope.launch {
            repository.updateDevice(original, updated)
            loadDevices()
        }
    }

    fun removeDevice(device: Device) {
        viewModelScope.launch {
            repository.removeDevice(device)
            loadDevices()
        }
    }

    fun getDeviceByIp(ip: String): Device? {
        return _devices.value.firstOrNull { it.ip == ip }
    }

    fun refreshStatuses() {
        viewModelScope.launch(Dispatchers.IO) {
            val current = _devices.value
            val newStatuses = current.associate { device ->
                device.ip to deviceStatus(device)
            }

            // Solo emitir si hay cambios
            val oldStatuses = _deviceStatusMap.value
            if (newStatuses != oldStatuses) {
                Log.i("refreshStatuses", "emitting new statuses -> $newStatuses")
                _deviceStatusMap.emit(newStatuses)
            }
        }
    }

    fun sendShutdownCommand(device: Device, delay: Int, unit: String, onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            val success = NetworkActions.sendMessage(
                device = device,
                command = "SHUTDOWN $delay $unit",
                NetworkScanner::shutdownRequest
            )
            onResult(success == true)
        }
    }

    fun wakeOnLan(device: Device, onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            val success = NetworkActions.sendWoL(device)
            if (success) {
                _deviceStatusMap.update { current ->
                    current.toMutableMap().apply {
                        this[device.ip] = this.getValue(device.ip).copy(isOnline = null)
                    }
                }
            }
            onResult(success)
        }
    }

    // Shutdown settings (quantity and time unit)

    private val settingsRepo = SettingsRepository(application)

    val shutdownDelay = settingsRepo.shutdownDelayFlow.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000), 15
    )

    val shutdownUnit = settingsRepo.shutdownUnitFlow.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000), ChronoUnit.SECONDS
    )

    fun changeDelay(newDelay: Int) {
        viewModelScope.launch {
            settingsRepo.saveShutdownDelay(newDelay)
        }
    }

    fun changeUnit(newUnit: ChronoUnit) {
        viewModelScope.launch {
            settingsRepo.saveShutdownUnit(newUnit)
        }
    }
}
