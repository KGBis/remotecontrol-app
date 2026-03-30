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
package io.github.kgbis.remotecontrol.app.features.devices

import io.github.kgbis.remotecontrol.app.core.model.Device
import io.github.kgbis.remotecontrol.app.core.model.DeviceInterface
import io.github.kgbis.remotecontrol.app.core.model.DeviceState
import io.github.kgbis.remotecontrol.app.core.model.DeviceStatus
import io.github.kgbis.remotecontrol.app.core.model.PendingAction
import io.github.kgbis.remotecontrol.app.core.model.matches
import io.github.kgbis.remotecontrol.app.core.network.ConnectionResult
import io.github.kgbis.remotecontrol.app.core.network.ProbeResult
import java.time.Instant

object DeviceSupport {

    fun computeDeviceStatus(
        previous: DeviceStatus,
        probeResult: ProbeResult,
        refreshInterval: Int,
        now: Long = System.currentTimeMillis()
    ): DeviceStatus {
        // fix for overdue actions
        val pendingAction = fixOverduePendingAction(previous.pendingAction)

        return when (probeResult.result) {
            // Connection to port 6800 was fine
            ConnectionResult.OK -> {
                previous.copy(
                    state = DeviceState.ONLINE,
                    trayReachable = true,
                    lastSeen = now,
                    pendingAction = pendingAction
                )
            }
            // Connection to port 6800 was refused
            ConnectionResult.OK_FALLBACK, ConnectionResult.CONNECT_ERROR -> {
                previous.copy(
                    state = DeviceState.ONLINE,
                    trayReachable = false,
                    lastSeen = now,
                    pendingAction = pendingAction
                )
            }
            // Host unreachable. 100% sure it's turned off
            ConnectionResult.HOST_UNREACHABLE -> {
                previous.copy(
                    state = DeviceState.OFFLINE,
                    trayReachable = false,
                    pendingAction = pendingAction
                )
            }

            // Connection timeout or unknown error. Status not reliable. Calculate!
            ConnectionResult.TIMEOUT_ERROR, ConnectionResult.UNKNOWN_ERROR -> {
                val confidenceCycles = when {
                    refreshInterval <= 15 -> 1.5
                    refreshInterval <= 30 -> 1.0
                    else -> 0.5
                }

                val offlineThresholdMs = (confidenceCycles * refreshInterval * 1000).toLong()
                val recentlySeen = now - previous.lastSeen <= offlineThresholdMs

                val newState = when {
                    recentlySeen -> previous.state
                    else -> DeviceState.OFFLINE
                }

                previous.copy(
                    state = newState,
                    trayReachable = false,
                    pendingAction = pendingAction
                )
            }
        }
    }

    fun mergeProbeDeviceWithStoredDevice(
        stored: Device, probe: ProbeResult
    ): ProbeResult {
        val probedDevice = probe.device ?: return probe

        val mergedDevice = probedDevice.copy(hostname = stored.hostname, status = stored.status)
        return probe.copy(device = mergedDevice)
    }

    fun mergeInterfaces(
        original: List<DeviceInterface>, probed: List<DeviceInterface>
    ): List<DeviceInterface> {

        val result = mutableListOf<DeviceInterface>()
        val usedOriginals = mutableSetOf<DeviceInterface>()

        for (p in probed) {
            val match = original.firstOrNull { it.matches(p) }

            if (match != null) {
                usedOriginals += match

                result += match.copy(
                    ip = p.ip, port = p.port, type = p.type, mac = p.mac ?: match.mac
                    // flags manuales se conservan aquí
                )
            } else {
                // Nueva interfaz descubierta
                result += p
            }
        }

        // Interfaces manuales que no aparecieron en el probe
        result += original.filter { it !in usedOriginals }

        return result
    }

    private fun fixOverduePendingAction(pendingAction: PendingAction): PendingAction {
        return when (pendingAction) {
            is PendingAction.ShutdownScheduled -> {
                val now = Instant.now()
                if (pendingAction.executeAt.isBefore(now)) PendingAction.None else pendingAction
            }

            else -> PendingAction.None
        }

    }
}