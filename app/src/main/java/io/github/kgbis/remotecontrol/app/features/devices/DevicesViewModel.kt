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
import io.github.kgbis.remotecontrol.app.core.network.NetworkInfo
import io.github.kgbis.remotecontrol.app.core.network.NetworkMonitor
import io.github.kgbis.remotecontrol.app.core.network.NetworkRangeDetector
import io.github.kgbis.remotecontrol.app.core.network.ProbeResult
import io.github.kgbis.remotecontrol.app.core.network.computeDeviceStatus
import io.github.kgbis.remotecontrol.app.core.network.probeDeviceFlow
import io.github.kgbis.remotecontrol.app.features.domain.DeviceMatcher
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
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

    private val deviceRepository = (application as RemotePcControlApp).devicesRepository

    private val networkRangeDetector = NetworkRangeDetector()

    private val networkMonitor = NetworkMonitor(application, viewModelScope, networkRangeDetector)

    private val inFlightProbes = mutableMapOf<UUID, Job>()

    private val probeMutex = Mutex()

    private val _devices = MutableStateFlow<List<Device>>(emptyList())

    val devices = _devices.asStateFlow()

    val sameNetworkFlow = combine(
        networkMonitor.networkInfo,
        devices
    ) { networkInfo, devices ->
        val tag = "sameNetworkFlow"
        Log.d(
            tag, "networkInfo = ${networkInfo.javaClass.simpleName}, devices = ${devices.size}"
        )
        if (networkInfo !is NetworkInfo.Local) return@combine false
        hasDevicesInSubnet(networkInfo.subnet)
    }


    val autoRefreshInterval =
        settingsRepo.autoRefreshIntervalFlow.stateIn(viewModelScope, SharingStarted.Eagerly, 30)

    private val _deviceStatusMap = MutableStateFlow<Map<UUID, DeviceStatus>>(emptyMap())

    val deviceStatusMap = _deviceStatusMap.asStateFlow()

    private val _mainScreenVisible = MutableStateFlow(false)

    val mainScreenVisible = _mainScreenVisible.asStateFlow()

    val isInLocalNetwork: StateFlow<Boolean> = networkMonitor.networkInfo
        .map { it is NetworkInfo.Local }.distinctUntilChanged().stateIn(
            scope = viewModelScope,
            started = SharingStarted.Eagerly,
            initialValue = false
        )

    val networkState = networkMonitor.networkInfo

    fun setMainScreenVisible(visible: Boolean) {
        _mainScreenVisible.value = visible
    }

    @OptIn(ExperimentalCoroutinesApi::class)
    private fun observeAutoRefresh() {
        combine(
            settingsRepo.autoRefreshEnabledFlow,
            settingsRepo.autoRefreshIntervalFlow,
            mainScreenVisible,
            appLifecycleObserver.isForegroundFlow,
            networkMonitor.networkInfo
        ) { enabled, interval, screenVisible, appForeground, netinfo ->
            (enabled && screenVisible && appForeground && netinfo is NetworkInfo.Local) to interval
        }.flatMapLatest { (shouldRun, interval) ->
            Log.d("observeAutoRefresh", "Should run = $shouldRun")
            if (!shouldRun) {
                emptyFlow()
            } else {
                tickerFlow(interval.seconds)
            }
        }.onEach {
            Log.d("observeAutoRefresh", "Running probeDevices()")
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
    }

    private fun removeOrphanStatuses() {
        val validIds = _devices.value.map { it.id }.toSet()
        _deviceStatusMap.value =
            _deviceStatusMap.value
                .filterKeys { it in validIds }
                .toMutableMap()
    }

    private fun observeAppVisibility() {
        appLifecycleObserver.visibilityEvents
            .onEach { event ->
                when (event) {
                    AppVisibilityEvent.Foreground -> onAppForegrounded()
                    AppVisibilityEvent.Background -> onAppBackgrounded()
                }
            }
            .launchIn(viewModelScope)
    }


    private fun onAppBackgrounded() {
        Log.d("onAppBackgrounded", "Canceling active probes & saving device statuses")
        cancelAllProbes()
        deviceRepository.saveDeviceStatuses(_deviceStatusMap.value)
    }


    private suspend fun onAppForegrounded() {
        // Load stored device status
        Log.d("onAppForegrounded", "Load statuses from repository")
        _deviceStatusMap.value = deviceRepository.loadDeviceStatuses()
        removeOrphanStatuses()

        // refresh network status, just in case...
        networkMonitor.refresh()
        Log.d(
            "onAppForegrounded",
            "Refreshed network status = ${networkState.value.javaClass.simpleName}"
        )
        observeNetwork()
    }

    @Suppress("SENSELESS_COMPARISON")
    fun getDevices() {
        viewModelScope.launch {
            _devices.value = deviceRepository.getDevices()
            val toRemove = _devices.value.filter { it.interfaces == null } // NOSONAR
            if (toRemove.isNotEmpty()) {
                toRemove.forEach { deviceRepository.removeDevice(it) }
                _devices.value = deviceRepository.getDevices()
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
            deviceRepository.addDevice(device)
            getDevices()
        }
    }

    fun addDiscoveredDevice(discoveredDevice: Device) {
        val found = DeviceMatcher(stored = _devices.value).findDeviceToAdd(discoveredDevice)
        if (found != null) {
            updateDevice(found, discoveredDevice)
        } else
            viewModelScope.launch {
                deviceRepository.addDevice(discoveredDevice)
                getDevices()
            }
    }

    fun addDevices(devices: List<Device>) {
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
            deviceRepository.addDevices(toSave)

            // update the existing ones
            toUpdate.forEach {
                val normalized = it.second
                normalized.normalize()
                deviceRepository.updateDevice(it.first, normalized)
            }

            getDevices()
        }
    }


    fun updateDevice(original: Device, updated: Device) {
        updated.normalize()
        viewModelScope.launch {
            inFlightProbes.remove(original.id)?.cancel()
            deviceRepository.updateDevice(original, updated)
            getDevices()
        }
    }

    fun removeDevice(device: Device) {
        viewModelScope.launch {
            inFlightProbes.remove(device.id)?.cancel()
            _deviceStatusMap.emit(_deviceStatusMap.value.filter { it.key != device.id })
            deviceRepository.removeDevice(device)
            getDevices()
        }
    }


    fun probeDevices() {
        viewModelScope.launch {
            probeDevicesInternal()
        }
    }

    private fun probeDevicesInternal() {
        // if we're not in local network do nothing
        val subnet = (networkMonitor.networkInfo.value as? NetworkInfo.Local)?.subnet
            ?: return

        viewModelScope.launch {
            _devices.value.forEach { device ->
                val deviceId = device.id ?: return@forEach

                if (!shouldLaunchProbe(deviceId)) {
                    return@forEach
                }

                val job = launchProbeJob(device, subnet)

                probeMutex.withLock {
                    inFlightProbes[deviceId] = job
                }
            }
        }
    }


    /**
     * Returns true if at least one device in the [subnet] passed or there are no devices at all
     */
    private fun hasDevicesInSubnet(subnet: String): Boolean {
        if (_devices.value.isEmpty()) {
            return true
        }

        val result = _devices.value.any { device ->
            device.interfaces.any { it.ip?.startsWith(subnet) == true }
        }

        return result
    }

    private fun handleNotInSameNetwork() {
        if (networkMonitor.networkInfo.value is NetworkInfo.Local) {
            Log.d("handleNotInSameNetwork", "Local Network. Return")
            return
        }

        // cancel probes
        cancelAllProbes()

        // set status as unknown
        _deviceStatusMap.value = _deviceStatusMap.value.mapValues {
            it.value.copy(
                state = DeviceState.UNKNOWN,
                trayReachable = false,
                pendingAction = PendingAction.None
            )
        }
    }

    private fun cancelAllProbes() {
        viewModelScope.launch {
            probeMutex.withLock {
                inFlightProbes.values.forEach { it.cancel() }
                inFlightProbes.clear()
            }
        }
    }


    @Suppress("RedundantIf")
    private suspend fun shouldLaunchProbe(deviceId: UUID): Boolean =
        probeMutex.withLock {
            if (inFlightProbes.containsKey(deviceId)) {
                false
            } else {
                true
            }
        }

    private fun launchProbeJob(
        device: Device,
        subnet: String
    ): Job = viewModelScope.launch {
        val deviceId = device.id ?: return@launch

        try {
            probeDeviceFlow(device, subnet).collect { probe ->
                handleProbeResult(device, deviceId, probe)
            }
        } finally {
            probeMutex.withLock {
                inFlightProbes.remove(deviceId)
            }
        }
    }

    private suspend fun handleProbeResult(
        device: Device,
        deviceId: UUID,
        probe: ProbeResult
    ) {
        val prevMap = _deviceStatusMap.value
        val previous = prevMap[deviceId]
            ?: DeviceStatus(device = device, trayReachable = false)

        val updated = computeDeviceStatus(
            previous = previous,
            probeResult = probe,
            refreshInterval = autoRefreshInterval.value
        )

        probe.device?.let { addDevices(listOf(it)) }

        _deviceStatusMap.emit(prevMap + (deviceId to updated))
    }

    private fun observeNetwork() {
        viewModelScope.launch {
            sameNetworkFlow.distinctUntilChanged()
                .collect { sameNetwork ->
                    if (!sameNetwork) {
                        handleNotInSameNetwork()
                    } else {
                        scheduleProbeRefresh()
                    }
                }
        }
    }

    private fun scheduleProbeRefresh() {
        viewModelScope.launch {
            delay(500)

            if (networkMonitor.networkInfo.value is NetworkInfo.Local) {
                probeDevices()
            }
        }
    }

    /* Commands to trigger */
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

                val state = when (cancellable) {
                    true -> previous[device.id]?.state ?: DeviceState.UNKNOWN
                    false -> DeviceState.OFFLINE
                }
                val status = _deviceStatusMap.value[device.id]?.copy(
                    pendingAction = pendingAction,
                    state = state
                )

                _deviceStatusMap.emit(previous + (device.id!! to status!!))
            }

            onResult(success == true)
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

    fun wakeOnLan(device: Device, onResult: (Boolean) -> Unit) {
        viewModelScope.launch {
            onResult(NetworkActions.sendWoL(device))
        }
    }

    /* Init */

    init {
        observeNetwork()
        loadInitialDataAndRefresh()
        observeAppVisibility()
        observeAutoRefresh()
    }
}