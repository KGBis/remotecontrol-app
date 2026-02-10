package io.github.kgbis.remotecontrol.app.core.network

sealed interface NetworkInfo {
    object Disconnected : NetworkInfo
    object Connecting : NetworkInfo // The "Twilight Zone"
    data class Local(val subnet: String) : NetworkInfo
}
