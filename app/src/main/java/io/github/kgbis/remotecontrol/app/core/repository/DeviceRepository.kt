package io.github.kgbis.remotecontrol.app.core.repository

import io.github.kgbis.remotecontrol.app.core.model.Device

interface DeviceRepository {

    fun loadDevices(): List<Device>

    suspend fun saveDevices(devices: List<Device>)
}