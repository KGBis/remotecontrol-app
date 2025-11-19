package com.example.remote.shutdown.network

import android.util.Log
import com.example.remote.shutdown.data.Device
import com.example.remote.shutdown.util.Constants.SHUTDOWN_PORT
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.Inet4Address
import java.net.InetAddress
import java.net.NetworkInterface
import java.net.Socket
import kotlin.experimental.inv

object NetworkActions {

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

    /*fun getBroadcastAddress(): InetAddress? {
        val interfaces = NetworkInterface.getNetworkInterfaces()
        for (intf in interfaces) {
            if (!intf.isUp || intf.isLoopback) continue
            for (addr in intf.interfaceAddresses) {
                Log.i("getBroadcastAddress", "Address -> $addr")
                addr.broadcast?.let { return it } // devuelve la broadcast real de la subred
            }
        }
        return null
    }*/

    fun getBroadcastAddress(
        emulatorFallback: String = "192.168.1.255"
    ): String {

        // Detectar emulador por fingerprint o IP típica 10.0.x.x
        val isEmulator =
            android.os.Build.FINGERPRINT.contains("generic", ignoreCase = true) ||
                    android.os.Build.MODEL.contains("Emulator", ignoreCase = true)

        // Recoger IP y máscara reales del dispositivo
        // Si no tenemos datos → fallback
        val local =
            getLocalIpAndMask() ?: return if (isEmulator) emulatorFallback else "255.255.255.255"


        val (ip, mask) = local

        // Si estamos en emulador: ignoramos cálculo → la broadcast real de la LAN
        if (isEmulator || ip.startsWith("10.0.")) {
            return emulatorFallback
        }

        // Cálculo normal de broadcast
        val broadcast = calculateBroadcast(
            InetAddress.getByName(ip),
            InetAddress.getByName(mask)
        )

        return broadcast.hostAddress
    }

    /**
     * Devuelve (IP, máscara) si existe interfaz válida.
     */
    fun getLocalIpAndMask(): Pair<String, String>? {
        val interfaces = NetworkInterface.getNetworkInterfaces()

        for (netIf in interfaces) {
            if (!netIf.isUp || netIf.isLoopback) continue

            val interfaceAddress = netIf.interfaceAddresses.firstOrNull { it.address is Inet4Address } ?: continue
            val address = interfaceAddress.address as Inet4Address

            val prefix = interfaceAddress.networkPrefixLength
            val maskAddress = prefixToMask(prefix.toInt())

            return address.hostAddress to maskAddress.hostAddress
        }
        return null
    }

    /**
     * Prefijo → máscara IPv4
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
     * IP + máscara → broadcast
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