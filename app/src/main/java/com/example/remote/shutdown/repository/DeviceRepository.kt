package com.example.remote.shutdown.repository

import android.content.Context
import androidx.core.content.edit
import com.example.remote.shutdown.data.Device
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONObject

class DeviceRepository(context: Context) {

    private val prefs = context.getSharedPreferences("devices_prefs", Context.MODE_PRIVATE)

    /**
     * Get all devices stored
     */
    suspend fun getDevices(): List<Device> = withContext(Dispatchers.IO) {
        val json = prefs.getString("devices", "[]") ?: "[]"
        val arr = JSONArray(json)

        val list: MutableList<Device> = mutableListOf()

        for (i in 0 until arr.length()) {
            val obj = arr.getJSONObject(i)
            list.add(
                Device(
                    name = obj.getString("name"),
                    ip = obj.getString("ip"),
                    mac = obj.optString("mac"),
                )
            )
        }

        // sort by IP (converted to Long for proper sorting)
        list.sortedBy { it.ip.toIpLong() }
    }

    /**
     * Add a new device. If IP was repeated (another device with same IP) this
     * will overwrite the stored one
     */
    suspend fun addDevice(device: Device) = withContext(Dispatchers.IO) {
        val devices = getDevices().toMutableList()
        devices.removeAll { it.ip == device.ip }
        devices.add(device)
        saveDevices(devices)
    }

    /**
     * Update a device.
     * Original data is wiped to avoid creating a new one device if IP is changed
     */
    suspend fun updateDevice(original: Device, updated: Device) = withContext(Dispatchers.IO) {
        val devices = getDevices().toMutableList()
        devices.removeAll { it.ip == original.ip }
        devices.add(updated)
        saveDevices(devices)
    }

    /**
     * Delete a device
     */
    suspend fun removeDevice(device: Device) = withContext(Dispatchers.IO) {
        val devices = getDevices().filterNot { it.ip == device.ip }
        saveDevices(devices)
    }

    /**
     * Save list of devices trimming and normalizing MAC if set
     */
    private fun saveDevices(devices: List<Device>) {
        val arr = JSONArray()
        for (d in devices) {
            // Normalize before saving
            d.normalize()

            // convert to JSON
            val obj = JSONObject()
            obj.put("name", d.name)
            obj.put("ip", d.ip)
            obj.put("mac", d.mac)
            arr.put(obj)
        }
        prefs.edit { putString("devices", arr.toString()) }
    }

    private fun String.toIpLong(): Long {
        val parts = this.split(".")
        if (parts.size != 4) return 0L
        return try {
            val a = parts[0].toLong()
            val b = parts[1].toLong()
            val c = parts[2].toLong()
            val d = parts[3].toLong()
            (a shl 24) or (b shl 16) or (c shl 8) or d
        } catch (_: NumberFormatException) {
            0L
        }
    }
}