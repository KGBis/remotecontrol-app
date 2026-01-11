package io.github.kgbis.remotecontrol.app.features.discovery

import android.app.Application
import android.util.Log
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import io.github.kgbis.remotecontrol.app.features.discovery.model.DiscoveringState
import io.github.kgbis.remotecontrol.app.core.model.InterfaceType
import io.github.kgbis.remotecontrol.app.features.discovery.mdns.DiscoveredServiceEntry
import io.github.kgbis.remotecontrol.app.features.discovery.mdns.MDNSDiscovery
import io.github.kgbis.remotecontrol.app.features.discovery.model.DiscoveredDevice
import io.github.kgbis.remotecontrol.app.features.discovery.model.DiscoveredEndpoint
import io.github.kgbis.remotecontrol.app.features.discovery.model.DiscoveryState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID

class DiscoveryViewModel(
    application: Application
) : AndroidViewModel(application) {

    private val _state = MutableStateFlow(DiscoveryState())
    val state: StateFlow<DiscoveryState> = _state

    private val mdnsDiscovery = MDNSDiscovery(application.applicationContext)

    init {
        initializeDiscovery()
        _state.update { it.copy(discoveringState = DiscoveringState.IDLE) }
    }

    private fun recomputeDevices(state: DiscoveryState): DiscoveryState {
        Log.d("recomputeDevices", "DiscoveryState (state) = $state")
        val merged = state.discoveredServices
            .groupBy { it.deviceId }
            .map { (deviceId, services) ->

                val first = services.first()

                DiscoveredDevice(
                    deviceId = deviceId,
                    name = first.name,
                    type = first.type,
                    txtRecords = first.txtRecords,
                    endpoints = services.map {
                        DiscoveredEndpoint(
                            ip = it.ip,
                            port = it.port,
                            interfaceMac = it.txtRecords["host-mac-address"]
                                ?: it.txtRecords["mac"],
                            interfaceType = InterfaceType.valueOf(
                                it.txtRecords["interface-type"] ?: "UNKNOWN"
                            )
                        )
                    }
                )
            }

        return state.copy(devices = merged)
    }


    private fun initializeDiscovery() {
        mdnsDiscovery.setDiscoveryListener(object : MDNSDiscovery.DiscoveryListener {

            override fun onServiceFound(service: MDNSDiscovery.DiscoveredService) {
                Log.d("onServiceFound", "se llama?? $service")
                val deviceId =
                    service.txtRecords["device-id"] ?: uuidFromHostname(service.txtRecords)

                val entry = DiscoveredServiceEntry(
                    deviceId = deviceId,
                    name = service.name,
                    type = service.type,
                    ip = service.host,
                    port = service.port,
                    txtRecords = service.txtRecords
                )

                Log.d("onServiceFound", "DiscoveredServiceEntry (entry) = $entry")

                _state.update { current ->
                    Log.d("update", "current = $current")
                    recomputeDevices(
                        current.copy(
                            discoveredServices = current.discoveredServices + entry
                        )
                    )
                }
            }

            override fun onServiceLost(serviceName: String) {
                _state.update { current ->
                    recomputeDevices(
                        current.copy(
                            discoveredServices = current.discoveredServices
                                .filterNot { it.name == serviceName }
                        )
                    )
                }
            }

            override fun onDiscoveryStarted() {
                _state.update { it.copy(isDiscovering = true, error = null) }
            }

            override fun onDiscoveryStopped() {
                _state.update { it.copy(isDiscovering = false) }
            }

            override fun onError(message: String) {
                _state.update { it.copy(error = message, isDiscovering = false) }
            }
        })
    }

    fun startDiscovery(serviceType: String = "_rpcctl._tcp") {
        viewModelScope.launch {
            _state.update {
                it.copy(
                    discoveredServices = emptyList(),
                    discoveringState = DiscoveringState.DISCOVERING
                )
            }
            mdnsDiscovery.startDiscovery(serviceType)
        }
    }

    fun stopDiscovery() {
        mdnsDiscovery.stopDiscovery()
        _state.update { it.copy(discoveringState = DiscoveringState.FINISHED) }
    }

    fun clearError() {
        _state.update { it.copy(error = null) }
    }

    override fun onCleared() {
        super.onCleared()
        mdnsDiscovery.stopDiscovery()
    }

    private fun uuidFromHostname(record: Map<String, String>): String {
        val hostname = record["host-name"] ?: record["hostname"] ?: "no-hostname"
        return UUID.nameUUIDFromBytes(hostname.toByteArray()).toString()
    }
}
