package io.github.kgbis.remotecontrol.app.core.model

import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.time.Instant
import java.util.UUID

class DeviceStatusTest {

    @Test
    fun `canShutdown is true when device is online reachable and no pending action`() {
        val status = DeviceStatus(
            device = deviceWithMac(),
            state = DeviceState.ONLINE,
            trayReachable = true,
            pendingAction = PendingAction.None
        )

        assertTrue(status.canShutdown)
    }

    @Test
    fun `canShutdown is false when shutdown is already scheduled`() {
        val instant = Instant.now()

        val status = DeviceStatus(
            device = deviceWithMac(),
            state = DeviceState.ONLINE,
            trayReachable = true,
            pendingAction = PendingAction.ShutdownScheduled(
                cancellable = true,
                scheduledAt = instant,
                executeAt = instant.plusSeconds(60)
            )
        )

        assertFalse(status.canShutdown)
    }

    @Test
    fun `canCancelShutdown is true when shutdown is scheduled and cancellable`() {
        val instant = Instant.now()

        val status = DeviceStatus(
            device = deviceWithMac(),
            state = DeviceState.ONLINE,
            trayReachable = true,
            pendingAction = PendingAction.ShutdownScheduled(cancellable = true,
                scheduledAt = instant,
                executeAt = instant.plusSeconds(60))
        )

        assertTrue(status.canCancelShutdown)
    }

    @Test
    fun `canCancelShutdown is false when shutdown is not cancellable`() {
        val instant = Instant.now()

        val status = DeviceStatus(
            device = deviceWithMac(),
            state = DeviceState.ONLINE,
            trayReachable = true,
            pendingAction = PendingAction.ShutdownScheduled(
                cancellable = false,
                scheduledAt = instant,
                executeAt = instant
            )
        )

        assertFalse(status.canCancelShutdown)
    }

    @Test
    fun `canWakeup is true when device is offline and has mac address`() {
        val status = DeviceStatus(
            device = deviceWithMac(),
            state = DeviceState.OFFLINE,
            trayReachable = false
        )

        assertTrue(status.canWakeup)
    }


    private fun deviceWithMac(): Device =
        Device(
            id = UUID.randomUUID(),
            hostname = "pc",
            deviceInfo = null,
            interfaces = mutableListOf(
                DeviceInterface(
                    ip = "192.168.1.10",
                    mac = "aa:bb:cc",
                    port = 1234,
                    type = InterfaceType.ETHERNET
                )
            )
        )

}