package io.github.kgbis.remotecontrol.app.features.domain

import android.util.Log
import io.github.kgbis.remotecontrol.app.core.model.Device
import io.github.kgbis.remotecontrol.app.features.devices.model.DeviceFormState
import io.github.kgbis.remotecontrol.app.features.devices.model.InterfaceFormState
import io.github.kgbis.remotecontrol.app.utils.DeviceFixtures
import io.mockk.every
import io.mockk.mockkStatic
import org.junit.jupiter.api.Assertions.assertEquals
import org.junit.jupiter.api.Assertions.assertTrue
import org.junit.jupiter.api.BeforeEach
import org.junit.jupiter.api.Test
import java.util.UUID

class DeviceConflictCheckerTest {

    @BeforeEach
    fun setup() {
        mockkStatic(Log::class)
        every { Log.d(any(), any()) } returns 0
        every { Log.i(any(), any()) } returns 0
        every { Log.w(any(String::class), any(String::class)) } returns 0
        every { Log.e(any(), any()) } returns 0
    }


    @Test
    fun `add a new device with non conflicting IP returns ConflictResult None`() {
        val devices = deviceList()
        val formData = DeviceFormState(
            hostname = "Dummy-1",
            interfaces = listOf(InterfaceFormState(ip = "192.168.1.44"))
        )

        val conflictChecker = DeviceConflictChecker(devices)
        val result = conflictChecker.check(formData, null)

        assertEquals(result, ConflictResult.None)
    }

    @Test
    fun `edit a device with non conflicting MAC returns ConflictResult None`() {
        val devices = deviceList()
        val formData = DeviceFormState(
            id = UUID.fromString("eeaf6b22-6878-427d-8520-6a1c1544a483"),
            hostname = "Dummy-1",
            interfaces = listOf(InterfaceFormState(ip = "192.168.1.40", mac = "c4:17:fe:bd:00:1d"))
        )

        val conflictChecker = DeviceConflictChecker(devices)
        val result = conflictChecker.check(formData, formData.id)

        assertEquals(result, ConflictResult.None)
    }



    @Test
    fun `add a new device with an already used IP returns ConflictResult IpConflict`() {
        val devices = deviceList()
        val formData = DeviceFormState(
            hostname = "Dummy-1",
            interfaces = listOf(InterfaceFormState(ip = "192.168.1.43")) // Titan's IP
        )

        val conflictChecker = DeviceConflictChecker(devices)
        val result = conflictChecker.check(formData, null)

        assertTrue(result is ConflictResult.IpConflict)
        assertTrue((result as ConflictResult.IpConflict).device.hostname == "Titan")
    }

    @Test
    fun `edit a device with an already used IP returns ConflictResult IpConflict`() {
        val devices = deviceList()
        val formData = DeviceFormState(
            id = UUID.fromString("eeaf6b22-6878-427d-8520-6a1c1544a483"),
            hostname = "Dummy-1",
            interfaces = listOf(
                InterfaceFormState(
                    ip = "192.168.1.43", // Titan's IP
                    mac = "c4:17:fe:bd:00:1d"
                )
            )
        )

        val conflictChecker = DeviceConflictChecker(devices)
        val result = conflictChecker.check(formData, formData.id)

        assertTrue(result is ConflictResult.IpConflict)
        assertTrue((result as ConflictResult.IpConflict).device.hostname == "Titan")
    }

    @Test
    fun `add a new device with an already used MAC returns ConflictResult MacConflict`() {
        val devices = deviceList()
        val formData = DeviceFormState(
            hostname = "Dummy-1",
            interfaces = listOf(
                InterfaceFormState(
                    ip = "192.168.1.48",
                    mac = "c4:17:fe:bd:f2:5d" // Caronte's MAC
                )
            )
        )

        val conflictChecker = DeviceConflictChecker(devices)
        val result = conflictChecker.check(formData, null)

        assertTrue(result is ConflictResult.MacConflict)
        assertTrue((result as ConflictResult.MacConflict).device.hostname == "Caronte")
    }

    @Test
    fun `edit a device with an already used MAC returns ConflictResult MacConflict`() {
        val devices = deviceList()
        val formData = DeviceFormState(
            id = UUID.fromString("eeaf6b22-6878-427d-8520-6a1c1544a483"),
            hostname = "Dummy-1",
            interfaces = listOf(
                InterfaceFormState(
                    ip = "192.168.1.48",
                    mac = "c4:17:fe:bd:f2:5d" // Caronte's MAC
                )
            )
        )

        val conflictChecker = DeviceConflictChecker(devices)
        val result = conflictChecker.check(formData, formData.id)

        assertTrue(result is ConflictResult.MacConflict)
        assertTrue((result as ConflictResult.MacConflict).device.hostname == "Caronte")
    }

    @Test
    fun `add a new device with an already used MAC and IP returns ConflictResult PossibleDuplicate`() {
        val devices = deviceList()
        val formData = DeviceFormState(
            hostname = "Dummy-1",
            interfaces = listOf(
                InterfaceFormState(
                    ip = "192.168.1.45", // Caronte's IP
                    mac = "c4:17:fe:bd:f2:5d" // Caronte's MAC
                )
            )
        )

        val conflictChecker = DeviceConflictChecker(devices)
        val result = conflictChecker.check(formData, null)

        assertTrue(result is ConflictResult.PossibleDuplicate)
        assertTrue((result as ConflictResult.PossibleDuplicate).device.hostname == "Caronte")
    }

    @Test
    fun `edit a device with an already used MAC and IP returns ConflictResult PossibleDuplicate`() {
        val devices = deviceList()
        val formData = DeviceFormState(
            id = UUID.fromString("eeaf6b22-6878-427d-8520-6a1c1544a483"),
            hostname = "Dummy-1",
            interfaces = listOf(
                InterfaceFormState(
                    ip = "192.168.1.45", // Caronte's IP
                    mac = "c4:17:fe:bd:f2:5d" // Caronte's MAC
                )
            )
        )

        val conflictChecker = DeviceConflictChecker(devices)
        val result = conflictChecker.check(formData, formData.id)

        assertTrue(result is ConflictResult.PossibleDuplicate)
        assertTrue((result as ConflictResult.PossibleDuplicate).device.hostname == "Caronte")
    }

    @Test
    fun `edit a device with an already used MAC and IP from different PCs returns ConflictResult MacConflict`() {
        val devices = deviceList()
        val formData = DeviceFormState(
            id = UUID.fromString("eeaf6b22-6878-427d-8520-6a1c1544a483"),
            hostname = "Dummy-1",
            interfaces = listOf(
                InterfaceFormState(
                    ip = "192.168.1.43", // Titan's IP
                    mac = "c4:17:fe:bd:f2:5d" // Caronte's MAC
                )
            )
        )

        val conflictChecker = DeviceConflictChecker(devices)
        val result = conflictChecker.check(formData, formData.id)

        assertTrue(result is ConflictResult.MacConflict)
        assertTrue((result as ConflictResult.MacConflict).device.hostname == "Caronte")
    }

    private fun deviceList(): List<Device> {
        return listOf(
            DeviceFixtures.device("titan"),
            DeviceFixtures.device("caronte"),
            DeviceFixtures.device("dummy")
        )
    }
}