package io.github.kgbis.remotecontrol.app.features.devices

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import io.github.kgbis.remotecontrol.app.core.AppLifecycleObserver
import io.github.kgbis.remotecontrol.app.core.AppVisibilityEvent
import io.github.kgbis.remotecontrol.app.core.model.Device
import io.github.kgbis.remotecontrol.app.core.model.DeviceInterface
import io.github.kgbis.remotecontrol.app.core.model.DeviceState
import io.github.kgbis.remotecontrol.app.core.model.PendingAction
import io.github.kgbis.remotecontrol.app.core.model.matches // NOSONAR
import io.github.kgbis.remotecontrol.app.core.model.refreshKey
import io.github.kgbis.remotecontrol.app.core.model.sortInterfaces
import io.github.kgbis.remotecontrol.app.core.network.NetworkActions
import io.github.kgbis.remotecontrol.app.core.network.NetworkInfo
import io.github.kgbis.remotecontrol.app.core.network.NetworkMonitor
import io.github.kgbis.remotecontrol.app.core.network.ProbeResult
import io.github.kgbis.remotecontrol.app.core.network.computeDeviceStatus
import io.github.kgbis.remotecontrol.app.core.network.probeDeviceBestResult
import io.github.kgbis.remotecontrol.app.core.repository.DeviceRepository
import io.github.kgbis.remotecontrol.app.core.repository.SettingsRepository
import io.github.kgbis.remotecontrol.app.features.domain.DeviceMatcher
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.Job
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.emptyFlow
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.launchIn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onEach
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.time.Instant
import java.time.temporal.ChronoUnit
import java.util.UUID
import kotlin.time.Duration
import kotlin.time.Duration.Companion.seconds

