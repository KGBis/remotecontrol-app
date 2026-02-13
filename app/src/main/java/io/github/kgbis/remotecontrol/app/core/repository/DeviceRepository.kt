package io.github.kgbis.remotecontrol.app.core.repository

import android.content.Context
import android.util.Log
import androidx.core.content.edit
import com.google.gson.reflect.TypeToken
import io.github.kgbis.remotecontrol.app.core.model.Device
import io.github.kgbis.remotecontrol.app.core.model.DeviceJson
import io.github.kgbis.remotecontrol.app.core.model.DeviceState
import io.github.kgbis.remotecontrol.app.core.model.DeviceStatus
import io.github.kgbis.remotecontrol.app.core.model.PendingAction
import io.github.kgbis.remotecontrol.app.core.model.sortInterfaces
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

private const val DEVICES_VERSION = 2
private const val DEVICES_REPO_NAME = "devices"
private const val DEVICES_VERSION_NAME = "device_version"

class DeviceRepository(context: Context): DeviceRepositoryContract {

    private val deviceListType = object : TypeToken<List<Device>>() {}.type

    private val prefs = context.getSharedPreferences("stored_devices", Context.MODE_PRIVATE)

    private val mutex = Mutex()

    /**
     * Load, converts from JSON, and updates older versions devices.
     * Sync by design
     */
    override fun loadDevices(): List<Device> {
        val json = prefs.getString(DEVICES_REPO_NAME, "[]") ?: "[]"
        Log.d("loadDevices", "json=$json")
        val loadedDevices = DeviceJson.gson.fromJson<List<Device>>(json, deviceListType)
        val devices = updateLoadedDevicesIfNeeded(loadedDevices)
        return devices.map { device -> device.sortInterfaces() }.toList().sortedBy { it.id }
    }

    override suspend fun saveDevices(devices: List<Device>) {
        mutex.withLock {
            val toJson = DeviceJson.gson.toJson(devices.sortedBy { it.id }
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
