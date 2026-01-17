package io.github.kgbis.remotecontrol.app.core.network

import android.util.Log
import io.github.kgbis.remotecontrol.app.core.model.Device
import io.github.kgbis.remotecontrol.app.core.model.DeviceInterface
import io.github.kgbis.remotecontrol.app.core.model.DeviceState
import io.github.kgbis.remotecontrol.app.core.model.DeviceStatus
import io.github.kgbis.remotecontrol.app.core.model.PendingAction
import io.github.kgbis.remotecontrol.app.core.network.NetworkActions.sendMessage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.channels.ClosedSendChannelException
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.channelFlow
import kotlinx.coroutines.withContext
import java.net.ConnectException
import java.net.InetSocketAddress
import java.net.NoRouteToHostException
import java.net.Socket
import java.net.SocketTimeoutException

data class ProbeResult(
    val ip: String,
    val port: Int,
    val mac: String?,
    val result: ConnectionResult,
    val durationMs: Long,
    val device: Device? = null
)

private const val WIN_FALLBACK = 135

private const val PRIMARY_TIMEOUT = 800
private const val SECONDARY_TIMEOUT = 300


fun probeDeviceFlow(
    device: Device,
    subnet: String
): Flow<ProbeResult> = channelFlow {
    // interfaces that are in the same subnet (i.e. 192.168.1.x)
    val ifaces = device.interfaces.filter { it.ip?.startsWith(subnet) == true }

    for (iface in ifaces) {
        val probe = withContext(Dispatchers.IO) { // NOSONAR
            val start = System.currentTimeMillis()
            Log.d("probeDeviceFlow", "trying connection to ${device.hostname} (${iface.ip})...")

            // just a connection try
            val result = connect(device = device, iface = iface)

            // Get full device info from tray if connection was OK
            val fullDevice =
                if (result == ConnectionResult.OK) fetchDeviceInfo(device, iface.ip!!) else device

            Log.d("probeDeviceFlow", "connection to ${device.hostname} result $result")

            ProbeResult(
                ip = iface.ip!!,
                port = iface.port!!,
                mac = iface.mac,
                result = result,
                durationMs = System.currentTimeMillis() - start,
                device = fullDevice
            )
        }

        try {
            send(probe)
        } catch (_: ClosedSendChannelException) {
            // not interested
        }

        if (probe.result == ConnectionResult.OK || probe.result == ConnectionResult.OK_FALLBACK) {
            close()
            break
        }
    }
}

private fun connect(device: Device, iface: DeviceInterface): ConnectionResult {
    var result = tryConnect(iface.ip!!, iface.port!!)

    // only try fallback connection in case of timeout or unknown errors
    if (result == ConnectionResult.TIMEOUT_ERROR || result == ConnectionResult.UNKNOWN_ERROR) {
        val osname = device.deviceInfo?.osName ?: ""
        val fallbackPort = when {
            osname.startsWith("win", true) -> WIN_FALLBACK
            else -> -1
        }

        if (fallbackPort == -1) {
            return result
        }

        Log.d("connect","Target OS is Windows, fallback will be tried.")
        result = tryConnect(
            ip = iface.ip!!,
            port = fallbackPort,
            timeout = SECONDARY_TIMEOUT,
            isFallback = true
        )
    }

    return result
}

private suspend fun fetchDeviceInfo(inDevice: Device, ipToSend: String): Device? {
    val command = "INFO $ipToSend"
    return sendMessage(
        device = inDevice,
        command = command,
        processor = NetworkActions::deviceInfoResponse
    )
}


fun computeDeviceStatus(
    previous: DeviceStatus,
    probeResult: ProbeResult,
    refreshInterval: Int,
    now: Long = System.currentTimeMillis()
): DeviceStatus {
    return when (probeResult.result) {
        // Connection to port 6800 was fine
        ConnectionResult.OK -> {
            previous.copy(state = DeviceState.ONLINE, trayReachable = true, lastSeen = now)
        }
        // Connection to port 6800 was refused
        ConnectionResult.OK_FALLBACK, ConnectionResult.CONNECT_ERROR -> {
            previous.copy(state = DeviceState.ONLINE, trayReachable = false, lastSeen = now)
        }
        // Host unreachable. 100% sure it's turned off
        ConnectionResult.HOST_UNREACHABLE -> {
            previous.copy(state = DeviceState.OFFLINE, trayReachable = false)
        }

        // Connection timeout or unknown error. Status not reliable. Calculate!
        ConnectionResult.TIMEOUT_ERROR, ConnectionResult.UNKNOWN_ERROR -> {
            val confidenceCycles = when {
                refreshInterval <= 15 -> 2.4
                refreshInterval <= 30 -> 1.5
                refreshInterval <= 45 -> 1.2
                else -> 1.1
            }

            val offlineThresholdMs = (confidenceCycles * refreshInterval * 1_000).toLong()
            val recentlySeen = now - previous.lastSeen < offlineThresholdMs
            val newState = when {
                recentlySeen -> previous.state
                else -> DeviceState.OFFLINE
            }

            previous.copy(
                state = newState,
                trayReachable = false,
                pendingAction = if (newState == DeviceState.ONLINE)
                    previous.pendingAction
                else
                    PendingAction.None
            )
        }
    }
}

fun tryConnect(
    ip: String,
    port: Int,
    timeout: Int = PRIMARY_TIMEOUT,
    isFallback: Boolean = false
): ConnectionResult {
    return try {
        Socket().use { socket ->
            socket.connect(InetSocketAddress(ip, port), timeout)
        }
        if (isFallback) {
            ConnectionResult.OK_FALLBACK
        } else {
            ConnectionResult.OK
        }
    } catch (e: Exception) {
        val msg = e.message.orEmpty()
        when (e) {
            is NoRouteToHostException -> ConnectionResult.HOST_UNREACHABLE // host is dead / wrong ip
            is ConnectException -> {
                if (msg.contains("ECONNREFUSED")) {
                    ConnectionResult.CONNECT_ERROR // host alive, port is closed
                } else
                    if (msg.contains("ETIMEDOUT")) {
                        ConnectionResult.TIMEOUT_ERROR // host did not reply (probably OFF)
                    } else {
                        Log.e("tryConnect", "UNKNOWN ERROR $msg")
                        ConnectionResult.UNKNOWN_ERROR
                    }
            }

            is SocketTimeoutException -> ConnectionResult.TIMEOUT_ERROR // undetermined
            else -> ConnectionResult.UNKNOWN_ERROR
        }

    }
}

enum class ConnectionResult() {
    OK,
    OK_FALLBACK,
    CONNECT_ERROR,
    HOST_UNREACHABLE,
    TIMEOUT_ERROR,
    UNKNOWN_ERROR
}