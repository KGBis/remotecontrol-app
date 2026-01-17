package io.github.kgbis.remotecontrol.app.core.model

data class DeviceStatus(
    val device: Device,
    val state: DeviceState = DeviceState.UNKNOWN,
    val trayReachable: Boolean,
    val lastSeen: Long = System.currentTimeMillis(),
    val pendingAction: PendingAction = PendingAction.None
) {
    val canShutdown =
        state == DeviceState.ONLINE && trayReachable && pendingAction == PendingAction.None

    val canCancelShutdown = (state == DeviceState.ONLINE
            && trayReachable) && (pendingAction is PendingAction.ShutdownScheduled && pendingAction.cancellable)

    val canWakeup: Boolean
        get() = state == DeviceState.OFFLINE && device.hasMacAddress()
}