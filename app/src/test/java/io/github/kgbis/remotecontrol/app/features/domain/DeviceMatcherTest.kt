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
import io.github.kgbis.remotecontrol.app.core.model.DeviceInfo
import io.github.kgbis.remotecontrol.app.core.model.DeviceInterface
import io.github.kgbis.remotecontrol.app.core.model.DeviceStatus
import io.github.kgbis.remotecontrol.app.core.model.DeviceState
import io.github.kgbis.remotecontrol.app.core.model.InterfaceType
import io.github.kgbis.remotecontrol.app.core.model.PendingAction
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import java.util.UUID

class DeviceMatcherTest {

    @Test
    fun `device with same ip but different mac should not match`() {
        val stored = Device(
            hostname = "pc",
            deviceInfo = DeviceInfo(
                osName = "Windows 10",
                osVersion = "6.1",
                trayVersion = "2026.01.1"
            ),
            interfaces = listOf(
                DeviceInterface(
                    ip = "192.168.1.10",
                    mac = "aa:aa:aa:aa:aa:aa",
                    type = InterfaceType.ETHERNET,
                    port = 6800
                )
            ),
            id = UUID.randomUUID(),
            status = DeviceStatus(
                state = DeviceState.ONLINE,
                trayReachable = true,
                lastSeen = System.currentTimeMillis(),
                pendingAction = PendingAction.None
            ),
        )

        val incoming = Device(
            hostname = "pc",
            deviceInfo = DeviceInfo(
                osName = "Windows 10",
                osVersion = "6.1",
                trayVersion = "2026.01.1"
            ),
            interfaces = listOf(
                DeviceInterface(
                    ip = "192.168.1.10",
                    mac = "bb:bb:bb:cc:dd:ee",
                    type = InterfaceType.ETHERNET,
                    port = 6800
                )
            ),
            id = UUID.randomUUID(),
            status = DeviceStatus(
                state = DeviceState.ONLINE,
                trayReachable = true,
                lastSeen = System.currentTimeMillis(),
                pendingAction = PendingAction.None
            )
        )

        val matcher = DeviceMatcher(MatchConfig(), listOf(stored))

        assertNull(matcher.findDeviceToAdd(incoming))
    }

    @Test
    fun `device with same mac and different ip should match`() {
        val stored = Device(
            hostname = "pc",
            deviceInfo = DeviceInfo(
                osName = "Windows 10",
                osVersion = "6.1",
                trayVersion = "2026.01.1"
            ),
            interfaces = listOf(
                DeviceInterface(
                    ip = "192.168.1.10",
                    mac = "aa:aa:aa:aa:aa:aa",
                    type = InterfaceType.ETHERNET,
                    port = 6800
                )
            ),
            id = UUID.randomUUID(),
            status = DeviceStatus(
                state = DeviceState.ONLINE,
                trayReachable = true,
                lastSeen = System.currentTimeMillis(),
                pendingAction = PendingAction.None
            )
        )

        val incoming = Device(
            hostname = "pc",
            deviceInfo = DeviceInfo(
                osName = "Windows 10",
                osVersion = "6.1",
                trayVersion = "2026.01.1"
            ),
            interfaces = listOf(
                DeviceInterface(
                    ip = "192.168.1.20",
                    mac = "aa:aa:aa:aa:aa:aa",
                    type = InterfaceType.ETHERNET,
                    port = 6800
                )
            ),
            id = UUID.randomUUID(),
            status = DeviceStatus(
                state = DeviceState.ONLINE,
                trayReachable = true,
                lastSeen = System.currentTimeMillis(),
                pendingAction = PendingAction.None
            )
        )

        val matcher = DeviceMatcher(MatchConfig(), listOf(stored))

        assertNotNull(matcher.findDeviceToAdd(incoming))
    }

    // Added by mDNS
    @Test
    fun `device with same ip but no mac should match if coming from mDNS`() {
        val stored = Device(
            hostname = "pc",
            deviceInfo = DeviceInfo(
                osName = "Windows",
                osVersion = "",
                trayVersion = ""
            ),
            interfaces = listOf(
                DeviceInterface(
                    ip = "192.168.1.10",
                    mac = null,
                    type = InterfaceType.ETHERNET,
                    port = 6800
                )
            ),
            id = UUID.randomUUID(),
            status = DeviceStatus(
                state = DeviceState.ONLINE,
                trayReachable = true,
                lastSeen = System.currentTimeMillis(),
                pendingAction = PendingAction.None
            )
        )

        val incoming = Device(
            hostname = "pc",
            deviceInfo = DeviceInfo(
                osName = "Windows 10",
                osVersion = "6.1",
                trayVersion = "2026.01.1"
            ),
            interfaces = listOf(
                DeviceInterface(
                    ip = "192.168.1.10",
                    mac = "00:11:22:33:44:55",
                    type = InterfaceType.ETHERNET,
                    port = 6800
                )
            ),
            id = UUID.randomUUID(),
            status = DeviceStatus(
                state = DeviceState.ONLINE,
                trayReachable = true,
                lastSeen = System.currentTimeMillis(),
                pendingAction = PendingAction.None
            )
        )

        val matcher = DeviceMatcher(MatchConfig(), listOf(stored))

        assertNotNull(matcher.findDeviceToAdd(incoming))
    }

    // Added manually. Only hostname and interface IP given
    @Test
    fun `device with same ip but no metadata should NOT match`() {
        val stored = Device(
            hostname = "pc",
            deviceInfo = DeviceInfo(
                osName = "",
                osVersion = "",
                trayVersion = ""
            ),
            interfaces = listOf(
                DeviceInterface(
                    ip = "192.168.1.10",
                    mac = "",
                    type = InterfaceType.ETHERNET,
                    port = 6800
                )
            ),
            id = UUID.randomUUID(),
            status = DeviceStatus(
                state = DeviceState.ONLINE,
                trayReachable = true,
                lastSeen = System.currentTimeMillis(),
                pendingAction = PendingAction.None
            )
        )

        val incoming = Device(
            hostname = "Incoming PC",
            deviceInfo = DeviceInfo(osName = "", osVersion = "", trayVersion = ""),
            interfaces = listOf(
                DeviceInterface(
                    ip = "192.168.1.10",
                    mac = "",
                    type = InterfaceType.UNKNOWN,
                    port = 6800
                )
            ),
            id = UUID.randomUUID(),
            status = DeviceStatus(
                state = DeviceState.ONLINE,
                trayReachable = true,
                lastSeen = System.currentTimeMillis(),
                pendingAction = PendingAction.None
            )
        )

        val matcher = DeviceMatcher(MatchConfig(), listOf(stored))

        assertNull(matcher.findDeviceToAdd(incoming))
    }
}