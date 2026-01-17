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
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.stateIn
import java.net.Inet4Address

class NetworkMonitor(
    context: Context,
    scope: CoroutineScope,
    private val networkRangeDetector: NetworkRangeDetector
) {

    private val connectivityManager =
        context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager

    @OptIn(FlowPreview::class)
    val networkInfo: StateFlow<NetworkInfo> =
        callbackFlow {
            val callback = object : ConnectivityManager.NetworkCallback() {

                override fun onAvailable(network: Network) = emit()
                override fun onLost(network: Network) = emit()
                override fun onCapabilitiesChanged(
                    network: Network,
                    networkCapabilities: NetworkCapabilities
                ) = emit()

                private fun emit() {
                    trySend(computeNetworkInfo())
                }
            }

            connectivityManager.registerDefaultNetworkCallback(callback)

            // initial state
            trySend(computeNetworkInfo())

            awaitClose {
                connectivityManager.unregisterNetworkCallback(callback)
            }
        }.debounce { info ->
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
