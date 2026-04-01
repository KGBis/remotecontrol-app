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
 *
 * SPDX-License-Identifier: GPL-3.0-or-later
 */
package io.github.kgbis.remotecontrol.app.features.discovery

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
import org.apache.commons.lang3.math.NumberUtils
import java.util.UUID

object DeviceTransformer {

    /**
     * Use this function if mDNS payload changes, and you want to still using (migrate) older format(s).
     * UI is prepared to show them all correctly.
     *
     * - For invalid payloads return [DeviceTransformResult.Invalid]
     * - For outdated payloads return [DeviceTransformResult.Outdated]
     * - For up-to-date payloads return [DeviceTransformResult.Ok]
     *
     * #### Code examples:
     * ```kotlin
     * // No device.id = invalid
     * val uuid = runCatching { UUID.fromString(deviceId) }.getOrNull()
     *   ?: return DeviceTransformResult.Invalid(
     *       discovered,
     *       warning = DiscoveredDeviceWarning.Outdated(R.string.discover_error_id_format,"INVALID_ID")
     *      )
     *
     * val outdated =
     *
     *
     * // No host-name or hostname = outdated but usable
     * val hostname =
     *   discovered.txtRecords["host-name"] ?: discovered.txtRecords["hostname"]
     *     ?: return DeviceTransformResult.Outdated(
     *         discovered,
     *         null,
     *         DiscoveredDeviceWarning.Outdated(R.string.discover_warn_old_version, MIN_VERSION)
     *       )
     * ```
     */
    fun transformToDevice(
        discovered: DiscoveredDevice
    ): DeviceTransformResult {

        val deviceId = discovered.deviceId
        val outdated =
            DiscoveredDeviceWarning.Outdated(R.string.discover_warn_old_version, MIN_VERSION)

        // current mDNS payload includes uuid (device-id), anyway DiscoveryViewModel's onServiceFound generates one
        val uuid = UUID.fromString(deviceId)

        // current mDNS payload uses "host-name"
        val hostname = discovered.txtRecords["host-name"] ?: return DeviceTransformResult.Outdated(
            discovered,
            null,
            outdated
        )

        // current mDNS payload includes interface data (ip, mac, port and type)
        val interfaces = discovered.endpoints.mapNotNull {
            val type = it.interfaceType ?: return@mapNotNull null // UNKNOWN
            val mac = it.interfaceMac
            DeviceInterface(it.ip, mac, it.port, type)
        }

        // no interfaces => invalid (should never happen)
        if (interfaces.isEmpty()) {
            return DeviceTransformResult.Invalid(
                discovered,
                warning = DiscoveredDeviceWarning.Outdated(
                    R.string.discover_error_old_version,
                    "NO_INTERFACES"
                )
            )
        }

        // current mDNS payload uses "os-name" and "os-version"
        val osName = discovered.txtRecords["os-name"].orEmpty()
        val osVersion = discovered.txtRecords["os-version"].orEmpty()

        // current mDNS payload uses "tray-version"
        val tray =
            discovered.txtRecords["tray-version"].orEmpty()

        val device = Device(
            id = uuid,
            hostname = hostname,
            deviceInfo = DeviceInfo(
                osName = osName,
                osVersion = osVersion,
                trayVersion = tray
            ),
            status = DeviceStatus(
                state = DeviceState.ONLINE,
                trayReachable = true,
                lastSeen = System.currentTimeMillis(),
                pendingAction = PendingAction.None
            ),
            interfaces = interfaces
        )

        // We'll warn about needed update
        if (isOldVersion(device)) {
            return DeviceTransformResult.Outdated(discovered, device, outdated)
        }

        return DeviceTransformResult.Ok(device)
    }

    /**
     * Version numbering can be `[major, minor, patch]` for current release.
     * It could be also `[year, month, patch]` or even `[year, month, day, patch]` for old
     * development versioning.
     *
     * @return `true` if version is up to date or `false` if not
     */
    private fun isOldVersion(device: Device): Boolean {
        // is going to be in [major, minor, patch] format
        val minVersion = StringUtils.split(MIN_VERSION, ".")
            .map { if (NumberUtils.isParsable(it)) it.toInt() else 0 }

        // can be [2026, 3, 24] or [1, 0, 1]
        val currentVersion = StringUtils.split(device.deviceInfo?.trayVersion, ".")
            .map { if (NumberUtils.isParsable(it)) it.toInt() else 0 }

        // If it's in the format of 202x.yy.zz is old version
        if (currentVersion[0] >= 2025) {
            return true
        }

        val length = maxOf(minVersion.size, currentVersion.size)

        for (i in 0 until length) {
            val min = minVersion.getOrElse(i) { 0 }
            val dev = currentVersion.getOrElse(i) { 0 }

            if (dev > min) return false
            if (dev < min) return true
        }

        return false
    }
}