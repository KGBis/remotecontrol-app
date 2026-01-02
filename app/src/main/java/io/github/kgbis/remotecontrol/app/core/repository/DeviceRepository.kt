package io.github.kgbis.remotecontrol.app.core.repository

import android.content.Context
import androidx.core.content.edit
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import io.github.kgbis.remotecontrol.app.core.model.Device
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext

class DeviceRepository(context: Context) {

    private val myType = object : TypeToken<List<Device>>() {}.type

    private val prefs = context.getSharedPreferences("stored_devices", Context.MODE_PRIVATE)

    // Mutex para proteger la lista
    private val mutex = Mutex()

    suspend fun getDevices(): List<Device> = withContext(Dispatchers.IO) {
        val json = prefs.getString("devices", "[]") ?: "[]"
        val devices = Gson().fromJson<List<Device>>(json, myType)
        devices.sortedBy { it.id }
    }

    suspend fun addDevice(device: Device) = withContext(Dispatchers.IO) {
        mutex.withLock {
            val devices = getDevices().toMutableList()
            devices.removeAll { it.id == device.id }
            devices.add(device)
            saveDevices(devices)
        }
    }

    suspend fun addDevices(devicesToAdd: List<Device>) = withContext(Dispatchers.IO) {
        mutex.withLock {
            val devices = getDevices().toMutableList()
            devicesToAdd.forEach { d ->
                devices.removeAll { it.id == d.id }
                devices.add(d)
            }
            saveDevices(devices)
        }
    }

    suspend fun updateDevice(original: Device, updated: Device) = withContext(Dispatchers.IO) {
        mutex.withLock {
            val devices = getDevices().toMutableList()
            devices.removeAll { it.id == original.id }
            devices.add(updated)
            saveDevices(devices)
        }
    }

    suspend fun removeDevice(device: Device) = withContext(Dispatchers.IO) {
        mutex.withLock {
            val devices = getDevices().filterNot { it.id == device.id }
            saveDevices(devices)
        }
    }

    private fun saveDevices(devices: List<Device>) {
        val toJson = Gson().toJson(devices)
        prefs.edit { putString("devices", toJson) }
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
