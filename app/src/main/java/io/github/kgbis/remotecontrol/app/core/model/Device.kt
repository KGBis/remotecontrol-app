package io.github.kgbis.remotecontrol.app.core.model

import io.github.kgbis.remotecontrol.app.core.util.Utils.ipAsInt
import io.github.kgbis.remotecontrol.app.features.devices.model.DeviceFormState
import io.github.kgbis.remotecontrol.app.features.devices.model.InterfaceFormState
import java.util.UUID

data class Device(
    var id: UUID?,
    var hostname: String,
    var deviceInfo: DeviceInfo?,
    val interfaces: MutableList<DeviceInterface> = mutableListOf()
) {
    /**
     * Trims fields and normalizes MAC address to colon separated & lowercase (i.e. 91:75:1a:ec:9a:c7)
     */
    fun normalize() {
        hostname = hostname.trim()
        interfaces.forEach {
            it.mac = it.mac?.trim()?.replace('-', ':')?.lowercase()
            it.ip = it.ip?.trim()
        }
    }

    fun hasMacAddress(): Boolean {
        return this.interfaces.any { !it.mac.isNullOrEmpty() }
    }
}

fun Device.sortInterfaces(): Device {
    val ifaces =
        interfaces.toList()
            .sortedWith(compareBy({ it.type.ordinal }, { it.ip?.ipAsInt() ?: Int.MAX_VALUE }))

    interfaces.clear()
    interfaces.addAll(ifaces)

    return this
}

@Suppress("UselessCallOnNotNull")
fun Device.isRenderable(): Boolean =
    id != null &&
            !hostname.isNullOrBlank() &&
            !interfaces.isNullOrEmpty() &&
            interfaces.any { !it.ip.isNullOrBlank() }

fun Device.toFormState(): DeviceFormState =
    DeviceFormState(
        id = id,
        hostname = hostname,
        osName = deviceInfo?.osName.orEmpty(),
        osVersion = deviceInfo?.osVersion.orEmpty(),
        trayVersion = deviceInfo?.trayVersion.orEmpty(),
        interfaces = interfaces.map {
            InterfaceFormState(
                ip = it.ip.orEmpty(),
                port = it.port?.toString().orEmpty(),
                mac = it.mac.orEmpty(),
                type = it.type
            )
        }
    )

data class DeviceInfo(
    var osName: String,
    var osVersion: String,
    var trayVersion: String,
)

data class DeviceInterface(
    var ip: String?,
    var mac: String?,
    var port: Int?,
    var type: InterfaceType
)

fun DeviceInterface.refreshKey(): String = "$ip:$port:$type"

fun DeviceInterface.matches(other: DeviceInterface): Boolean {
    // 1️⃣ MAC if exists in both
    if (!mac.isNullOrBlank() && !other.mac.isNullOrBlank()) {
        return mac.equals(other.mac, ignoreCase = true)
    }

    // 2️⃣ Fallback: IP + port
    return ip == other.ip && port == other.port
}
