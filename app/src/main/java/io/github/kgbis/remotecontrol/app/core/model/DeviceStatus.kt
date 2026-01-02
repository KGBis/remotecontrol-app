package io.github.kgbis.remotecontrol.app.core.model

data class DeviceStatus(
    val device: Device,
    val state: DeviceState = DeviceState.UNKNOWN,
    val trayReachable: Boolean,
    val lastSeen: Long = 0L
) {
    val canShutdown = state == DeviceState.ONLINE && trayReachable

    val canWakeup: Boolean
        get() = state == DeviceState.OFFLINE && device.hasMacAddress()
}