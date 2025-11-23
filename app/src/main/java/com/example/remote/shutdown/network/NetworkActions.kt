package com.example.remote.shutdown.network

import android.util.Log
import com.example.remote.shutdown.data.Device
import com.example.remote.shutdown.util.Constants.SHUTDOWN_PORT
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.BufferedReader
import java.io.BufferedWriter
import java.io.ByteArrayOutputStream
import java.io.InputStream
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

object NetworkActions {

    suspend fun <T> sendMessage(
        device: Device,
        command: String,
        processor: (String, Device) -> T?,
        timeout: Int = 500
    ): T? =
        withContext(Dispatchers.IO) {
            try {
                val socket = Socket()
                socket.soTimeout = timeout
                socket.connect(InetSocketAddress(device.ip, SHUTDOWN_PORT), timeout)
                Log.i("sendMessage", "Connection established")

                socket.use { s ->
                    val msg = "$command\n"
                    Log.i("sendMessage", "Message sent: '$msg'")

                    val out = BufferedWriter(OutputStreamWriter(s.getOutputStream()))
                    val reader = BufferedReader(InputStreamReader(s.getInputStream()))

                    // Write request
                    out.write(msg)
                    out.flush()

                    // read response
                    val message = reader.readLine()
                    Log.i("sendMessage", "Message received: '$message'")

                    return@withContext processor(message, device)
                }
            } catch (e: Exception) {
                Log.i("sendMessage", "failed -> $e")
                Log.i("sendMessage", "failed", e.cause)
                return@withContext null
            }
        }

    /**
     * Tries to send an information request to target device port [SHUTDOWN_PORT].
     * @return a [Triple] with values `true`, `hostname` and `mac address`
     * if successful or `true`, `null` and `null` if not.
     */
    suspend fun sendInfoRequest(
        device: Device,
        timeout: Int = 500
    ): Triple<Boolean, String, String> =
        withContext(Dispatchers.IO) {
            try {
                val socket = Socket()
                socket.soTimeout = timeout
                socket.connect(InetSocketAddress(device.ip, SHUTDOWN_PORT), timeout)
                Log.i("sendInfoRequest", "Connection established")

                socket.use { s ->
                    val msg = "INFO ${device.ip}\n"
                    Log.i("sendInfoRequest", "Message sent: '$msg'")

                    val out = BufferedWriter(OutputStreamWriter(s.getOutputStream()))
                    val reader = BufferedReader(InputStreamReader(s.getInputStream()))

                    // Write request
                    out.write(msg)
                    out.flush()

                    // read response
                    val message = reader.readLine()
                    Log.i("sendInfoRequest", "Message received: '$message'")
                    val strings = message.split(" ")

                    return@withContext when (strings.size) {
                        2 -> Triple(true, strings[0], strings[1])
                        1 -> Triple(true, strings[0], device.mac)
                        else -> Triple(false, device.ip, device.mac)
                    }
                }
            } catch (e: Exception) {
                Log.i("sendInfoRequest", "failed -> $e")
                return@withContext Triple(false, device.ip, device.mac)
            }
        }

    suspend fun sendShutdown(device: Device, delay: Int, unit: String): Boolean =
        withContext(Dispatchers.IO) {
            try {
                Socket(device.ip, SHUTDOWN_PORT).use { socket ->
                    val msg = "SHUTDOWN $delay $unit"
                    Log.d("sendShutdown", "Message sent: '$msg'")
                    socket.getOutputStream().apply {
                        write(msg.toByteArray())
                        flush()
                    }
                }
                true
            } catch (e: Exception) {
                e.printStackTrace()
                false
            }
        }

    suspend fun sendWoL(device: Device): Boolean =
        withContext(Dispatchers.IO) {
            try {
                val macBytes = device.mac.split(":").map { it.toInt(16).toByte() }.toByteArray()
                val bytes = ByteArray(6 + 16 * macBytes.size)
                for (i in 0 until 6) bytes[i] = 0xFF.toByte()
                for (i in 6 until bytes.size step macBytes.size)
                    System.arraycopy(macBytes, 0, bytes, i, macBytes.size)

                val address = InetAddress.getByName(getBroadcastAddress())

                DatagramSocket().use { socket ->
                    val packet = DatagramPacket(bytes, bytes.size, address, 9)
                    socket.send(packet)
                }
                true
            } catch (e: Exception) {
                e.printStackTrace()
                false
            }
        }

    fun getBroadcastAddress(
        emulatorFallback: String = "192.168.1.255"
    ): String {

        // Detect if emulator by fingerprint o typical 10.0.x.x IP
        val isEmulator =
            android.os.Build.FINGERPRINT.contains("generic", ignoreCase = true) ||
                    android.os.Build.MODEL.contains("Emulator", ignoreCase = true)

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
     * Returns (IP, máscara) if network interface is valid
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

    fun InputStream.readAllBytesCompat(): ByteArray {
        return this.use { input ->
            val buffer = ByteArrayOutputStream()
            val data = ByteArray(4096)
            var n: Int
            while (input.read(data).also { n = it } != -1) {
                buffer.write(data, 0, n)
            }
            buffer.toByteArray()
        }
    }

}