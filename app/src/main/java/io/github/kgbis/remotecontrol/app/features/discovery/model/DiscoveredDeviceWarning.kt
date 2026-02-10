package io.github.kgbis.remotecontrol.app.features.discovery.model

import androidx.annotation.StringRes

sealed interface DiscoveredDeviceWarning {

    data object None : DiscoveredDeviceWarning

    data class Outdated(
        @param:StringRes val reasonRes: Int,
        val param: String
    ) : DiscoveredDeviceWarning
}
