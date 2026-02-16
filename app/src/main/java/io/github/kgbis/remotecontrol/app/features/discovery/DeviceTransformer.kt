package io.github.kgbis.remotecontrol.app.features.discovery

// DeviceTransformer.kt
import io.github.kgbis.remotecontrol.app.MIN_VERSION
import io.github.kgbis.remotecontrol.app.R
import io.github.kgbis.remotecontrol.app.core.model.Device
import io.github.kgbis.remotecontrol.app.core.model.DeviceInfo
import io.github.kgbis.remotecontrol.app.core.model.DeviceInterface
import io.github.kgbis.remotecontrol.app.core.model.DeviceState
import io.github.kgbis.remotecontrol.app.core.model.DeviceStatus
import io.github.kgbis.remotecontrol.app.core.model.PendingAction
import io.github.kgbis.remotecontrol.app.features.discovery.model.DeviceTransformResult
import io.github.kgbis.remotecontrol.app.features.discovery.model.DiscoveredDevice
import io.github.kgbis.remotecontrol.app.features.discovery.model.DiscoveredDeviceWarning
import org.apache.commons.lang3.StringUtils
import java.util.UUID

object DeviceTransformer {

    fun transformToDevice(
        discovered: DiscoveredDevice
    ): DeviceTransformResult {

        val deviceId = discovered.deviceId

        val uuid = runCatching { UUID.fromString(deviceId) }.getOrNull()
            ?: return DeviceTransformResult.Invalid(
                discovered,
                warning = DiscoveredDeviceWarning.Outdated(
                    R.string.discover_error_id_format,
                    "INVALID_ID"
                )
            )

        // host-name or hostname as fallback
        val hostname =
            discovered.txtRecords["host-name"]
                ?: discovered.txtRecords["hostname"]
                ?: return DeviceTransformResult.Outdated(
                    discovered,
                    null,
                    warning = DiscoveredDeviceWarning.Outdated(
                        R.string.discover_warn_old_version,
                        "NO_HOSTNAME"
                    )
                )

        val interfaces = discovered.endpoints.mapNotNull {
            val type = it.interfaceType ?: return@mapNotNull null
            val mac = it.interfaceMac
                ?: return DeviceTransformResult.Outdated(
                    discovered,
                    null,
                    warning = DiscoveredDeviceWarning.Outdated(
                        R.string.discover_warn_old_version,
                        "NO_MAC_ADDRESS"
                    )
                )
            DeviceInterface(it.ip, mac, it.port, type)
        }

        // no interfaces section -> version is old
        if (interfaces.isEmpty()) {
            return DeviceTransformResult.Invalid(
                discovered,
                warning = DiscoveredDeviceWarning.Outdated(
                    R.string.discover_error_old_version,
                    "NO_INTERFACES"
                )
            )
        }

        val osName = discovered.txtRecords["os-name"] ?: discovered.txtRecords["os"].orEmpty()
        val tray =
            discovered.txtRecords["tray-version"] ?: discovered.txtRecords["version"].orEmpty()

        val device = Device(
            id = uuid,
            hostname = hostname,
            deviceInfo = DeviceInfo(
                osName = osName,
                osVersion = discovered.txtRecords["os-version"].orEmpty(),
                trayVersion = tray
            ),
            status = DeviceStatus(
                state = DeviceState.ONLINE,
                trayReachable = true,
                lastSeen = System.currentTimeMillis(),
                pendingAction = PendingAction.None
            ),
            interfaces = interfaces
        )/*.also {
            it.interfaces.addAll(interfaces)
        }*/

        // For version
        if (isOldVersion(device)) {
            return DeviceTransformResult.Outdated(
                discovered,
                device = device,
                warning = DiscoveredDeviceWarning.Outdated(
                    R.string.discover_warn_old_version,
                    MIN_VERSION
                )
            )
        }

        return DeviceTransformResult.Ok(device)
    }

    private fun isOldVersion(device: Device): Boolean {
        val devVersionDots = StringUtils.countMatches(device.deviceInfo?.trayVersion, ".")
        val minVersionDots = StringUtils.countMatches(MIN_VERSION, ".")

        // newer versions are yyyy.mm.revision (two dots), old were yyyy.mm.dd.revision (three dots)
        if (devVersionDots > minVersionDots) {
            return true
        }

        val deviceVer = device.deviceInfo?.trayVersion?.replace(".", "")?.toLong() ?: 0
        val minVer = MIN_VERSION.replace(".", "").toLong()

        return (deviceVer < minVer)
    }
}