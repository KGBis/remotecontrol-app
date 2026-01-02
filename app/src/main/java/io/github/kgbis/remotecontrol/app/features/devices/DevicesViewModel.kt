package io.github.kgbis.remotecontrol.app.features.devices

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import io.github.kgbis.remotecontrol.app.core.model.Device
import io.github.kgbis.remotecontrol.app.core.model.DeviceState
import io.github.kgbis.remotecontrol.app.core.model.DeviceStatus
import io.github.kgbis.remotecontrol.app.core.network.NetworkActions
import io.github.kgbis.remotecontrol.app.core.network.NetworkRangeDetector
import io.github.kgbis.remotecontrol.app.core.network.computeDeviceStatus
import io.github.kgbis.remotecontrol.app.core.network.probeDeviceFlow
import io.github.kgbis.remotecontrol.app.core.repository.DeviceRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.cancel
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.util.UUID

class DevicesViewModel(
    application: Application
) : AndroidViewModel(application) {

    private val repository = DeviceRepository(application)
    private val networkRangeDetector = NetworkRangeDetector()

    private val _devices = MutableStateFlow<List<Device>>(emptyList())
    val devices = _devices.asStateFlow()

    private val _deviceStatusMap = MutableStateFlow<Map<UUID, DeviceStatus>>(emptyMap())
    val deviceStatusMap = _deviceStatusMap.asStateFlow()

    init {
        getDevices()
    }

    private var autoProbeJob: Job? = null

    fun startAutoProbe(interval: Int) {
        Log.d("startAutoProbe", "is active? ${autoProbeJob?.isActive} ")
        if (autoProbeJob?.isActive == true) return

        Log.d("startAutoProbe", "Starting device probing")
        autoProbeJob = viewModelScope.launch {
            autoProbeLoop(interval)
        }
    }

    fun stopAutoProbe() {
        autoProbeJob?.cancel()
        autoProbeJob = null
    }

    private suspend fun autoProbeLoop(interval: Int) {
        while (currentCoroutineContext().isActive) {
            val devices = _devices.value
            if (devices.isNotEmpty()) {
                Log.d("autoProbeLoop", "Device list NOT EMPTY. Calling 'probeDevices()'")
                probeDevices()
            }

            delay(interval * 1000L) // millis here
        }
    }


    @Suppress("SENSELESS_COMPARISON")
    fun getDevices() {
        viewModelScope.launch {
            _devices.value = repository.getDevices()
            val toRemove = _devices.value.filter { it.interfaces == null }
            if (toRemove.isNotEmpty()) {
                toRemove.forEach { repository.removeDevice(it) }
                _devices.value = repository.getDevices()
            }
        }
    }

    fun findDeviceToAdd(inDevice: Device): Device? {
        val all = _devices.value

        // incoming device if IDs match
        var found = getDeviceById(inDevice.id)
        if (found != null) {
            Log.d("findDeviceToAdd", "found by Id ${inDevice.id}")
            return found
        }

        // if IDs don't match let's see other fields
        val macs: List<String?> = inDevice.interfaces.map { it.mac }

        found = all.firstOrNull { device ->
            @Suppress("SENSELESS_COMPARISON")
            if (device.interfaces == null)
                return null


            // if any of the MACs match, it's the same
            val storedMacs = device.interfaces.mapNotNull {
                Log.d("FD", "interface stored -> $it")
                it.mac
            }

            for (sMac in storedMacs) {
                if (macs.contains(sMac)) {
                    Log.d("findDeviceToAdd", "found by MAC $sMac")
                    return device
                }
            }

            // as last resort, check if hostname is the same. They usually do not change
            if (device.hostname.equals(inDevice.hostname, true)) {
                Log.d("findDeviceToAdd", "found by hostname '${device.hostname}'")
                return device
            }

            // nothing, then false
            false
        }

        return found
    }

    fun getDeviceById(id: UUID?): Device? {
        return _devices.value.firstOrNull { it.id == id }
    }

    fun addDevice(device: Device) {
        device.normalize()
        viewModelScope.launch {
            inFlightProbes.remove(device.id)?.cancel()
            repository.addDevice(device)
            getDevices()
        }
    }

    fun addDiscoveredDevice(discoveredDevice: Device) {
        val found = findDeviceToAdd(discoveredDevice)
        if (found != null) {
            updateDevice(found, discoveredDevice)
        } else
            viewModelScope.launch {
                repository.addDevice(discoveredDevice)
                getDevices()
            }
    }

    fun addDevices(devices: List<Device>) {
        viewModelScope.launch {
            val toUpdate = mutableListOf<Pair<Device, Device>>()
            val toSave = mutableListOf<Device>()

            for (device in devices) {
                val storedDevice = findDeviceToAdd(device)
                // Not found -> new
                if (storedDevice == null) {
                    toSave.add(device)
                } else {
                    toUpdate.add(storedDevice to device)
                }
            }

            // save new ones
            toSave.forEach { it.normalize() }
            repository.addDevices(toSave)

            // update the existing ones
            toUpdate.forEach {
                val normalized = it.second
                normalized.normalize()
                repository.updateDevice(it.first, normalized)
            }

            getDevices()
        }
    }


    fun updateDevice(original: Device, updated: Device) {
        updated.normalize()
        viewModelScope.launch {
            inFlightProbes.remove(original.id)?.cancel()
            repository.updateDevice(original, updated)
            getDevices()
        }
    }

    fun removeDevice(device: Device) {
        viewModelScope.launch {
            inFlightProbes.remove(device.id)?.cancel()
            _deviceStatusMap.emit(_deviceStatusMap.value.filter { it.key != device.id })
            repository.removeDevice(device)
            getDevices()
        }
    }


    private val inFlightProbes = mutableMapOf<UUID, Job>()
    private val probeMutex = Mutex()

    fun probeDevices() {
        val subnet = networkRangeDetector.getScanSubnet()

        viewModelScope.launch {
            _devices.value.forEach { device ->
                val deviceId = device.id ?: return@forEach

                val shouldLaunch = probeMutex.withLock {
                    if (inFlightProbes.containsKey(deviceId)) {
                        Log.d(
                            "probeDevices",
                            "CANCELLING PROBE. Probe in progress for ${device.hostname} ($deviceId)"
                        )
                        false
                    } else {
                        Log.d(
                            "probeDevices",
                            "STARTING PROBE for ${device.hostname} ($deviceId)"
                        )
                        true
                    }
                }

                if (!shouldLaunch) {
                    probeMutex.withLock {
                        val job = inFlightProbes[deviceId]
                        if (job?.isActive == true) {
                            job.cancel()
                        }
                        inFlightProbes.remove(deviceId)
                    }
                    // return@forEach
                }

                val job = launch {
                    try {
                        probeDeviceFlow(device, subnet).collect { probe ->
                            val prevMap = _deviceStatusMap.value
                            val previous = prevMap[deviceId]
                                ?: DeviceStatus(device = device, trayReachable = false)

                            val updated = computeDeviceStatus(
                                previous = previous,
                                probeResult = probe
                            )

                            _deviceStatusMap.emit(
                                prevMap + (deviceId to updated)
                            )
                        }
                    } finally {
                        probeMutex.withLock {
                            Log.d("probeMutex.withLock", "Removing $deviceId from inFlightProbes")
                            inFlightProbes.remove(deviceId)
                        }
                    }
                }

                probeMutex.withLock {
                    Log.d("probeMutex.withLock", "Adding $deviceId for $job to inFlightProbes")
                    inFlightProbes[deviceId] = job
                }
            }
        }
    }


    fun sendShutdownCommand(device: Device, delay: Int, unit: String, onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            val success = NetworkActions.sendMessage(
                device = device,
                command = "SHUTDOWN $delay $unit",
                NetworkActions::shutdownResponse
            )
            if (success == true) {
                val s = _deviceStatusMap.value[device.id]?.copy(state = DeviceState.UNKNOWN)
                val previous = _deviceStatusMap.value
                _deviceStatusMap.emit(previous + (device.id!! to s!!))
            }

            onResult(success == true)
        }
    }

    fun wakeOnLan(device: Device, onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            onResult(NetworkActions.sendWoL(device))
        }
    }
}