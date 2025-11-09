package com.example.remote.shutdown.data

import android.content.Context
import android.os.Build
import android.util.Log
import androidx.annotation.RequiresApi
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject
import java.net.DatagramPacket
import java.net.DatagramSocket
import java.net.InetAddress
import java.net.Socket
import androidx.core.content.edit

class DeviceRepository(context: Context) {

    private val prefs = context.getSharedPreferences("devices_prefs", Context.MODE_PRIVATE)

    suspend fun getDevices(): List<Device> = withContext(Dispatchers.IO) {
        val json = prefs.getString("devices", "[]") ?: "[]"
        val arr = JSONArray(json)
        val list = mutableListOf<Device>()
        for (i in 0 until arr.length()) {
            val obj = arr.getJSONObject(i)
            list.add(Device(name = obj.getString("name"), ip = obj.getString("ip")))
        }
        list
    }

    private fun saveDevices(devices: List<Device>) {
        val arr = JSONArray()
        for (d in devices) {
            val obj = JSONObject()
            obj.put("name", d.name)
            obj.put("ip", d.ip)
            arr.put(obj)
        }
        prefs.edit { putString("devices", arr.toString()) }
    }

    suspend fun addDevice(device: Device) = withContext(Dispatchers.IO) {
        val devices = getDevices().toMutableList()
        devices.removeAll { it.ip == device.ip }
        devices.add(device)
        saveDevices(devices)
    }

    suspend fun removeDevice(device: Device) = withContext(Dispatchers.IO) {
        val devices = getDevices().filterNot { it.ip == device.ip }
        saveDevices(devices)
    }

    // ----------------- Network commands (igual que antes) -----------------

    suspend fun sendShutdown(device: Device, delay: Int, unit: String): Boolean =
        withContext(Dispatchers.IO) {
            try {
                Socket(device.ip, 9999).use { socket ->
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

    suspend fun sendWOL(device: Device, mac: String): Boolean =
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
