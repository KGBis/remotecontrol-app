package io.github.kgbis.remotecontrol.app.features.discovery.model

data class DiscoveredDevice(
    val deviceId: String,
    val name: String,
    val type: String,
    val endpoints: List<DiscoveredEndpoint>,
    val txtRecords: Map<String, String>
)