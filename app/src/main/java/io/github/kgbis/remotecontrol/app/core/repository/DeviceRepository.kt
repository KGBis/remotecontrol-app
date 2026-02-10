package io.github.kgbis.remotecontrol.app.core.repository

import android.content.Context
import android.util.Log
import androidx.core.content.edit
import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.JsonDeserializationContext
import com.google.gson.JsonDeserializer
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.JsonPrimitive
import com.google.gson.JsonSerializationContext
import com.google.gson.JsonSerializer
import com.google.gson.reflect.TypeToken
import io.github.kgbis.remotecontrol.app.core.model.Device
import io.github.kgbis.remotecontrol.app.core.model.DeviceState
import io.github.kgbis.remotecontrol.app.core.model.DeviceStatus
import io.github.kgbis.remotecontrol.app.core.model.PendingAction
import io.github.kgbis.remotecontrol.app.core.model.sortInterfaces
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import java.lang.reflect.Type
import java.time.Instant

private const val DEVICES_VERSION = 2
private const val DEVICES_REPO_NAME = "devices"
private const val DEVICES_VERSION_NAME = "device_version"

class DeviceRepository(context: Context) {

    val instantAdapter: JsonSerializer<Instant> =
        object : JsonSerializer<Instant>, JsonDeserializer<Instant> {
            override fun serialize(
                src: Instant,
                typeOfSrc: Type,
                context: JsonSerializationContext
            ): JsonElement =
                JsonPrimitive(src.toString())

            override fun deserialize(
                json: JsonElement,
                typeOfT: Type,
                context: JsonDeserializationContext
            ): Instant =
                Instant.parse(json.asString)
        }

    val pendingActionAdapter: JsonSerializer<PendingAction> =
        object : JsonSerializer<PendingAction>, JsonDeserializer<PendingAction> {

            override fun serialize(
                src: PendingAction,
                typeOfSrc: Type,
                context: JsonSerializationContext
            ): JsonElement {
                return when (src) {
                    PendingAction.None ->
                        JsonObject().apply {
                            addProperty("type", "None")
                        }

                    is PendingAction.ShutdownScheduled ->
                        JsonObject().apply {
                            addProperty("type", "ShutdownScheduled")
                            addProperty("cancellable", src.cancellable)
                            add("scheduledAt", context.serialize(src.scheduledAt))
                            add("executeAt", context.serialize(src.executeAt))
                        }
                }
            }

            override fun deserialize(
                json: JsonElement,
                typeOfT: Type,
                context: JsonDeserializationContext
            ): PendingAction {
                val obj = json.asJsonObject
                val type = obj["type"]?.asString ?: "None"

                return when (type) {
                    "None" -> PendingAction.None

                    "ShutdownScheduled" ->
                        PendingAction.ShutdownScheduled(
                            scheduledAt = context.deserialize(
                                obj["scheduledAt"],
                                Instant::class.java
                            ),
                            executeAt = context.deserialize(obj["executeAt"], Instant::class.java),
                            cancellable = obj["cancellable"].asBoolean
                        )

                    else -> error("Unknown PendingAction type: $type")
                }
            }
        }

    val gson: Gson = GsonBuilder()
        .registerTypeAdapter(Instant::class.java, instantAdapter)
        .registerTypeAdapter(PendingAction::class.java, pendingActionAdapter)
        .create()

    private val deviceListType = object : TypeToken<List<Device>>() {}.type

    private val prefs = context.getSharedPreferences("stored_devices", Context.MODE_PRIVATE)

    private val mutex = Mutex()

    /**
     * Load, converts from json, and updates older versions devices.
     * Sync by design
     */
    fun loadDevices(): List<Device> {
        val json = prefs.getString(DEVICES_REPO_NAME, "[]") ?: "[]"
        Log.d("loadDevices", "json=$json")
        val loadedDevices = gson.fromJson<List<Device>>(json, deviceListType)
        val devices = updateLoadedDevicesIfNeeded(loadedDevices)
        return devices.map { device -> device.sortInterfaces() }.toList().sortedBy { it.id }
    }

    suspend fun saveDevices(devices: List<Device>) {
        mutex.withLock {
            val toJson = gson.toJson(devices.sortedBy { it.id }
                .map { device -> device.sortInterfaces() }
                .toList())
            prefs.edit {
                putInt(DEVICES_VERSION_NAME, DEVICES_VERSION)
                putString(DEVICES_REPO_NAME, toJson)
            }
        }
    }

    private fun updateLoadedDevicesIfNeeded(loadedDevices: List<Device>): List<Device> {
        val currentDeviceVersion = prefs.getInt(DEVICES_VERSION_NAME, 1)
        // to add breaking version changes, just add a new if() branch with version
        if (currentDeviceVersion == 1) {
            return loadedDevices.map { device ->
                device.copy(
                    status = DeviceStatus(
                        state = DeviceState.UNKNOWN,
                        trayReachable = false,
                        lastSeen = System.currentTimeMillis(),
                        pendingAction = PendingAction.None
                    )
                )
            }
        }
        return loadedDevices
    }
}
