package io.github.kgbis.remotecontrol.app.support

import io.github.kgbis.remotecontrol.app.core.network.NetworkInfo
import io.github.kgbis.remotecontrol.app.core.network.NetworkMonitor
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

class FakeNetworkMonitor : NetworkMonitor {

    private val _networkInfo = MutableStateFlow<NetworkInfo>(NetworkInfo.Disconnected)
    override val networkInfo: StateFlow<NetworkInfo> = _networkInfo

    var refreshCalls = 0
        private set

    override fun refresh() {
        refreshCalls++
    }

    fun setNetworkInfo(info: NetworkInfo) {
        _networkInfo.value = info
    }
}


