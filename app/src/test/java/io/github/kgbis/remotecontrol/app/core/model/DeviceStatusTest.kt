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
package io.github.kgbis.remotecontrol.app.core.model

import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.time.Instant
import java.util.UUID

class DeviceStatusTest {

    @Test
    fun `canShutdown is true when device is online reachable and no pending action`() {
        val device = Device(
            id = UUID.randomUUID(),
            hostname = "pc",
            deviceInfo = null,
            interfaces = mutableListOf(
                DeviceInterface(
                    ip = "192.168.1.20",
                    mac = null,
                    port = 1234,
                    type = InterfaceType.ETHERNET
                )
            ), status = DeviceStatus(
                state = DeviceState.ONLINE,
                trayReachable = true,
                pendingAction = PendingAction.None
            )
        )

        assertTrue(device.canShutdown())
    }

    @Test
    fun `canShutdown is false when shutdown is already scheduled`() {
        val instant = Instant.now()

        val device = Device(
            id = UUID.randomUUID(),
            hostname = "pc",
            deviceInfo = null,
            interfaces = mutableListOf(
                DeviceInterface(
                    ip = "192.168.1.20",
                    mac = null,
                    port = 1234,
                    type = InterfaceType.ETHERNET
                )
            ), status = DeviceStatus(
                state = DeviceState.ONLINE,
                trayReachable = true,
                pendingAction = PendingAction.ShutdownScheduled(
                    cancellable = true,
                    scheduledAt = instant,
                    executeAt = instant.plusSeconds(60)
                )
            )
        )

        assertFalse(device.canShutdown())
    }

    @Test
    fun `canCancelShutdown is true when shutdown is scheduled and cancellable`() {
        val instant = Instant.now()

        val device = Device(
            id = UUID.randomUUID(),
            hostname = "pc",
            deviceInfo = null,
            interfaces = mutableListOf(
                DeviceInterface(
                    ip = "192.168.1.20",
                    mac = null,
                    port = 1234,
                    type = InterfaceType.ETHERNET
                )
            ), status = DeviceStatus(
                state = DeviceState.ONLINE,
                trayReachable = true,
                pendingAction = PendingAction.ShutdownScheduled(
                    cancellable = true,
                    scheduledAt = instant,
                    executeAt = instant.plusSeconds(60)

                )
            )
        )

        assertTrue(device.canCancelShutdown())
    }

    @Test
    fun `canCancelShutdown is false when shutdown is not cancellable`() {
        val instant = Instant.now()

        val device = Device(
            id = UUID.randomUUID(),
            hostname = "pc",
            deviceInfo = null,
            interfaces = mutableListOf(
                DeviceInterface(
                    ip = "192.168.1.20",
                    mac = null,
                    port = 1234,
                    type = InterfaceType.ETHERNET
                )
            ), status = DeviceStatus(
                state = DeviceState.ONLINE,
                trayReachable = true,
                pendingAction = PendingAction.ShutdownScheduled(
                    cancellable = false,
                    scheduledAt = instant,
                    executeAt = instant
                )
            )
        )

        assertFalse(device.canCancelShutdown())
    }

    @Test
    fun `canWakeup is true when device is offline and has mac address`() {
        val device = Device(
            id = UUID.randomUUID(),
            hostname = "pc",
            deviceInfo = null,
            interfaces = mutableListOf(
                DeviceInterface(
                    ip = "192.168.1.20",
                    mac = "01:02:03:04:05:06",
                    port = 1234,
                    type = InterfaceType.ETHERNET
                )
            ), status = DeviceStatus(
                state = DeviceState.OFFLINE,
                trayReachable = false
            )
        )

        assertTrue(device.canWakeup())
    }

    @Test
    fun `canWakeup is false when device is offline and hasn't mac address`() {
        val device = Device(
            id = UUID.randomUUID(),
            hostname = "pc",
            deviceInfo = null,
            interfaces = mutableListOf(
                DeviceInterface(
                    ip = "192.168.1.20",
                    mac = null,
                    port = 1234,
                    type = InterfaceType.ETHERNET
                )
            ), status = DeviceStatus(
                state = DeviceState.OFFLINE,
                trayReachable = false
            )
        )

        assertFalse(device.canWakeup())
    }
}