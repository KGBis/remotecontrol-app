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

        if((macConflictDevice != null && ipConflictDevice != null) && (macConflictDevice.id == ipConflictDevice.id)) {
            return ConflictResult.PossibleDuplicate(macConflictDevice)
        }

        if(macConflictDevice != null) {
            return ConflictResult.MacConflict(macConflictDevice)
        }

        if(ipConflictDevice != null) {
            return ConflictResult.IpConflict(ipConflictDevice)
        }

        return ConflictResult.None
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
