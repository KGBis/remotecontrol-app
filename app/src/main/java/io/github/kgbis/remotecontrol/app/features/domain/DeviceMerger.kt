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
package io.github.kgbis.remotecontrol.app.features.domain

import io.github.kgbis.remotecontrol.app.core.model.Device
import io.github.kgbis.remotecontrol.app.features.devices.DeviceSupport.mergeInterfaces

object DeviceMerger {

    fun mergeFromDiscovery(stored: Device, discovered: Device): Device {
        return baseMerge(stored, discovered)
    }

    fun mergeFromProbe(stored: Device, probed: Device): Device {
        val mergedInterfaces = mergeInterfaces(stored.interfaces, probed.interfaces)
        return stored.copy(
            interfaces = mergedInterfaces,
            deviceInfo = probed.deviceInfo,
            status = probed.status
        )
    }

    private fun baseMerge(stored: Device, incoming: Device): Device {
        return incoming.copy(
            id = stored.id,
            hostname = stored.hostname // 👈 centralized rule
        )
    }
}