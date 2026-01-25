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
import io.github.kgbis.remotecontrol.app.core.model.sortInterfaces
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

    private val mutex = Mutex()

    suspend fun getDevices(): List<Device> = withContext(Dispatchers.IO) { // NOSONAR
        mutex.withLock {
            val json = prefs.getString("devices", "[]") ?: "[]"
            val devices = Gson().fromJson<List<Device>>(json, deviceListType)
            devices.sortedBy { it.id }
        }
    }

    suspend fun saveDevices(devices: List<Device>) {
        mutex.withLock {
            val toJson = Gson().toJson(devices.sortedBy { it.id }.map { device -> device.sortInterfaces() }.toList())
            prefs.edit { putString("devices", toJson) }
        }
    }

    /* Device status */

    suspend fun saveDeviceStatuses(statusMap: Map<UUID, DeviceStatus>) {
        mutex.withLock {
            val json = pendingActionGson.toJson(statusMap)
            prefs.edit { putString("device_statuses", json) }
        }
    }

    suspend fun loadDeviceStatuses(): Map<UUID, DeviceStatus> =
        withContext(Dispatchers.IO) { // NOSONAR
            mutex.withLock {
                val json = prefs.getString("device_statuses", "{}") ?: "{}"
                (pendingActionGson.fromJson(json, deviceStatusMapType) ?: emptyMap())
            }
        }


}
