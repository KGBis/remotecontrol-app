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
package io.github.kgbis.remotecontrol.app.features.domain

import android.util.Log
import io.github.kgbis.remotecontrol.app.core.model.Device
import io.github.kgbis.remotecontrol.app.features.devices.model.DeviceFormState
import java.util.UUID

class DeviceConflictChecker(
    private val devices: List<Device>
) {
    fun check(form: DeviceFormState, currentId: UUID?): ConflictResult {
        val formMacs = form.interfaces.map { it.mac }.filter { it.isNotEmpty() }.toSet()
        val formIps = form.interfaces.map { it.ip }.filter { it.isNotEmpty() }.toSet()

        val macConflictDevice =
            findMacConflicts(currentId = currentId, macs = formMacs, devices = devices)
        val ipConflictDevice =
            findIpConflicts(currentId = currentId, ips = formIps, devices = devices)

        Log.d("check", "macConflictDevice = $macConflictDevice")
        Log.d("check", "ipConflictDevice = $ipConflictDevice")

        if ((macConflictDevice != null && ipConflictDevice != null) && (macConflictDevice.id == ipConflictDevice.id)) {
            return ConflictResult.PossibleDuplicate(macConflictDevice)
        }

        if (macConflictDevice != null) {
            val deviceMacs =
                macConflictDevice.interfaces.map { it.mac }.filter { it?.isNotEmpty() == true }
                    .toSet()
            return ConflictResult.MacConflict(
                macConflictDevice,
                conflictsToString(formMacs, deviceMacs)
            )
        }

        if (ipConflictDevice != null) {
            val deviceIps =
                ipConflictDevice.interfaces.map { it.ip }.filter { it?.isNotEmpty() == true }
                    .toSet()
            return ConflictResult.IpConflict(
                ipConflictDevice,
                conflictsToString(formIps, deviceIps)
            )
        }

        return ConflictResult.None
    }

    private fun conflictsToString(formSet: Set<String>, deviceSet: Set<String?>): String {
        val mutable = formSet.toMutableSet()
        mutable.retainAll(deviceSet)
        val array = Array(mutable.size) { mutable.elementAt(it) }
        return array.joinToString(separator = "\n")
    }

    private fun findMacConflicts(
        currentId: UUID?,
        macs: Set<String>,
        devices: List<Device>
    ): Device? =
        devices.firstOrNull { device ->
            device.id != currentId &&
                    device.interfaces.any { it.mac in macs }
        }

    private fun findIpConflicts(
        currentId: UUID?,
        ips: Set<String>,
        devices: List<Device>
    ): Device? =
        devices.firstOrNull { device ->
            device.id != currentId &&
                    device.interfaces.any { it.ip in ips }
        }
}
