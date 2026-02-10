package io.github.kgbis.remotecontrol.app.features.discovery.model

import io.github.kgbis.remotecontrol.app.core.model.Device

sealed class DeviceTransformResult {

    interface WithDevice {
        val device: Device?
    }

    interface WithWarning {
        val warning: DiscoveredDeviceWarning
    }

    data class Ok(
        override val device: Device,
        override val warning: DiscoveredDeviceWarning = DiscoveredDeviceWarning.None
    ) : DeviceTransformResult(), WithDevice, WithWarning

    data class Outdated(
        val discovered: DiscoveredDevice,
        override val device: Device?,
        override val warning: DiscoveredDeviceWarning.Outdated
    ) : DeviceTransformResult(), WithDevice, WithWarning

    data class Invalid(
        val discovered: DiscoveredDevice,
        override val warning: DiscoveredDeviceWarning.Outdated
    ) : DeviceTransformResult(), WithWarning
}