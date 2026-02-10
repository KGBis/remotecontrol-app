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
