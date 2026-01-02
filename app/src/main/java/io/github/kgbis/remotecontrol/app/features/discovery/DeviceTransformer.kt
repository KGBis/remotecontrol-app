package io.github.kgbis.remotecontrol.app.features.discovery

// DeviceTransformer.kt
import io.github.kgbis.remotecontrol.app.R
import io.github.kgbis.remotecontrol.app.core.model.Device
import io.github.kgbis.remotecontrol.app.core.model.DeviceInfo
import io.github.kgbis.remotecontrol.app.core.model.DeviceInterface
import io.github.kgbis.remotecontrol.app.features.discovery.model.DeviceTransformResult
import io.github.kgbis.remotecontrol.app.features.discovery.model.DiscoveredDevice
import java.util.UUID

object DeviceTransformer {

    fun transformToDevice(
        discovered: DiscoveredDevice
    ): DeviceTransformResult {

        val deviceId = discovered.deviceId

        val uuid = runCatching { UUID.fromString(deviceId) }.getOrNull()
            ?: return DeviceTransformResult.Invalid(
                discovered,
                R.string.discover_error_id_format,
                "INVALID_ID"
            )

        // host-name or hostname as fallback
        val hostname =
            discovered.txtRecords["host-name"]
                ?: discovered.txtRecords["hostname"]
                ?: return DeviceTransformResult.Outdated(
                    discovered,
                    R.string.discover_warn_old_version,
                    "NO_HOSTNAME"
                )

        val interfaces = discovered.endpoints.mapNotNull {
            val type = it.interfaceType ?: return@mapNotNull null
            val mac = it.interfaceMac
            ?: return DeviceTransformResult.Outdated(
                discovered,
                R.string.discover_warn_old_version,
                "NO_MAC_ADDRESS"
            )
            DeviceInterface(it.ip, mac, it.port, type)
        }

        // no interfaces section -> version is old
        if (interfaces.isEmpty()) {
            return DeviceTransformResult.Invalid(
                discovered,
                R.string.discover_error_old_version,
                "NO_INTERFACES"
            )
        }

        val device = Device(
            id = uuid,
            hostname = hostname,
            deviceInfo = DeviceInfo(
                osName = discovered.txtRecords["os-name"] ?: discovered.txtRecords["os"].orEmpty(),
                osVersion = discovered.txtRecords["os-version"].orEmpty(),
                trayVersion = discovered.txtRecords["tray-version"]
                    ?: discovered.txtRecords["version"].orEmpty()
            ),
        ).also {
            it.interfaces.addAll(interfaces)
        }

        return DeviceTransformResult.Ok(device)
    }
}