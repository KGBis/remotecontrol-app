package io.github.kgbis.remotecontrol.app.features.discovery.mdns

data class DiscoveredServiceEntry(
    val deviceId: String,
    val name: String,
    val type: String,
    val ip: String,
    val port: Int,
    val txtRecords: Map<String, String>
)