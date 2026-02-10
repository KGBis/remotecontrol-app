package io.github.kgbis.remotecontrol.app.core.network

import android.util.Log
import io.github.kgbis.remotecontrol.app.core.model.Device
import io.github.kgbis.remotecontrol.app.core.model.DeviceInterface
import io.github.kgbis.remotecontrol.app.core.model.DeviceStatus
import io.github.kgbis.remotecontrol.app.core.model.DeviceState
import io.github.kgbis.remotecontrol.app.core.model.PendingAction
import io.github.kgbis.remotecontrol.app.core.model.sortInterfaces
import io.github.kgbis.remotecontrol.app.core.network.NetworkActions.sendMessage
import kotlinx.coroutines.Dispatchers
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

suspend fun probeDeviceBestResult(
    device: Device,
    subnet: String
): ProbeResult {
    // interfaces that are in the same subnet (i.e. 192.168.1.x)
    val ifaces = device.interfaces.filter { it.ip?.startsWith(subnet) == true }
    var best: ProbeResult? = null

    for (iface in ifaces) {
        val probe = withContext(Dispatchers.IO) { // NOSONAR
            val start = System.currentTimeMillis()

            // just a connection try
            val result = connect(device = device, iface = iface)

            // Get full device info from tray if connection was OK
            val fullDevice =
                if (result == ConnectionResult.OK) fetchDeviceInfo(device, iface.ip!!) else device

            ProbeResult(
                ip = iface.ip!!,
                port = iface.port!!,
                mac = iface.mac,
                result = result,
                durationMs = System.currentTimeMillis() - start,
                device = fullDevice?.sortInterfaces()
            )
        }

        if (betterThan(best, probe)) {
            best = probe
        }

        if (probe.result == ConnectionResult.OK) {
            break
        }
    }

    return best!!
}

private fun betterThan(previous: ProbeResult?, current: ProbeResult): Boolean {
    if (previous == null) return true
    if (current.result.value > previous.result.value) return true
    return false
}


private fun connect(device: Device, iface: DeviceInterface): ConnectionResult {
    var result = tryConnect(iface.ip!!, iface.port!!)

    // only try fallback connection in case of connection errors (value = 0)
    if (result.value == 0) {
        val osname = device.deviceInfo?.osName ?: ""
        val fallbackPort = when {
            osname.startsWith("win", true) -> WIN_FALLBACK
            else -> -1
        }

        if (fallbackPort == -1) {
            return result
        }

        Log.d("connect", "Target OS is Windows, fallback will be tried.")
        result = tryConnect(
            ip = iface.ip,
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
                refreshInterval <= 15 -> 1.5
                refreshInterval <= 30 -> 1.0
                else -> 0.5
            }

            val offlineThresholdMs = (confidenceCycles * refreshInterval * 1000).toLong()
            val recentlySeen = now - previous.lastSeen <= offlineThresholdMs

            Log.d(
                "computeDeviceStatus",
                "confidence=$confidenceCycles, threshold=$offlineThresholdMs"
            )
            Log.d(
                "computeDeviceStatus",
                "${now - previous.lastSeen} <= $offlineThresholdMs? $recentlySeen"
            )

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

enum class ConnectionResult(var value: Int) {
    OK(100),
    OK_FALLBACK(50),
    CONNECT_ERROR(0),
    HOST_UNREACHABLE(0),
    TIMEOUT_ERROR(0),
    UNKNOWN_ERROR(0)
}