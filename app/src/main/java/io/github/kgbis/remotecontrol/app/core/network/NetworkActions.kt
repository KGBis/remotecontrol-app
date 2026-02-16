package io.github.kgbis.remotecontrol.app.core.network

import android.os.Build
import android.util.Log
import com.google.gson.Gson
import com.google.gson.JsonSyntaxException
import com.google.gson.reflect.TypeToken
import io.github.kgbis.remotecontrol.app.core.model.Device
import io.github.kgbis.remotecontrol.app.core.model.DeviceState
import io.github.kgbis.remotecontrol.app.core.model.DeviceStatus
import io.github.kgbis.remotecontrol.app.core.model.InterfaceType
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.BufferedWriter
import java.io.InputStreamReader
import java.io.OutputStreamWriter
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.Inet4Address
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.NetworkInterface
import java.net.Socket
import kotlin.experimental.inv

const val REMOTETRAY_PORT = 6800

object NetworkActions {

    fun simpleAckResponse(message: String): Boolean {
        Log.d("simpleAckResponse", "message=$message")
        return message == "ACK"
    }

    fun shutdownResponse(message: String): Boolean {
        Log.d("shutdownResponse", "message=$message")
        return when (message) {
            "ACK" -> true

            // Expected errors after immediate shutdown
            "SocketTimeoutException",
            "SocketException",
            "EOFException" -> true

            else -> false
        }
    }

    fun deviceInfoResponse(trayResponse: String): Device? {
        if (trayResponse.contains("ERROR")) {
            Log.w("deviceInfoResponse", "Error response: $trayResponse")
            return null
        }

        try {
            val dev = Gson().fromJson<Device>(trayResponse, object : TypeToken<Device>() {}.type)
            return dev.copy(
                status = DeviceStatus(
                    state = DeviceState.ONLINE,
                    trayReachable = true,
                    lastSeen = System.currentTimeMillis()
                )
            )

        } catch (_: JsonSyntaxException) {
            Log.w("deviceInfoResponse", "Old version response: $trayResponse")
            return null
        }
    }

    /**
     * Sends a [command] over TCP/IP to the `device.ip` and [REMOTETRAY_PORT]
     */
    suspend fun <T> sendMessage(
        device: Device,
        command: String,
        processor: (String) -> T?,
        timeout: Int = 500
    ): T? =
        withContext(Dispatchers.IO) { // NOSONAR
            var message = ""
            for (iface in device.interfaces) {
                try {
                    val socket = Socket()
                    socket.soTimeout = timeout

                    socket.connect(
                        InetSocketAddress(iface.ip!!, REMOTETRAY_PORT),
                        timeout
                    )

                    socket.use { s ->
                        val msg = "$command\n"

                        val out = BufferedWriter(OutputStreamWriter(s.getOutputStream()))
                        val reader = BufferedReader(InputStreamReader(s.getInputStream()))

                        // Write request
                        out.write(msg)
                        out.flush()

                        // read response
                        val message = reader.readLine()
                        return@withContext processor(message)
                    }
                } catch (e: Exception) {
                    Log.e("sendMessage", "failed: $e -> ${e.message}")
                    if (message.isEmpty()) message = e.javaClass.simpleName
                }
            }

            return@withContext null
        }

    suspend fun sendWoL(device: Device): Boolean =
        withContext(Dispatchers.IO) { // NOSONAR

            var sent = false

            val orderedIfaces = device.interfaces.sortedBy {
                when (it.type) {
                    InterfaceType.ETHERNET -> 0
                    InterfaceType.UNKNOWN -> 1
                    InterfaceType.WIFI -> 2
                }
            }

            for (iface in orderedIfaces) {
                val mac = iface.mac ?: continue

                try {
                    sendMagicPacket(mac)
                    sent = true
                } catch (e: Exception) {
                    Log.w("sendWoL", "Failed WoL via ${iface.type}: ${e.message}")
                }
            }

            sent
        }

    private fun sendMagicPacket(mac: String) {
        val macBytes = mac.split(":").map { it.toInt(16).toByte() }.toByteArray()
        val bytes = ByteArray(6 + 16 * macBytes.size)

        repeat(6) { bytes[it] = 0xFF.toByte() }
        for (i in 6 until bytes.size step macBytes.size) {
            System.arraycopy(macBytes, 0, bytes, i, macBytes.size)
        }

        val address = InetAddress.getByName(getBroadcastAddress())
        DatagramSocket().use { socket ->
            socket.send(DatagramPacket(bytes, bytes.size, address, 9))
        }
    }

    fun getBroadcastAddress(
        emulatorFallback: String = "192.168.1.255"
    ): String {
        // Detect if emulator by fingerprint o typical 10.0.x.x IP
        val isEmulator =
            Build.FINGERPRINT.contains("generic", ignoreCase = true) ||
                    Build.MODEL.contains("Emulator", ignoreCase = true)

        // Grab device's real IP and network mask. If nothing → fallback
        val local =
            getLocalIpAndMask() ?: return if (isEmulator) emulatorFallback else "255.255.255.255"

        // each part of the Pair
        val (ip, mask) = local

        // If in emulator use fallback
        if (isEmulator || ip.startsWith("10.0.")) {
            return emulatorFallback
        }

        // calculate broadcast IP
        val broadcast = calculateBroadcast(
            InetAddress.getByName(ip),
            InetAddress.getByName(mask)
        )

        return broadcast.hostAddress!!
    }

    /**
     * Returns (IP, mask) if network interface is valid
     */
    fun getLocalIpAndMask(): Pair<String, String>? {
        val interfaces = NetworkInterface.getNetworkInterfaces()

        for (netIf in interfaces) {
            if (!netIf.isUp || netIf.isLoopback) continue

            val interfaceAddress =
                netIf.interfaceAddresses.firstOrNull { it.address is Inet4Address } ?: continue
            val address = interfaceAddress.address as Inet4Address

            val prefix = interfaceAddress.networkPrefixLength
            val maskAddress = prefixToMask(prefix.toInt())

            return address.hostAddress!! to maskAddress.hostAddress!!
        }
        return null
    }

    /**
     * Prefix → IPv4 mask
     */
    fun prefixToMask(prefix: Int): InetAddress {
        val mask = ByteArray(4)
        var bits = prefix

        for (i in 0..3) {
            val value = if (bits >= 8) 255 else (256 - (1 shl (8 - bits)))
            mask[i] = value.toByte()
            bits -= 8
            if (bits < 0) bits = 0
        }

        return InetAddress.getByAddress(mask)
    }

    /**
     * IP + mask → broadcast
     */
    fun calculateBroadcast(ip: InetAddress, mask: InetAddress): InetAddress {
        val ipBytes = ip.address
        val maskBytes = mask.address
        val broadcast = ByteArray(ipBytes.size)

        for (i in ipBytes.indices) {
            broadcast[i] = (ipBytes[i].toInt() or maskBytes[i].inv().toInt()).toByte()
        }

        return InetAddress.getByAddress(broadcast)
    }

}