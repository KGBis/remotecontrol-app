/*
 * Remote PC Control
 * Copyright (C) 2026 Enrique García (https://github.com/KGBis)
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 */
package io.github.kgbis.remotecontrol.app.core.network

import android.content.Context
import android.net.ConnectivityManager
import android.net.Network
import android.net.NetworkCapabilities
import android.net.NetworkCapabilities.NET_CAPABILITY_VALIDATED
import android.net.NetworkCapabilities.TRANSPORT_WIFI
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.flow.stateIn
import java.net.Inet4Address

class NetworkMonitorImpl(
    context: Context,
    scope: CoroutineScope,
    private val networkRangeDetector: NetworkRangeDetector
): NetworkMonitor {

    private val connectivityManager =
        context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

    private val manualRefresh = MutableSharedFlow<Unit>(extraBufferCapacity = 1)


    @OptIn(FlowPreview::class)
    override val networkInfo: StateFlow<NetworkInfo> =
        merge(
            callbackFlow {
                val callback = object : ConnectivityManager.NetworkCallback() {
                    override fun onAvailable(network: Network) {
                        trySend(Unit)
                    }

                    override fun onLost(network: Network) {
                        trySend(Unit)
                    }

                    override fun onCapabilitiesChanged(
                        network: Network,
                        networkCapabilities: NetworkCapabilities
                    ) {
                        trySend(Unit)
                    }
                }

                connectivityManager.registerDefaultNetworkCallback(callback)

                // estado inicial
                trySend(Unit)

                awaitClose {
                    connectivityManager.unregisterNetworkCallback(callback)
                }
            },
            manualRefresh
        ).map { computeNetworkInfo() }
            .debounce { info ->
                when (info) {
                    is NetworkInfo.Connecting -> 800L
                    is NetworkInfo.Disconnected -> 300L
                    is NetworkInfo.Local -> 200L
                }
            }.distinctUntilChanged()
            .stateIn(
                scope,
                SharingStarted.Eagerly,
                NetworkInfo.Disconnected
            )

    override fun refresh() {
        manualRefresh.tryEmit(Unit)
    }

    private fun computeNetworkInfo(): NetworkInfo {
        // network & capabilities or disconnected
        val network = connectivityManager.activeNetwork ?: return NetworkInfo.Disconnected
        val caps =
            connectivityManager.getNetworkCapabilities(network) ?: return NetworkInfo.Disconnected

        return when {
            !caps.hasTransport(TRANSPORT_WIFI) -> NetworkInfo.Disconnected
            caps.hasCapability(NET_CAPABILITY_VALIDATED) -> computeSubnet(network)
            else -> NetworkInfo.Connecting  // twilight zone
        }
    }

    private fun computeSubnet(network: Network): NetworkInfo {
        val linkProps = connectivityManager.getLinkProperties(network)

        val ipv4 = linkProps
            ?.linkAddresses
            ?.map { it.address }
            ?.filterIsInstance<Inet4Address>()
            ?.firstOrNull { !it.isLoopbackAddress }
            ?: return NetworkInfo.Disconnected

        val subnet = ipv4.hostAddress?.substringBeforeLast(".")

        val effectiveSubnet =
            if (networkRangeDetector.isEmulator())
                networkRangeDetector.getScanSubnet()
            else
                subnet

        // here we have a value, so no problem with !!
        return NetworkInfo.Local(effectiveSubnet!!)
    }
}
