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
import io.github.kgbis.remotecontrol.app.network.ScanManager
import io.github.kgbis.remotecontrol.app.repository.DeviceRepository
import io.github.kgbis.remotecontrol.app.repository.SettingsRepository
import io.github.kgbis.remotecontrol.app.util.Utils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.time.temporal.ChronoUnit

class MainViewModel(application: Application) : AndroidViewModel(application) {

    val networkRangeDetector = NetworkRangeDetector()

    private val repository = DeviceRepository(application)

    private val settingsRepo = SettingsRepository(application)

    /* Device Repository and actions */

    private val _devices = MutableStateFlow<List<Device>>(emptyList())
    val devices = _devices.asStateFlow()

    // Device status
    private val _deviceStatusMap = MutableStateFlow<Map<String, DeviceStatus>>(emptyMap())
    val deviceStatusMap = _deviceStatusMap.asStateFlow()

    val routerIps: List<String> by lazy {
        Utils.loadRouterIps(getApplication())
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

    fun addDevices(devices: List<Device>) {
        viewModelScope.launch {
            devices.forEach { it.normalize() }
            repository.addDevices(devices)
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

    /* Shutdown settings (quantity and time unit) */

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

    /* Auto-refresh settings */

    val autoRefreshEnabled = settingsRepo.autoRefreshEnabledFlow.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000), true
    )

    val autoRefreshInterval = settingsRepo.autoRefreshIntervalFlow.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000), 15
    )

    fun setAutoRefreshEnabled(value: Boolean) {
        viewModelScope.launch {
            settingsRepo.saveAutorefreshEnabled(value)
        }
    }

    fun setAutoRefreshInterval(value: Float) {
        viewModelScope.launch {
            settingsRepo.saveAutorefreshDelay(value)
        }
    }

    /* Network scan (socket) Timeout */
    val socketTimeout = settingsRepo.socketTimeoutFlow.stateIn(
        viewModelScope,
        SharingStarted.WhileSubscribed(5_000), 500
    )

    fun setSocketTimeout(value: Float) {
        viewModelScope.launch {
            settingsRepo.saveSocketTimeout(value)
        }
    }

    val aboutKeys: Map<String, String> by lazy {
        Utils.loadAboutKeys(getApplication())
    }

    /* Network scan manager stuff  */

    // ---- Scan Manager ----
    private val scanManager by lazy {
        ScanManager(
            networkRangeDetector = networkRangeDetector,
            timeout = socketTimeout.value,
            maxConcurrent = 30
        )
    }

    val scanProgress = scanManager.progress
    val scanResults = scanManager.results
    val scanState = scanManager.state

    fun startScan() = scanManager.startScan()
    fun cancelScan() = scanManager.cancelScan()

    // Class initializer. Load list of stored devices
    init {
        loadDevices()
    }
}
