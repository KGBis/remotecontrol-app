package io.github.kgbis.remotecontrol.app.core.network

import android.os.Build
import android.util.Log
import io.github.kgbis.remotecontrol.app.core.model.Device
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
import kotlin.collections.iterator
import kotlin.experimental.inv

const val SHUTDOWN_PORT = 6800

object NetworkActions {

    fun shutdownResponse(message: String, @Suppress("unused") device: Device): Boolean {
        Log.d("shutdownRequest", "Computer response: $message")
        return message == "ACK"
    }

    /**
     * Sends a [command] over TCP/IP to the `device.ip` and [SHUTDOWN_PORT]
     */
    suspend fun <T> sendMessage(
        device: Device,
        command: String,
        processor: (String, Device) -> T?,
        timeout: Int = 500
    ): T? =
        withContext(Dispatchers.IO) { // NOSONAR
            var exception = ""
            for (iface in device.interfaces) {
                try {
                    val socket = Socket()
                    socket.soTimeout = timeout

                    socket.connect(
                        InetSocketAddress(iface.ip!!, SHUTDOWN_PORT),
                        timeout
                    )
                    Log.d("sendMessage", "Connection established to ${iface.ip}")

                    socket.use { s ->
                        val msg = "$command\n"
                        Log.d("sendMessage", "Message sent: '$msg'")

                        val out = BufferedWriter(OutputStreamWriter(s.getOutputStream()))
                        val reader = BufferedReader(InputStreamReader(s.getInputStream()))

                        // Write request
                        out.write(msg)
                        out.flush()

                        // read response
                        val message = reader.readLine()
                        Log.d("sendMessage", "Message received: '$message'")

                        return@withContext processor(message, device)
                    }
                } catch (e: Exception) {
                    Log.e("sendMessage", "failed: $e -> ${e.message}")
                    if(exception.isEmpty()) exception = e.javaClass.simpleName
                }
            }
            //
            return@withContext null
        }

    suspend fun sendWoL(device: Device): Boolean =
        withContext(Dispatchers.IO) { // NOSONAR
            for (iface in device.interfaces) {
                try {
                    val macBytes =
                        iface.mac!!.split(":").map { it.toInt(16).toByte() }.toByteArray()
                    val bytes = ByteArray(6 + 16 * macBytes.size)
                    for (i in 0 until 6) bytes[i] = 0xFF.toByte()
                    for (i in 6 until bytes.size step macBytes.size)
                        System.arraycopy(macBytes, 0, bytes, i, macBytes.size)

                    val address = InetAddress.getByName(getBroadcastAddress())

                    DatagramSocket().use { socket ->
                        val packet = DatagramPacket(bytes, bytes.size, address, 9)
                        socket.send(packet)
                    }
                    return@withContext true
                } catch (e: Exception) {
                    Log.e("sendWoL", "Error sending Wake-on-LAN to ${device.hostname}. Error: $e")
                }
            }
            return@withContext false
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