class DevicesViewModel(
    application: Application,
    val deviceRepository: DeviceRepository,
    val settingsRepository: SettingsRepository,
    val networkMonitor: NetworkMonitor,
    val appLifecycleObserver: AppLifecycleObserver,
    val dispatcher: CoroutineDispatcher = Dispatchers.Main
) : AndroidViewModel(application) {

    private val inFlightProbes = mutableMapOf<UUID, Job>()

    private val probeMutex = Mutex()

    private val _devices = MutableStateFlow(deviceRepository.loadDevices())

    val devices = _devices.asStateFlow()

    val sameNetworkFlow = combine(
        networkMonitor.networkInfo,
        devices
    ) { networkInfo, _ ->
        if (networkInfo !is NetworkInfo.Local) return@combine false
        hasDevicesInSubnet(networkInfo.subnet)
    }


    val autoRefreshInterval =
        settingsRepository.autoRefreshIntervalFlow.stateIn(
            viewModelScope,
            SharingStarted.Eagerly,
            30
        )

    val autoRefreshEnable =
        settingsRepository.autoRefreshEnabledFlow.stateIn(
            viewModelScope,
            SharingStarted.Eagerly,
            true
        )

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

    @OptIn(ExperimentalCoroutinesApi::class, FlowPreview::class)
    private fun observeAutoRefresh() {
        combine(
            settingsRepository.autoRefreshEnabledFlow,
            settingsRepository.autoRefreshIntervalFlow,
            mainScreenVisible,
            appLifecycleObserver.isForegroundFlow,
            networkMonitor.networkInfo
        ) { enabled, interval, screenVisible, appForeground, netinfo ->
            (enabled && screenVisible && appForeground && netinfo is NetworkInfo.Local) to interval
        }.debounce(500).flatMapLatest { (shouldRun, interval) ->
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
        while (currentCoroutineContext().isActive) {
            delay(period)
            emit(Unit)
        }
    }

    private fun loadDevicesList() {
        _devices.value = deviceRepository.loadDevices()
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


    private suspend fun onAppBackgrounded() {
        Log.d("onAppBackgrounded", "Canceling active probes & saving devices")
        cancelAllProbes()
        deviceRepository.saveDevices(_devices.value)
    }


    private fun onAppForegrounded() {
        // Load stored devices
        loadDevicesList()

        // refresh network status, just in case...
        Log.d("onAppForegrounded", "Refresh network monitor")
        networkMonitor.refresh()

        // Observe network changes
        Log.d("onAppForegrounded", "Observe network changes")
        observeNetwork()
    }

    /* Device operations */

    /**
     * Get device by ID
     */
    fun getDeviceById(id: UUID?): Device? {
        return _devices.value.firstOrNull { it.id == id }?.sortInterfaces()
    }

    /**
     * Add a single device. If already exists an update is performed
     */
    fun addDevice(device: Device) {
        device.normalize()
        viewModelScope.launch(dispatcher) {
            val currentDevices = _devices.value.toMutableList()

            // check if device already exists
            val storedDevice = DeviceMatcher(stored = currentDevices).findDeviceToAdd(device)

            if (storedDevice == null) {
                // Add device in view model
                currentDevices.add(device)
                _devices.value = currentDevices

                // refresh
                if (!autoRefreshEnable.value) {
                    probeDevices()
                }

                // save new list in repository
                deviceRepository.saveDevices(currentDevices)
            } else {
                // update
                updateDevice(storedDevice, device)
            }
        }
    }

    fun addDiscoveredDevices(detected: List<Device>) {
        viewModelScope.launch(dispatcher) {
            val toUpdate = mutableListOf<Pair<Device, Device>>()
            val toSave = mutableListOf<Device>()

            for (device in detected) {
                val storedDevice = DeviceMatcher(stored = _devices.value).findDeviceToAdd(device)
                // Not found -> new
                if (storedDevice == null) {
                    toSave.add(device)
                } else {
                    // If a discovered device matches an existing one, we treat it as the same
                    // logical device and force the stored ID to avoid duplicates (e.g. dual-boot PCs)
                    val merged = device.copy(id = storedDevice.id)
                    toUpdate.add(storedDevice to merged)
                }
            }

            // normalize all
            toSave.forEach { it.normalize() }
            val normalizedToUpdate = toUpdate.map { (orig, toUpdate) ->
                toUpdate.normalize()
                orig to toUpdate
            }

            // remove any in-flight probe for updated
            normalizedToUpdate.forEach {
                inFlightProbes.remove(it.first.id)?.cancel()
            }

            // get all current devices
            val list = _devices.value.toMutableList()

            // add new devices to list
            list.addAll(toSave)

            // update in the list
            val updatesById = normalizedToUpdate.associateBy { it.second.id }
            val updatedList = list.map { item ->
                updatesById[item.id]?.second ?: item
            }

            // Application order:
            // 1. Update devices list (UI source of truth)
            _devices.value = updatedList

            // 2. Update status map for discovered devices
            val uuids = toSave.mapNotNull { it.id } + normalizedToUpdate.mapNotNull { it.second.id }
            setStatusOnline(uuids)

            // 3. Persist updated device list
            deviceRepository.saveDevices(_devices.value)
        }
    }

    fun updateDeviceFromProbe(deviceId: UUID, probe: ProbeResult) {
        val storedDevice = getDeviceById(deviceId)
        val mergedInterfaces = mergeInterfaces(storedDevice!!.interfaces, probe.device!!.interfaces)
        val updated = storedDevice.copy(
            interfaces = mergedInterfaces,
            deviceInfo = probe.device.deviceInfo,
            status = probe.device.status
        )

        _devices.update { current ->
            current.map { if (it.id == deviceId) updated else it }
        }
    }

    fun mergeInterfaces(
        original: List<DeviceInterface>,
        probed: List<DeviceInterface>
    ): MutableList<DeviceInterface> {

        val result = mutableListOf<DeviceInterface>()
        val usedOriginals = mutableSetOf<DeviceInterface>()

        for (p in probed) {
            val match = original.firstOrNull { it.matches(p) }

            if (match != null) {
                usedOriginals += match

                result += match.copy(
                    ip = p.ip,
                    port = p.port,
                    type = p.type,
                    mac = p.mac ?: match.mac
                    // flags manuales se conservan aquí
                )
            } else {
                // Nueva interfaz descubierta
                result += p
            }
        }

        // Interfaces manuales que no aparecieron en el probe
        result += original.filter { it !in usedOriginals }

        return result
    }


    private fun setStatusOnline(uuids: List<UUID>) {
        _devices.update { list ->
            list.map { device ->
                if (device.id in uuids) {
                    device.copy(
                        status = device.status.copy(
                            state = DeviceState.ONLINE,
                            trayReachable = true,
                            lastSeen = System.currentTimeMillis()
                        )
                    )
                } else {
                    device
                }
            }
        }
    }


    fun updateDevice(original: Device, updated: Device) {
        updated.normalize()

        val needsRefresh = shouldRefresh(original, updated)

        viewModelScope.launch(dispatcher) {
            // remove any in-flight probe for device id (just in case autorefresh is on)
            inFlightProbes.remove(original.id)?.cancel()

            // update device in view model
            _devices.update { it.map { dev -> if (dev.id == original.id) updated else dev } }

            // refresh device list (UI) if needed
            if (needsRefresh && !inFlightProbes.containsKey(updated.id)) {
                probeDevices()
            }

            // save new list in repository
            deviceRepository.saveDevices(_devices.value)
        }
    }

    private fun shouldRefresh(
        original: Device,
        updated: Device
    ): Boolean {
        if (autoRefreshEnable.value) return false

        val originalProbeKeys = original.interfaces.map { it.refreshKey() }.toSet()
        val updatedProbeKeys = updated.interfaces.map { it.refreshKey() }.toSet()

        Log.d(
            "shouldRefresh",
            "Refresh is needed for ${original.hostname}? ${originalProbeKeys != updatedProbeKeys}"
        )

        return originalProbeKeys != updatedProbeKeys
    }

    fun removeDevice(device: Device) {
        viewModelScope.launch(dispatcher) {
            // remove any in-flight probe for device id
            inFlightProbes.remove(device.id)?.cancel()

            // remove status and device in view model
            _devices.value = _devices.value.filter { dev -> dev.id != device.id }

            // save new list in repository
            deviceRepository.saveDevices(_devices.value)
        }
    }


    fun probeDevices() {
        viewModelScope.launch(dispatcher) {
            probeDevicesInternal()
        }
    }

    private fun probeDevicesInternal() {
        // if we're not in local network do nothing
        val subnet = (networkMonitor.networkInfo.value as? NetworkInfo.Local)?.subnet
            ?: return

        viewModelScope.launch(dispatcher) {
            _devices.value.forEach { device ->
                val deviceId = device.id ?: return@forEach

                if (!shouldLaunchProbe(deviceId)) {
                    Log.d("probeDevicesInternal", "Probe in flight for $deviceId")
                    return@forEach
                }

                probeMutex.withLock {
                    inFlightProbes[deviceId] = launchProbeJob(device, subnet)
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
            return
        }

        // cancel probes
        cancelAllProbes()

        // set status as unknown
        _devices.update { list ->
            list.map { device ->
                device.copy(status = device.status.copy(state = DeviceState.UNKNOWN))
            }
        }
    }

    private fun cancelAllProbes() {
        viewModelScope.launch(dispatcher) {
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
        subnet: String,
    ): Job = viewModelScope.launch(dispatcher) {
        val deviceId = device.id ?: return@launch

        try {
            handleProbeResult(
                device,
                deviceId,
                probeDeviceBestResult(device, subnet),
            )
        } finally {
            probeMutex.withLock {
                inFlightProbes.remove(deviceId)
            }
        }
    }

    private fun handleProbeResult(
        device: Device,
        deviceId: UUID,
        probe: ProbeResult
    ) {
        val previousStatus = device.status
        val effectiveProbeResult = mergeProbeWithStoredDevice(device, probe)
        val updatedStatus = computeDeviceStatus(
            previous = previousStatus,
            probeResult = effectiveProbeResult,
            refreshInterval = autoRefreshInterval.value,
        )

        // add/update in view model
        effectiveProbeResult.device?.let {
            updateDeviceFromProbe(
                deviceId,
                probe.copy(device = probe.device?.copy(status = updatedStatus))
            )
        }
    }

    private fun mergeProbeWithStoredDevice(
        stored: Device,
        probe: ProbeResult
    ): ProbeResult {
        val probedDevice = probe.device ?: return probe

        val mergedDevice = probedDevice.copy(hostname = stored.hostname, status = stored.status)
        return probe.copy(device = mergedDevice)
    }


    private fun observeNetwork() {
        viewModelScope.launch(dispatcher) {
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
        viewModelScope.launch(dispatcher) {
            if (networkMonitor.networkInfo.value is NetworkInfo.Local) {
                probeDevices()
            }
        }
    }

    /* Commands to trigger */
    fun sendShutdownCommand(
        device: Device,
        delay: Int,
        unit: String,
        onResult: (Boolean) -> Unit
    ) {
        viewModelScope.launch(dispatcher) {
            val scheduledAt = Instant.now()
            val executeAt = scheduledAt.plus(delay.toLong(), ChronoUnit.valueOf(unit))
            val cancellable = delay != 0

            val success = NetworkActions.sendMessage(
                device = device,
                command = "SHUTDOWN $delay $unit",
                NetworkActions::shutdownResponse
            )
            if (success == true) {
                val pendingAction = PendingAction.ShutdownScheduled(
                    scheduledAt = scheduledAt,
                    executeAt = executeAt,
                    cancellable = cancellable
                )

                _devices.update { devices ->
                    devices.map {
                        if (it.id == device.id) {
                            it.copy(status = it.status.copy(pendingAction = pendingAction))
                        } else {
                            it
                        }
                    }
                }
            }

            onResult(success == true)
        }
    }

    fun sendCancelShutdownCommand(device: Device, onResult: (Boolean) -> Unit) {
        viewModelScope.launch(dispatcher) {
            val success = NetworkActions.sendMessage(
                device = device,
                command = "CANCEL_SHUTDOWN",
                NetworkActions::simpleAckResponse
            )

            if (success == true) {
                _devices.update { devices ->
                    devices.map {
                        if (it.id == device.id) {
                            it.copy(status = it.status.copy(pendingAction = PendingAction.None))
                        } else {
                            it
                        }
                    }
                }
            }

            onResult(success == true)
        }
    }

    fun wakeOnLan(device: Device, onResult: (Boolean) -> Unit) {
        viewModelScope.launch(dispatcher) {
            onResult(NetworkActions.sendWoL(device))
        }
    }

    /* Init */

    init {
        viewModelScope.launch(dispatcher) {
            observeNetwork()
            observeAppVisibility()
            observeAutoRefresh()
        }
    }
}