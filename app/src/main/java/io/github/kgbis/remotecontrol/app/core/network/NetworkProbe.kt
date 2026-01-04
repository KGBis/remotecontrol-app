package io.github.kgbis.remotecontrol.app.core.network

import android.util.Log
import io.github.kgbis.remotecontrol.app.core.model.Device
import io.github.kgbis.remotecontrol.app.core.model.DeviceInterface
import io.github.kgbis.remotecontrol.app.core.model.DeviceState
import io.github.kgbis.remotecontrol.app.core.model.DeviceStatus
import io.github.kgbis.remotecontrol.app.core.model.PendingAction
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
import java.util.Date

data class ProbeResult(
    val ip: String,
    val port: Int,
    val mac: String?,
    val result: ConnectionResult,
    val durationMs: Long,
)

private const val WIN_FALLBACK = 135

private const val PRIMARY_TIMEOUT = 800
private const val SECONDARY_TIMEOUT = 300


fun probeDeviceFlow(
    device: Device,
    subnet: String
): Flow<ProbeResult> = channelFlow {
    val ifaces = device.interfaces.filter { it.ip?.startsWith(subnet) == true }
    for (iface in ifaces) {
        val probe = withContext(Dispatchers.IO) { // NOSONAR
            val start = System.currentTimeMillis()
            Log.i("probeDeviceFlow", "tryConnect for ${device.hostname} (${iface.ip})...")

            val result = connect(device = device, iface = iface)

            ProbeResult(
                ip = iface.ip!!,
                port = iface.port!!,
                mac = iface.mac,
                result = result,
                durationMs = System.currentTimeMillis() - start
            )
        }

        try {
            send(probe)
        } catch (e: ClosedSendChannelException) {
            Log.d("probeDeviceFlow", "Channel closed: ${e.message}")
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
            Log.d(
                "connect",
                "Target OS is not Windows, no fallback will be tried. Returning $result"
            )
            return result
        }

        result = tryConnect(
            ip = iface.ip!!,
            port = fallbackPort,
            timeout = SECONDARY_TIMEOUT,
            isFallback = true
        )
    }

    Log.i("", "Result = $result")
    return result
}


fun computeDeviceStatus(
    previous: DeviceStatus,
    probeResult: ProbeResult,
    now: Long = System.currentTimeMillis()
): DeviceStatus {
    Log.d("computeDeviceStatus", "probe result -> $probeResult")

    return when (probeResult.result) {
        // Connection to port 6800 was fine
        ConnectionResult.OK -> {
            Log.i("computeDeviceStatus", "ConnectionResult.OK")
            DeviceStatus(
                device = previous.device,
                state = DeviceState.ONLINE,
                trayReachable = true,
                lastSeen = now,
                pendingAction = previous.pendingAction
            )
        }
        // Connection to port 6800 was refused
        ConnectionResult.OK_FALLBACK, ConnectionResult.CONNECT_ERROR -> {
            Log.i(
                "computeDeviceStatus",
                "ConnectionResult.OK_FALLBACK or ConnectionResult.CONNECT_ERROR"
            )
            DeviceStatus(
                device = previous.device,
                state = DeviceState.ONLINE,
                trayReachable = false,
                lastSeen = now,
                pendingAction = previous.pendingAction
            )
        }
        // Host unreachable
        ConnectionResult.HOST_UNREACHABLE -> {
            Log.i("computeDeviceStatus", "ConnectionResult.HOST_UNREACHABLE")
            DeviceStatus(
                device = previous.device,
                state = DeviceState.OFFLINE,
                trayReachable = false,
                lastSeen = previous.lastSeen
            )
        }

        ConnectionResult.TIMEOUT_ERROR, ConnectionResult.UNKNOWN_ERROR -> {
            val recentlySeen = now - previous.lastSeen < 3 * 60_000
            val status = when (recentlySeen) {
                true -> when (previous.state) {
                    DeviceState.ONLINE -> DeviceState.ONLINE
                    DeviceState.UNKNOWN -> DeviceState.OFFLINE
                    else -> DeviceState.UNKNOWN
                }

                false -> DeviceState.OFFLINE
            }

            Log.d("computeDeviceStatus", "TIMEOUT_ERROR. Last seen ${Date(previous.lastSeen)}")

            DeviceStatus(
                device = previous.device,
                state = status,
                trayReachable = false,
                lastSeen = previous.lastSeen,
                pendingAction = if(status == DeviceState.ONLINE) previous.pendingAction else PendingAction.None
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
            Log.d("tryConnect", "tryConnect: ip $ip:$port check if can connect")
            socket.connect(InetSocketAddress(ip, port), timeout)
            Log.d("tryConnect", "ip $ip:$port connected")
        }
        if (isFallback) {
            ConnectionResult.OK_FALLBACK
        } else {
            ConnectionResult.OK
        }
    } catch (e: Exception) {

        Log.w("tryConnect", "Exception ${e.javaClass.simpleName}, msg -> ${e.message}")
        val msg = e.message.orEmpty()
        when {
            e is NoRouteToHostException -> ConnectionResult.HOST_UNREACHABLE // host is dead / wrong ip
            e is ConnectException && msg.contains("ECONNREFUSED") -> ConnectionResult.CONNECT_ERROR   // host alive, port is closed
            e is ConnectException && msg.contains("ETIMEDOUT") -> ConnectionResult.TIMEOUT_ERROR   // host did not reply (probably OFF)
            e is SocketTimeoutException -> ConnectionResult.TIMEOUT_ERROR // undetermined
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