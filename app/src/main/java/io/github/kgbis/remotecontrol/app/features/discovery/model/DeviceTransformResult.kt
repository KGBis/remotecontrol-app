package io.github.kgbis.remotecontrol.app.features.discovery.model

import io.github.kgbis.remotecontrol.app.core.model.Device

sealed class DeviceTransformResult {

    data class Ok(
        val device: Device
    ) : DeviceTransformResult()

    data class Outdated(
        val discovered: DiscoveredDevice,
        val reason: Int,
        val reasonText: String,
    ) : DeviceTransformResult()

    data class Invalid(
        val discovered: DiscoveredDevice,
        val reason: Int,
        val reasonText: String,
    ) : DeviceTransformResult()
}