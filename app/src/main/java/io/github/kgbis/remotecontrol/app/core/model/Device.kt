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
package io.github.kgbis.remotecontrol.app.core.model

import io.github.kgbis.remotecontrol.app.core.util.Utils.ipAsInt
import io.github.kgbis.remotecontrol.app.features.devices.model.DeviceFormState
import io.github.kgbis.remotecontrol.app.features.devices.model.InterfaceFormState
import java.util.UUID

data class Device(
    val id: UUID?,
    val hostname: String,
    val deviceInfo: DeviceInfo?,
    val interfaces: List<DeviceInterface> = listOf(),
    val status: DeviceStatus
) {
    fun hasMacAddress(): Boolean {
        return this.interfaces.any { !it.mac.isNullOrEmpty() }
    }

    fun canShutdown(): Boolean {
        return status.state == DeviceState.ONLINE && status.trayReachable && status.pendingAction == PendingAction.None
    }

    fun canCancelShutdown(): Boolean {
        return (status.state == DeviceState.ONLINE && status.trayReachable)
                && (status.pendingAction is PendingAction.ShutdownScheduled && status.pendingAction.cancellable)
    }

    fun canWakeup(): Boolean {
        return status.state == DeviceState.OFFLINE && hasMacAddress()
    }
}

/**
 * Trims fields and normalizes MAC address to colon separated & lowercase (i.e. 91:75:1a:ec:9a:c7)
 */
fun Device.normalize(): Device =
    copy(
        hostname = hostname.trim(),
        interfaces = interfaces.map {
            it.copy(
                mac = it.mac?.trim()?.replace('-', ':')?.lowercase(),
                ip = it.ip?.trim()
            )
        }
    )


/**
 * Sorts interfaces by type and IP (if more than one of same type exists)
 */
fun Device.sortInterfaces(): Device =
    copy(
        interfaces = interfaces.sortedWith(
            compareBy(
                { it.type.ordinal },
                { it.ip?.ipAsInt() ?: Int.MAX_VALUE }
            )
        )
    )


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
        osName = normalizeOsFamily(deviceInfo?.osName),
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

private fun normalizeOsFamily(osName: String?): String =
    when {
        osName == null -> ""
        osName.startsWith("Windows", true) -> "Windows"
        osName.startsWith("Linux", true) -> "Linux"
        osName.startsWith("mac", true) -> "macOS"
        else -> osName
    }

data class DeviceInfo(
    val osName: String,
    val osVersion: String,
    val trayVersion: String,
)

data class DeviceInterface(
    val ip: String?,
    val mac: String?,
    val port: Int?,
    val type: InterfaceType
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

data class DeviceStatus(
    val state: DeviceState = DeviceState.UNKNOWN,
    val trayReachable: Boolean,
    val lastSeen: Long = System.currentTimeMillis(),
    val pendingAction: PendingAction = PendingAction.None
)