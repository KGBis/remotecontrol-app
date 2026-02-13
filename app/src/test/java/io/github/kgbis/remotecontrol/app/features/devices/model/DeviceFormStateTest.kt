package io.github.kgbis.remotecontrol.app.features.devices.model

import io.github.kgbis.remotecontrol.app.core.model.InterfaceType
import io.github.kgbis.remotecontrol.app.core.model.toFormState
import io.github.kgbis.remotecontrol.app.utils.DeviceFixtures
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.Test

class DeviceFormStateTest {

    @Test
    fun `when Device is transformed into DeviceFormState osName is normalized`() {
        val device = DeviceFixtures.device("titan") // osName = "Windows 11"
        assertTrue(device.deviceInfo?.osName == "Windows 11")

        val formData = device.toFormState()
        assertEquals(formData.osName, "Windows")
    }

    @Test
    fun `when DeviceFormState is applied to a device and OS is changed version is cleared`() {
        val formData = DeviceFormState(hostname = "Titan", osName = "Linux", osVersion = "10.0")
        val device = DeviceFixtures.device("titan") // osName = "Windows 11", osVersion = "10.0"
        assertTrue(device.deviceInfo?.osName == "Windows 11")
        assertTrue(device.deviceInfo?.osVersion == "10.0")

        val resultingDevice = formData.applyTo(device)

        assertTrue(resultingDevice.deviceInfo?.osName == "Linux")
        assertTrue(resultingDevice.deviceInfo?.osVersion?.isEmpty() == true)
    }

    @Test
    fun `when DeviceFormState is applied to a device different IPs are added even if same MAC`() {
        val formData = DeviceFormState(
            hostname = "Titan",
            osName = "Linux",
            osVersion = "10.0",
            interfaces = listOf(
                InterfaceFormState(
                    ip = "192.168.1.43",
                    mac = "58:11:22:dc:a9:5c",
                    port = "6800",
                    type = InterfaceType.ETHERNET
                ),
                InterfaceFormState(
                    ip = "192.168.1.44",
                    mac = "58:11:22:dc:a9:5c",
                    port = "6800",
                    type = InterfaceType.UNKNOWN
                )
            )
        )
        val device = DeviceFixtures.device("titan")

        val resultingDevice = formData.applyTo(device)

        assertEquals(resultingDevice.interfaces.size, 2)
    }

    @Test
    fun `when DeviceFormState is applied to a device, equal IPs are merged`() {
        val formData = DeviceFormState(
            hostname = "Titan",
            osName = "Linux",
            osVersion = "10.0",
            interfaces = listOf(
                InterfaceFormState(
                    ip = "192.168.1.44",
                    mac = "58:11:22:dc:a9:5c",
                    port = "6800",
                    type = InterfaceType.UNKNOWN
                ),
                InterfaceFormState(
                    ip = "192.168.1.43",
                    type = InterfaceType.WIFI
                )
            )
        )
        val device = DeviceFixtures.device("titan")

        val resultingDevice = formData.applyTo(device)

        // If deleted or changed, the MAC will be lost or different from stored device!
        val iface =
            resultingDevice.interfaces.firstOrNull { it.ip == "192.168.1.43" && it.type == InterfaceType.WIFI }

        assertEquals(iface?.mac, null)
        assertEquals(resultingDevice.interfaces.size, 2)
    }

    @Test
    fun `when DeviceFormState is applied to a device and no changes in interfaces, they remain the same`() {
        val formData = DeviceFormState(
            hostname = "Titan Linux",
            osName = "Linux",
            osVersion = "10.0",
            trayVersion = "2026.01.5",
            interfaces = listOf(
                InterfaceFormState(
                    ip = "192.168.1.34",
                    mac = "a0:29:42:98:1e:a3",
                    port = "6800",
                    type = InterfaceType.WIFI
                ),
                InterfaceFormState(
                    ip = "192.168.1.43",
                    mac = "58:11:22:dc:a9:5c",
                    port = "6800",
                    type = InterfaceType.ETHERNET
                )
            )
        )
        val device = DeviceFixtures.device("titan")

        val resultingDevice = formData.applyTo(device)

        assertEquals("Titan Linux", resultingDevice.hostname)
        assertEquals(resultingDevice.interfaces.size, 2)
        assertEquals(device.interfaces, resultingDevice.interfaces)
    }
}