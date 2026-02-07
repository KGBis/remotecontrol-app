package io.github.kgbis.remotecontrol.app.features.domain

import io.github.kgbis.remotecontrol.app.core.model.Device
import io.github.kgbis.remotecontrol.app.core.model.DeviceInfo
import io.github.kgbis.remotecontrol.app.core.model.DeviceInterface
import io.github.kgbis.remotecontrol.app.core.model.InterfaceType
import org.junit.jupiter.api.Assertions.assertNotNull
import org.junit.jupiter.api.Assertions.assertNull
import org.junit.jupiter.api.Test
import java.util.UUID

class DeviceMatcherTest {

    @Test
    fun `device with same ip but different mac should not match`() {
        val stored = Device(
            hostname = "pc",
            deviceInfo = DeviceInfo(
                osName = "Windows 10",
                osVersion = "6.1",
                trayVersion = "2026.01.1"
            ),
            interfaces = mutableListOf(
                DeviceInterface(
                    ip = "192.168.1.10",
                    mac = "aa:aa:aa:aa:aa:aa",
                    type = InterfaceType.ETHERNET,
                    port = 6800
                )
            ),
            id = UUID.randomUUID(),
        )

        val incoming = Device(
            hostname = "pc",
            deviceInfo = DeviceInfo(
                osName = "Windows 10",
                osVersion = "6.1",
                trayVersion = "2026.01.1"
            ),
            interfaces = mutableListOf(
                DeviceInterface(
                    ip = "192.168.1.10",
                    mac = "bb:bb:bb:cc:dd:ee",
                    type = InterfaceType.ETHERNET,
                    port = 6800
                )
            ),
            id = UUID.randomUUID(),
        )

        val matcher = DeviceMatcher(MatchConfig(), listOf(stored))

        assertNull(matcher.findDeviceToAdd(incoming))
    }

    @Test
    fun `device with same mac and different ip should match`() {
        val stored = Device(
            hostname = "pc",
            deviceInfo = DeviceInfo(
                osName = "Windows 10",
                osVersion = "6.1",
                trayVersion = "2026.01.1"
            ),
            interfaces = mutableListOf(
                DeviceInterface(
                    ip = "192.168.1.10",
                    mac = "aa:aa:aa:aa:aa:aa",
                    type = InterfaceType.ETHERNET,
                    port = 6800
                )
            ),
            id = UUID.randomUUID(),
        )

        val incoming = Device(
            hostname = "pc",
            deviceInfo = DeviceInfo(
                osName = "Windows 10",
                osVersion = "6.1",
                trayVersion = "2026.01.1"
            ),
            interfaces = mutableListOf(
                DeviceInterface(
                    ip = "192.168.1.20",
                    mac = "aa:aa:aa:aa:aa:aa",
                    type = InterfaceType.ETHERNET,
                    port = 6800
                )
            ),
            id = UUID.randomUUID(),
        )

        val matcher = DeviceMatcher(MatchConfig(), listOf(stored))

        assertNotNull(matcher.findDeviceToAdd(incoming))
    }

    // Added by mDNS
    @Test
    fun `device with same ip but no mac should match if coming from mDNS`() {
        val stored = Device(
            hostname = "pc",
            deviceInfo = DeviceInfo(
                osName = "Windows",
                osVersion = "",
                trayVersion = ""
            ),
            interfaces = mutableListOf(
                DeviceInterface(
                    ip = "192.168.1.10",
                    mac = null,
                    type = InterfaceType.ETHERNET,
                    port = 6800
                )
            ),
            id = UUID.randomUUID(),
        )

        val incoming = Device(
            hostname = "pc",
            deviceInfo = DeviceInfo(
                osName = "Windows 10",
                osVersion = "6.1",
                trayVersion = "2026.01.1"
            ),
            interfaces = mutableListOf(
                DeviceInterface(
                    ip = "192.168.1.10",
                    mac = "00:11:22:33:44:55",
                    type = InterfaceType.ETHERNET,
                    port = 6800
                )
            ),
            id = UUID.randomUUID(),
        )

        val matcher = DeviceMatcher(MatchConfig(), listOf(stored))

        assertNotNull(matcher.findDeviceToAdd(incoming))
    }

    // Added manually. Only hostname and interface IP given
    @Test
    fun `device with same ip but no metadata should NOT match`() {
        val stored = Device(
            hostname = "pc",
            deviceInfo = DeviceInfo(
                osName = "",
                osVersion = "",
                trayVersion = ""
            ),
            interfaces = mutableListOf(
                DeviceInterface(
                    ip = "192.168.1.10",
                    mac = "",
                    type = InterfaceType.ETHERNET,
                    port = 6800
                )
            ),
            id = UUID.randomUUID(),
        )

        val incoming = Device(
            hostname = "Incoming PC",
            deviceInfo = DeviceInfo(osName = "", osVersion = "", trayVersion = ""),
            interfaces = mutableListOf(
                DeviceInterface(
                    ip = "192.168.1.10",
                    mac = "",
                    type = InterfaceType.UNKNOWN,
                    port = 6800
                )
            ),
            id = UUID.randomUUID(),
        )

        val matcher = DeviceMatcher(MatchConfig(), listOf(stored))

        assertNull(matcher.findDeviceToAdd(incoming))
    }
}