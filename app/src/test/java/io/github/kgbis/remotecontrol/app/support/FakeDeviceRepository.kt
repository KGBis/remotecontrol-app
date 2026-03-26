package io.github.kgbis.remotecontrol.app.support

import io.github.kgbis.remotecontrol.app.core.model.Device
import io.github.kgbis.remotecontrol.app.core.repository.DeviceRepository

class FakeDeviceRepository : DeviceRepository {

    private var devices: List<Device> = listOf(
        DeviceFixtures.device("titan"),
        DeviceFixtures.device("caronte"),
        DeviceFixtures.device("dummy")
    )

    override fun loadDevices(): List<Device> = devices

    override suspend fun saveDevices(devices: List<Device>) {
        this.devices = devices
    }
}
