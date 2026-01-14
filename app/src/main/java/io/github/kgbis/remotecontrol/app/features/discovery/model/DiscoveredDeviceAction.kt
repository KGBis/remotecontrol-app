package io.github.kgbis.remotecontrol.app.features.discovery.model

sealed interface DiscoveredDeviceAction {
    data object Click : DiscoveredDeviceAction
    data object ToggleSelection : DiscoveredDeviceAction
    data object StartMultiSelect : DiscoveredDeviceAction
}