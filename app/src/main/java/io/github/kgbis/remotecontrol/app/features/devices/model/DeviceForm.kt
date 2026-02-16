package io.github.kgbis.remotecontrol.app.features.devices.model

import io.github.kgbis.remotecontrol.app.core.model.Device
import io.github.kgbis.remotecontrol.app.core.model.DeviceInfo
import io.github.kgbis.remotecontrol.app.core.model.DeviceInterface
import io.github.kgbis.remotecontrol.app.core.model.DeviceState
import io.github.kgbis.remotecontrol.app.core.model.DeviceStatus
import io.github.kgbis.remotecontrol.app.core.model.InterfaceType
import io.github.kgbis.remotecontrol.app.core.model.PendingAction
import io.github.kgbis.remotecontrol.app.core.model.sortInterfaces
import io.github.kgbis.remotecontrol.app.core.network.REMOTETRAY_PORT
import java.util.UUID

data class DeviceFormState(
    val id: UUID? = UUID.randomUUID(),
    val hostname: String = "",
    val osName: String = "",
    val osVersion: String = "",
    val trayVersion: String = "",
    val interfaces: List<InterfaceFormState> = emptyList()
)

fun DeviceFormState.applyTo(device: Device): Device {
    val interfaces = this.interfaces
        .map { iface ->
            DeviceInterface(
                ip = iface.ip,
                mac = iface.mac.takeIf { it.isNotBlank() },
                port = iface.port.toInt(),
                type = iface.type
            )
        }
        .groupBy { it.ip }
        .map { (_, sameIpIfaces) ->
            sameIpIfaces.firstOrNull { !it.mac.isNullOrBlank() }
                ?: sameIpIfaces.first()
        }

    // Little adjustment (cleaning) when OS is changed
    val actualOsVersion = if (osName != device.deviceInfo?.osName) "" else osVersion

    return device.copy(
        hostname = hostname,
        deviceInfo = DeviceInfo(osName, actualOsVersion, trayVersion),
        interfaces = interfaces
    ).sortInterfaces()
}


fun DeviceFormState.toNewDevice(): Device {
    val interfaces = this.interfaces
        .map { iface ->
            DeviceInterface(
                ip = iface.ip,
                mac = iface.mac.takeIf { it.isNotBlank() },
                port = iface.port.toInt(),
                type = iface.type
            )
        }
        .groupBy { it.ip }
        .map { (_, sameIpIfaces) ->
            sameIpIfaces.firstOrNull { !it.mac.isNullOrBlank() }
                ?: sameIpIfaces.first()
        }

    val info = DeviceInfo(osName, osVersion, trayVersion)

    return Device(
        id = id,
        hostname = hostname,
        deviceInfo = info,
        interfaces = interfaces,
        status = DeviceStatus(
            state = DeviceState.UNKNOWN,
            trayReachable = false,
            lastSeen = System.currentTimeMillis(),
            pendingAction = PendingAction.None
        )
    ).sortInterfaces()
}


data class InterfaceFormState(
    val ip: String = "",
    val port: String = "$REMOTETRAY_PORT",
    val mac: String = "",
    val type: InterfaceType = InterfaceType.UNKNOWN
)

enum class DeviceFormMode {
    CREATE,
    EDIT
}


