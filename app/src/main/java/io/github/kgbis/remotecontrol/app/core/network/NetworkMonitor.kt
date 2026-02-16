package io.github.kgbis.remotecontrol.app.core.network

import kotlinx.coroutines.flow.StateFlow

interface NetworkMonitor {

    val networkInfo: StateFlow<NetworkInfo>

    fun refresh()
}