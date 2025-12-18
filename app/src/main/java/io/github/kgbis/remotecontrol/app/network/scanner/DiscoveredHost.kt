package io.github.kgbis.remotecontrol.app.network.scanner

import java.net.InetAddress

data class DiscoveredHost(
    val name: String,
    val host: InetAddress,
    val port: Int,
    val txt: Map<String, String>
)

