package io.github.kgbis.remotecontrol.app.features.devices

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import io.github.kgbis.remotecontrol.app.RemotePcControlApp
import io.github.kgbis.remotecontrol.app.core.AppVisibilityEvent
import io.github.kgbis.remotecontrol.app.core.model.Device
import io.github.kgbis.remotecontrol.app.core.model.DeviceState
import io.github.kgbis.remotecontrol.app.core.model.DeviceStatus
import io.github.kgbis.remotecontrol.app.core.model.PendingAction
import io.github.kgbis.remotecontrol.app.core.network.NetworkActions
import io.github.kgbis.remotecontrol.app.core.network.NetworkRangeDetector
import io.github.kgbis.remotecontrol.app.core.network.computeDeviceStatus
import io.github.kgbis.remotecontrol.app.core.network.probeDeviceFlow
import io.github.kgbis.remotecontrol.app.core.repository.DeviceRepository
import io.github.kgbis.remotecontrol.app.features.domain.DeviceMatcher
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.UUID
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

class DevicesViewModel(
    application: Application
) : AndroidViewModel(application) {

    private val appLifecycleObserver = (application as RemotePcControlApp).appLifecycleObserver

    private val settingsRepo = (application as RemotePcControlApp).settingsRepository

    val autoRefreshInterval =
        settingsRepo.autoRefreshIntervalFlow.stateIn(viewModelScope, SharingStarted.Eagerly, 30)

    private val repository = DeviceRepository(application)
    private val networkRangeDetector = NetworkRangeDetector()

    private val _devices = MutableStateFlow<List<Device>>(emptyList())
    val devices = _devices.asStateFlow()

    private val _deviceStatusMap = MutableStateFlow<Map<UUID, DeviceStatus>>(emptyMap())
    val deviceStatusMap = _deviceStatusMap.asStateFlow()

    private val _notInSameNetwork = MutableStateFlow<Boolean>(false)
    val notInSameNetwork = _notInSameNetwork.asStateFlow()

    private val _mainScreenVisible = MutableStateFlow(false)
    val mainScreenVisible = _mainScreenVisible.asStateFlow()

    fun setMainScreenVisible(visible: Boolean) {
        _mainScreenVisible.value = visible
    }


    init {
        Log.d("DevicesViewModel", "Init")
        loadInitialDataAndRefresh()
        observeAutoRefresh()

        appLifecycleObserver.visibilityEvents
            .onEach { event ->
                when (event) {
                    AppVisibilityEvent.Foreground -> onAppForegrounded()
                    AppVisibilityEvent.Background -> onAppBackgrounded()
                }
            }
            .launchIn(viewModelScope)
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    private fun observeAutoRefresh() {
        Log.d("observeAutoRefresh", "enter")
        combine(
            settingsRepo.autoRefreshEnabledFlow,
            settingsRepo.autoRefreshIntervalFlow,
            mainScreenVisible,
            appLifecycleObserver.isForegroundFlow
        ) { enabled, interval, screenVisible, appForeground ->
            Log.d(
                "observeAutoRefresh",
                "Autorefresh=$enabled, Main Screen visible=$screenVisible, App in foreground=$appForeground"
            )
            (enabled && screenVisible && appForeground) to interval
        }.flatMapLatest { (shouldRun, interval) ->
            if (!shouldRun) {
                Log.d("observeAutoRefresh", "It should not run")
                emptyFlow()
            } else {
                Log.d("observeAutoRefresh", "It should run. Interval is $interval seconds")
                tickerFlow(interval.seconds)
            }
        }.onEach {
            Log.d("observeAutoRefresh", "Running 'probeDevices()'")
            probeDevices()
        }.launchIn(viewModelScope)
    }

    fun tickerFlow(period: Duration) = flow {
        while (true) {
            emit(Unit)
            delay(period)
        }
    }


    private fun loadInitialDataAndRefresh() {
        Log.d("loadInitialDataAndRefresh", "Load devices from repository")
        getDevices()
        viewModelScope.launch {
            Log.d("loadInitialDataAndRefresh", "Load statuses from repository")
            onAppForegrounded()
            Log.d("loadInitialDataAndRefresh", "Remove orphan statuses")
            removeOrphanStatuses()
        }
    }

    private fun removeOrphanStatuses() {
        val validIds = _devices.value.map { it.id }.toSet()
        _deviceStatusMap.value =
            _deviceStatusMap.value
                .filterKeys { it in validIds }
                .toMutableMap()
    }


    private fun onAppBackgrounded() {
        Log.d("onAppBackgrounded", "Saving device statuses")
        repository.saveDeviceStatuses(_deviceStatusMap.value)
    }

    private suspend fun onAppForegrounded() {
        Log.d("onAppBackgrounded", "Getting device statuses")
        _deviceStatusMap.value = repository.loadDeviceStatuses()
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
        Log.d("addDiscoveredDevice", "Adding discovered device: $discoveredDevice")
        val found = DeviceMatcher(stored = _devices.value).findDeviceToAdd(discoveredDevice)
        if (found != null) {
            updateDevice(found, discoveredDevice)
        } else
            viewModelScope.launch {
                repository.addDevice(discoveredDevice)
                getDevices()
            }
    }

    fun addDevices(devices: List<Device>) {
        Log.d("addDevices", "Adding discovered devices: $devices")
        viewModelScope.launch {
            val toUpdate = mutableListOf<Pair<Device, Device>>()
            val toSave = mutableListOf<Device>()

            for (device in devices) {
                val storedDevice = DeviceMatcher(stored = _devices.value).findDeviceToAdd(device)
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

        // No any device in subnet
        val matchingIfaces =
            _devices.value.any { device -> device.interfaces.any { it.ip?.startsWith(subnet) == true } }

        Log.d("probeDevices", "entering")
        viewModelScope.launch {
            if (!matchingIfaces) {
                _notInSameNetwork.value = true
                val map = _deviceStatusMap.value.mapValues {
                    it.value.copy(
                        state = DeviceState.UNKNOWN,
                        trayReachable = false,
                        pendingAction = PendingAction.None
                        // lastSeen → DOES NOT change
                    )
                }

                _deviceStatusMap.emit(map)
                return@launch
            }

            _notInSameNetwork.value = false

            _devices.value.forEach { device ->
                val deviceId = device.id ?: return@forEach

                val shouldLaunch = probeMutex.withLock {
                    if (inFlightProbes.containsKey(deviceId)) {
                        Log.d(
                            "probeDevices",
                            "Probe in progress for ${device.hostname} ($deviceId)"
                        )
                        false
                    } else {
                        Log.d("probeDevices", "No probes for ${device.hostname} ($deviceId)")
                        true
                    }
                }

                if (!shouldLaunch) {
                    Log.d("probeDevices", "SHOuLDN'T launch for ${device.hostname} ($deviceId)")
                    probeMutex.withLock {
                        val job = inFlightProbes[deviceId]
                        if (job?.isActive == true) {
                            job.cancel()
                        }
                        inFlightProbes.remove(deviceId)
                    }
                }

                val job = launch {
                    try {
                        Log.d("probeDevices", "job. launch {}")
                        probeDeviceFlow(device, subnet).collect { probe ->
                            val prevMap = _deviceStatusMap.value
                            val previous = prevMap[deviceId]
                                ?: DeviceStatus(device = device, trayReachable = false)

                            val updated = computeDeviceStatus(
                                previous = previous,
                                probeResult = probe,
                                refreshInterval = autoRefreshInterval.value
                            )

                            if (probe.device != null) {
                                Log.d(
                                    "probeDevices",
                                    "Collected probe device info -> ${probe.device}"
                                )
                                addDevices(listOf(probe.device))
                            }

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

    fun sendCancelShutdownCommand(device: Device, onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            val success = NetworkActions.sendMessage(
                device = device,
                command = "CANCEL_SHUTDOWN",
                NetworkActions::simpleAckResponse
            )

            if (success == true) {
                val status =
                    _deviceStatusMap.value[device.id]?.copy(pendingAction = PendingAction.None)
                val previous = _deviceStatusMap.value
                _deviceStatusMap.emit(previous + (device.id!! to status!!))
            }

            onResult(success == true)
        }
    }


    fun sendShutdownCommand(device: Device, delay: Int, unit: String, onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            val scheduledAt = Instant.now()
            val executeAt = scheduledAt.plus(delay.toLong(), ChronoUnit.valueOf(unit))
            val cancellable = delay != 0

            val success = NetworkActions.sendMessage(
                device = device,
                command = "SHUTDOWN $delay $unit",
                NetworkActions::simpleAckResponse
            )
            if (success == true) {
                val pendingAction = PendingAction.ShutdownScheduled(
                    scheduledAt = scheduledAt,
                    executeAt = executeAt,
                    cancellable = cancellable
                )
                val previous = _deviceStatusMap.value

                Log.w("", "Is cancellable shutdown? $cancellable")

                val state = when (cancellable) {
                    true -> {
                        Log.w("PREVIOUS_STATE", "${previous[device.id]?.state}")
                        previous[device.id]?.state ?: DeviceState.UNKNOWN
                    }

                    false -> {
                        Log.w("OFFLINE", "OFFLINE")
                        DeviceState.OFFLINE
                    }
                }
                val status = _deviceStatusMap.value[device.id]?.copy(
                    pendingAction = pendingAction,
                    state = state
                )

                Log.d("sendShutdownCommand", "Status after shutdown command -> $status")

                _deviceStatusMap.emit(previous + (device.id!! to status!!))
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