package io.github.kgbis.remotecontrol.app.features.devices.model

import io.github.kgbis.remotecontrol.app.core.model.Device
import io.github.kgbis.remotecontrol.app.core.model.DeviceInfo
import io.github.kgbis.remotecontrol.app.core.model.DeviceInterface
import io.github.kgbis.remotecontrol.app.core.model.InterfaceType
import io.github.kgbis.remotecontrol.app.core.network.SHUTDOWN_PORT
import java.util.UUID

data class DeviceFormState(
    val hostname: String = "",
    val osName: String = "",
    val osVersion: String = "",
    val trayVersion: String = "",
    val interfaces: List<InterfaceFormState> = emptyList()
)

fun DeviceFormState.toDevice(): Device {
    val interfaces = this.interfaces.map { iface ->
        DeviceInterface(
            ip = iface.ip,
            mac = iface.mac,
            port = iface.port.toInt(),
            type = iface.type
        )
    }.toMutableList()

    val info = DeviceInfo(this.osName, this.osVersion, this.trayVersion)

    return Device(
        id = UUID.randomUUID(),
        hostname = this.hostname,
        deviceInfo = info,
        interfaces = interfaces,
    )
}

data class InterfaceFormState(
    val ip: String = "",
    val port: String = "$SHUTDOWN_PORT",
    val mac: String = "",
    val type: InterfaceType = InterfaceType.UNKNOWN
)

enum class DeviceFormMode {
    CREATE,
    EDIT
}


