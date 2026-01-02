package io.github.kgbis.remotecontrol.app.features.discovery.model

import io.github.kgbis.remotecontrol.app.core.model.InterfaceType

data class DiscoveredEndpoint(
    val ip: String,
    val port: Int,
    val interfaceMac: String? = "",
    val interfaceType: InterfaceType? = InterfaceType.UNKNOWN
)
