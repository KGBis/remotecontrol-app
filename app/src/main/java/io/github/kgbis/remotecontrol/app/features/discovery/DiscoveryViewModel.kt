/*
 * Remote PC Control
 * Copyright (C) 2026 Enrique García (https://github.com/KGBis)
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 */
package io.github.kgbis.remotecontrol.app.features.discovery

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import io.github.kgbis.remotecontrol.app.core.model.InterfaceType
import io.github.kgbis.remotecontrol.app.features.discovery.mdns.DiscoveredServiceEntry
import io.github.kgbis.remotecontrol.app.features.discovery.mdns.MDNSDiscovery
import io.github.kgbis.remotecontrol.app.features.discovery.model.DeviceTransformResult
import io.github.kgbis.remotecontrol.app.features.discovery.model.DiscoveredDevice
import io.github.kgbis.remotecontrol.app.features.discovery.model.DiscoveredEndpoint
import io.github.kgbis.remotecontrol.app.features.discovery.model.DiscoveringState
import io.github.kgbis.remotecontrol.app.features.discovery.model.DiscoveryState
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.distinctUntilChangedBy
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import java.util.UUID

class DiscoveryViewModel(
    application: Application
) : AndroidViewModel(application) {

    private val _state = MutableStateFlow(DiscoveryState())
    val state: StateFlow<DiscoveryState> = _state

    private val mdnsDiscovery = MDNSDiscovery(application.applicationContext)

    val devices = state
        .map { it.devices }
        .distinctUntilChangedBy { list ->
            list.mapNotNull { it.txtRecords["device-id"] }.toSet()
        }
        .map { transformDiscoveredToDevices(it) }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(), emptyList())

    init {
        initializeDiscovery()
        _state.update { it.copy(discoveringState = DiscoveringState.IDLE) }
    }

    private fun transformDiscoveredToDevices(
        discoveredServices: List<DiscoveredDevice>
    ): List<DeviceTransformResult> {
        return discoveredServices.map { discovered ->
            DeviceTransformer.transformToDevice(discovered)
        }
    }

    private fun recomputeDevices(state: DiscoveryState): DiscoveryState {
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

                _state.update { discoveryState ->
                    val entries = (discoveryState.discoveredServices + entry).toSet()
                    recomputeDevices(
                        discoveryState.copy(
                            discoveredServices = entries.toList()
                        )
                    )
                }
            }

            override fun onServiceLost(serviceName: String) {
                _state.update { discoveryState ->
                    recomputeDevices(
                        discoveryState.copy(
                            discoveredServices = discoveryState.discoveredServices
                                .filterNot { it.name == serviceName }
                        )
                    )
                }
            }

            override fun onDiscoveryStarted() {
                _state.update { it.copy(isDiscovering = true, error = null, discoveredServices = emptyList()) }
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
                it.copy(discoveringState = DiscoveringState.DISCOVERING)
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
