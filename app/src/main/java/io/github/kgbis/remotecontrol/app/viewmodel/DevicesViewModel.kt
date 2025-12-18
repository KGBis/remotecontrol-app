package io.github.kgbis.remotecontrol.app.viewmodel

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import io.github.kgbis.remotecontrol.app.data.Device
import io.github.kgbis.remotecontrol.app.data.DeviceStatus
import io.github.kgbis.remotecontrol.app.network.NetworkActions
import io.github.kgbis.remotecontrol.app.network.NetworkRangeDetector
import io.github.kgbis.remotecontrol.app.network.NetworkScanner
import io.github.kgbis.remotecontrol.app.repository.DeviceRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlin.collections.set

class DevicesViewModel(
    application: Application
) : AndroidViewModel(application) {

    private val repository = DeviceRepository(application)
    private val networkRangeDetector = NetworkRangeDetector()

    private val _devices = MutableStateFlow<List<Device>>(emptyList())
    val devices = _devices.asStateFlow()

    private val _deviceStatusMap = MutableStateFlow<Map<String, DeviceStatus>>(emptyMap())
    val deviceStatusMap = _deviceStatusMap.asStateFlow()

    init {
        getDevices()
    }

    fun getDevices() {
        viewModelScope.launch {
            _devices.value = repository.getDevices()
        }
    }

    fun getDeviceByIp(ip: String): Device? {
        return _devices.value.firstOrNull { it.ip == ip }
    }

    fun addDevice(device: Device) {
        device.normalize()
        viewModelScope.launch {
            repository.addDevice(device)
            getDevices()
        }
    }

    fun addDevices(devices: List<Device>) {
        viewModelScope.launch {
            devices.forEach { it.normalize() }
            repository.addDevices(devices)
            getDevices()
        }
    }


    fun updateDevice(original: Device, updated: Device) {
        updated.normalize()
        viewModelScope.launch {
            repository.updateDevice(original, updated)
            getDevices()
        }
    }

    fun removeDevice(device: Device) {
        viewModelScope.launch {
            repository.removeDevice(device)
            getDevices()
        }
    }

    fun refreshStatuses() {
        val localSubnet = networkRangeDetector.getScanSubnet()
        Log.d("refreshStatuses", "Refreshing device list for subnet $localSubnet")
        viewModelScope.launch(Dispatchers.IO) {
            val current = _devices.value
            val newStatuses = current.associate { device ->
                device.ip to NetworkScanner.deviceStatus(device, localSubnet)
            }

            // emit only if changes occur
            val oldStatuses = _deviceStatusMap.value
            if (newStatuses != oldStatuses) {
                Log.d("refreshStatuses", "emitting new statuses -> $newStatuses")
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
}
