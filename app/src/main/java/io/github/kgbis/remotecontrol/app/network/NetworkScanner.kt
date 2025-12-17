package io.github.kgbis.remotecontrol.app.network

import android.util.Log
import io.github.kgbis.remotecontrol.app.data.Device
import io.github.kgbis.remotecontrol.app.data.DeviceStatus
import io.github.kgbis.remotecontrol.app.data.State
import kotlinx.coroutines.CoroutineDispatcher
// import io.github.kgbis.remotecontrol.app.network.NetworkScanner.portsToScan
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.InetSocketAddress
import java.net.Socket
import java.net.SocketTimeoutException

object NetworkScanner {

    const val SHUTDOWN_PORT = 6800

    private val DEEP_PORTS = listOf(
        135,   // Windows RPC
        3389,  // RDP
        22,    // SSH
        80,    // HTTP
        443    // HTTPS
    )

    private val GHOST_PORTS = listOf(
        445,   // SMB
        139    // NetBIOS
    )

    private val portsToScan = listOf(SHUTDOWN_PORT) + DEEP_PORTS + GHOST_PORTS

    private val dispatcher: CoroutineDispatcher = Dispatchers.IO

    fun infoRequest(message: String, device: Device): Triple<Boolean, String, String> {
        Log.d("infoRequest", "Processing INFO response")
        val strings = message.split(" ")

        return when (strings.size) {
            2 -> Triple(true, strings[0], strings[1])
            1 -> Triple(true, strings[0], device.mac)
            else -> Triple(false, device.ip, device.mac)
        }
    }

    fun shutdownRequest(message: String, @Suppress("unused") device: Device): Boolean {
        Log.d("shutdownRequest", "Computer response: $message")
        return message == "ACK"
    }

    /**
     * Returns a [DeviceStatus] filled with info about:
     * 1. Device is online/offline
     * 2. Device can be shut down
     * 3. Device can be woken up
     *
     * The steps to achieve these are:
     * 1. Check if [SHUTDOWN_PORT] is reachable. If so, device is online and can be shut down. Go to 3.
     * If [SHUTDOWN_PORT] is not reachable, could be by shutdown computer application not being installed
     * or device offline. Go to next.
     * 2. Check if any of the [portsToScan] can be reached. If so, device is online.
     * 3. Check if device has its MAC filled in the app. If so, Wake-on-LAN can be used.
     */
    suspend fun deviceStatus(device: Device, subnet: String, timeout: Int = 500): DeviceStatus =
        withContext(dispatcher) {
            // If we're not in the same subnet as the device, why bother asking network?
            Log.d(
                "deviceStatus",
                "device ip = ${device.ip}, subnet = $subnet... Want to refresh? ${
                    device.ip.startsWith(subnet)
                } "
            )
            if (!device.ip.startsWith(subnet)) {
                return@withContext DeviceStatus()
            }

            return@withContext when (canConnect(device.ip, SHUTDOWN_PORT, timeout)) {
                null -> { // network errors: broken DNS, host unreachable, etc.
                    Log.d("deviceStatus", "Status cannot be determined. Network error")
                    DeviceStatus()
                }

                true -> { // Port 6800 replies → Fully online
                    Log.d("deviceStatus", "${device.ip} is online and can shutdown")
                    DeviceStatus(
                        state = State.Awake,
                        isOnline = true,
                        canShutdown = true
                    )
                }

                else -> { // shutdownPortResult == false → 6800 unreachable
                    // try "real" online ports
                    val switchedOn = isPcOnline(device.ip, DEEP_PORTS)
                    if (switchedOn) {
                        Log.d(
                            "deviceStatus",
                            "${device.ip} is online by checking one of $DEEP_PORTS"
                        )
                        DeviceStatus(state = State.Awake, isOnline = true)
                    }

                    // try Hibernate/suspend ports that are reachable
                    val hibernating = isPcOnline(device.ip, GHOST_PORTS)
                    val canWakeup = device.mac.isNotBlank()
                    if (hibernating) {
                        Log.d(
                            "deviceStatus",
                            "${device.ip} is in standby or hibernating by checking one of $GHOST_PORTS"
                        )
                        DeviceStatus(
                            state = State.HibernateOrStandby,
                            isOnline = false,
                            canWakeup = canWakeup
                        )
                    }

                    // if nothing of the above... it's probably down
                    Log.d("deviceStatus", "${device.ip} seems to be down")
                    DeviceStatus(state = State.Down, canWakeup = canWakeup)
                }
            }
        }

    /**
     * Returns `true` if an IP address can be reached connecting to any of the [portsToScan].
     * If none of the ports connect, `false` is returned
     */
    suspend fun isPcOnline(ip: String, portList: List<Int> = portsToScan): Boolean =
        withContext(dispatcher) {
            val init = System.currentTimeMillis()
            return@withContext if (checkPcStatus(ip, portList)) {
                Log.d(
                    "isPcOnline",
                    "$ip ==> END isPcOnline in ${System.currentTimeMillis() - init} ms"
                )
                true
            } else {
                Log.d(
                    "isPcOnline",
                    "$ip ==> END isPcOnline FALSE in ${System.currentTimeMillis() - init} ms"
                )
                false
            }
        }

    /**
     * Returns `true` if an IP address can be reached connecting to any of the [portList].
     * If none of the ports connect, `false` is returned
     */
    suspend fun checkPcStatus(
        ip: String,
        portList: List<Int> = portsToScan,
        timeout: Int = 500
    ): Boolean {
        return portList.any { port ->
            withContext(dispatcher) {
                Log.d("checkPcStatus", "Checking $ip @ $port")
                val result = canConnect(ip, port, timeout) ?: false
                Log.d("checkPcStatus", "Result in $ip @ $port -> $result")
                result
            }
        }
    }

    private fun canConnect(ip: String, port: Int, timeout: Int): Boolean? {
        return try {
            Socket().use { socket ->
                socket.connect(InetSocketAddress(ip, port), timeout)
                Log.d("canConnect", "for ip $ip:$port connected")
            }
            true
        } catch (e: Exception) {
            if (e is SocketTimeoutException) {
                return false
            }
            Log.d("canConnect", "Scanning $ip @ $port threw exception ${e.toString()}")
            null
        }
    }

}
