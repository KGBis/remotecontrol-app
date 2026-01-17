package io.github.kgbis.remotecontrol.app.core.repository

import android.content.Context
import androidx.core.content.edit
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.JsonDeserializer
import com.google.gson.reflect.TypeToken
import io.github.kgbis.remotecontrol.app.core.model.Device
import io.github.kgbis.remotecontrol.app.core.model.DeviceStatus
import io.github.kgbis.remotecontrol.app.core.model.PendingAction
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import java.time.Instant
import java.util.UUID

class DeviceRepository(context: Context) {

    val pendingActionGson: Gson = GsonBuilder()
        .registerTypeAdapter(PendingAction::class.java, JsonDeserializer { json, _, _ ->
            val obj = json.asJsonObject
            val type = obj["type"]?.asString ?: "None"

            when (type) {
                "None" -> PendingAction.None
                "ShutdownScheduled" -> PendingAction.ShutdownScheduled(
                    scheduledAt = Instant.parse(obj["scheduledAt"].asString),
                    executeAt = Instant.parse(obj["executeAt"].asString),
                    cancellable = obj["cancellable"].asBoolean
                )

                else -> throw IllegalArgumentException("Unknown PendingAction type: $type")
            }
        })
        .create()

    private val deviceListType = object : TypeToken<List<Device>>() {}.type

    private val deviceStatusMapType = object : TypeToken<Map<UUID, DeviceStatus>>() {}.type

    private val prefs = context.getSharedPreferences("stored_devices", Context.MODE_PRIVATE)

    // Mutex para proteger la lista
    private val mutex = Mutex()

    suspend fun getDevices(): List<Device> = withContext(Dispatchers.IO) { // NOSONAR
        val json = prefs.getString("devices", "[]") ?: "[]"
        val devices = Gson().fromJson<List<Device>>(json, deviceListType)
        devices.sortedBy { it.id }
    }

    suspend fun addDevice(device: Device) = withContext(Dispatchers.IO) { // NOSONAR
        mutex.withLock {
            val devices = getDevices().toMutableList()
            devices.removeAll { it.id == device.id }
            devices.add(device)
            saveDevices(devices)
        }
    }

    suspend fun addDevices(devicesToAdd: List<Device>) = withContext(Dispatchers.IO) { // NOSONAR
        mutex.withLock {
            val devices = getDevices().toMutableList()
            devicesToAdd.forEach { d ->
                devices.removeAll { it.id == d.id }
                devices.add(d)
            }
            saveDevices(devices)
        }
    }

    suspend fun updateDevice(original: Device, updated: Device) = withContext(Dispatchers.IO) { // NOSONAR
        mutex.withLock {
            val devices = getDevices().toMutableList()
            devices.removeAll { it.id == original.id }
            devices.add(updated)
            saveDevices(devices)
        }
    }

    suspend fun removeDevice(device: Device) = withContext(Dispatchers.IO) { // NOSONAR
        mutex.withLock {
            val devices = getDevices().filterNot { it.id == device.id }
            saveDevices(devices)
        }
    }

    private fun saveDevices(devices: List<Device>) {
        val toJson = Gson().toJson(devices)
        prefs.edit { putString("devices", toJson) }
    }

    /* Device status */

    fun saveDeviceStatuses(statusMap: Map<UUID, DeviceStatus>) {
        val json = pendingActionGson.toJson(statusMap)
        prefs.edit { putString("device_statuses", json) }
    }

    suspend fun loadDeviceStatuses(): Map<UUID, DeviceStatus> =
        withContext(Dispatchers.IO) { // NOSONAR
            val json = prefs.getString("device_statuses", "{}") ?: "{}"
            (pendingActionGson.fromJson(json, deviceStatusMapType) ?: emptyMap())
        }


}
