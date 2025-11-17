package com.example.remote.shutdown.network

import android.util.Log
import com.example.remote.shutdown.data.Device
import com.example.remote.shutdown.network.NetworkStatus.SHUTDOWN_PORT
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.net.Socket

object NetworkActions {

    suspend fun sendShutdown(device: Device, delay: Int, unit: String): Boolean =
        withContext(Dispatchers.IO) {
            try {
                Socket(device.ip, SHUTDOWN_PORT).use { socket ->
                    val msg = "SHUTDOWN $delay $unit"
                    Log.d("sendShutdown", msg)
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

    suspend fun sendWoL(device: Device, mac: String): Boolean =
        withContext(Dispatchers.IO) {
            try {
                val macBytes = mac.split(":").map { it.toInt(16).toByte() }.toByteArray()
                val bytes = ByteArray(6 + 16 * macBytes.size)
                for (i in 0 until 6) bytes[i] = 0xFF.toByte()
                for (i in 6 until bytes.size step macBytes.size)
                    System.arraycopy(macBytes, 0, bytes, i, macBytes.size)

                DatagramSocket().use { socket ->
                    val packet = DatagramPacket(bytes, bytes.size, InetAddress.getByName(device.ip), 9)
                    socket.send(packet)
                }
                true
            } catch (e: Exception) {
                e.printStackTrace()
                false
            }
        }
}