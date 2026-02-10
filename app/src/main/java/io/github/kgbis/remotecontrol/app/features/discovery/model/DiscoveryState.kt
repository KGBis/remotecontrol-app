package io.github.kgbis.remotecontrol.app.features.discovery.model

import io.github.kgbis.remotecontrol.app.features.discovery.mdns.DiscoveredServiceEntry

data class DiscoveryState(
    val isDiscovering: Boolean = false,
    val discoveringState: DiscoveringState = DiscoveringState.IDLE,
    val discoveredServices: List<DiscoveredServiceEntry> = emptyList(),
    val devices: List<DiscoveredDevice> = emptyList(),
    val error: String? = null
)
