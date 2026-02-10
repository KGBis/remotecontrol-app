package io.github.kgbis.remotecontrol.app.core.model

import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertFalse
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test
import java.util.UUID

class DeviceTest {

    @Test
    fun `normalize trims fields and normalizes mac address`() {
        val device = Device(
            id = UUID.randomUUID(),
            hostname = "  My-PC  ",
            deviceInfo = null,
            interfaces = mutableListOf(
                DeviceInterface(
                    ip = " 192.168.1.10 ",
                    mac = "91-75-1A-EC-9A-C7",
                    port = 8000,
                    type = InterfaceType.ETHERNET
                )
            ),
            status = DeviceStatus(
                state = DeviceState.ONLINE,
                trayReachable = true,
                lastSeen = System.currentTimeMillis(),
                pendingAction = PendingAction.None
            )
        )

        device.normalize()

        assertEquals("My-PC", device.hostname)
        assertEquals("192.168.1.10", device.interfaces[0].ip)
        assertEquals("91:75:1a:ec:9a:c7", device.interfaces[0].mac)
    }

    @Test
    fun `hasMacAddress returns true when any interface has mac`() {
        val device = Device(
            id = null,
            hostname = "pc",
            deviceInfo = null,
            interfaces = mutableListOf(
                DeviceInterface(
                    ip = "10.0.0.1",
                    mac = null,
                    port = 1234,
                    type = InterfaceType.WIFI
                ),
                DeviceInterface(
                    ip = "10.0.0.2",
                    mac = "aa:bb:cc:dd:ee:ff",
                    port = 1234,
                    type = InterfaceType.ETHERNET
                )
            ),
            status = DeviceStatus(
                state = DeviceState.ONLINE,
                trayReachable = true,
                lastSeen = System.currentTimeMillis(),
                pendingAction = PendingAction.None
            )
        )

        assertTrue(device.hasMacAddress())
    }

    @Test
    fun `hasMacAddress returns false when no interface has mac`() {
        val device = Device(
            id = null,
            hostname = "pc",
            deviceInfo = null,
            interfaces = mutableListOf(
                DeviceInterface(
                    ip = "10.0.0.1",
                    mac = null,
                    port = 1234,
                    type = InterfaceType.WIFI
                )
            ),
            status = DeviceStatus(
                state = DeviceState.ONLINE,
                trayReachable = true,
                lastSeen = System.currentTimeMillis(),
                pendingAction = PendingAction.None
            )
        )

        assertFalse(device.hasMacAddress())
    }

    @Test
    fun `isRenderable returns true for valid device`() {
        val device = Device(
            id = UUID.randomUUID(),
            hostname = "pc",
            deviceInfo = null,
            interfaces = mutableListOf(
                DeviceInterface(
                    ip = "192.168.1.20",
                    mac = null,
                    port = 1234,
                    type = InterfaceType.ETHERNET
                )
            ), status = DeviceStatus(
                state = DeviceState.ONLINE,
                trayReachable = true,
                lastSeen = System.currentTimeMillis(),
                pendingAction = PendingAction.None
            )
        )

        assertTrue(device.isRenderable())
    }

    @Test
    fun `isRenderable returns false when interfaces have no ip`() {
        val device = Device(
            id = UUID.randomUUID(),
            hostname = "pc",
            deviceInfo = null,
            interfaces = mutableListOf(
                DeviceInterface(
                    ip = null,
                    mac = "aa:bb",
                    port = 1234,
                    type = InterfaceType.ETHERNET
                )
            ), status = DeviceStatus(
                state = DeviceState.ONLINE,
                trayReachable = true,
                lastSeen = System.currentTimeMillis(),
                pendingAction = PendingAction.None
            )
        )

        assertFalse(device.isRenderable())
    }

    @Test
    fun `matches returns true when mac matches ignoring case`() {
        val a = DeviceInterface(
            ip = "1.1.1.1",
            mac = "AA:BB:CC",
            port = 1,
            type = InterfaceType.WIFI
        )

        val b = DeviceInterface(
            ip = "2.2.2.2",
            mac = "aa:bb:cc",
            port = 9999,
            type = InterfaceType.ETHERNET
        )

        assertTrue(a.matches(b))
    }

    @Test
    fun `matches falls back to ip and port when mac is missing`() {
        val a = DeviceInterface(
            ip = "192.168.1.10",
            mac = null,
            port = 8080,
            type = InterfaceType.WIFI
        )

        val b = DeviceInterface(
            ip = "192.168.1.10",
            mac = null,
            port = 8080,
            type = InterfaceType.WIFI
        )

        assertTrue(a.matches(b))
    }
}