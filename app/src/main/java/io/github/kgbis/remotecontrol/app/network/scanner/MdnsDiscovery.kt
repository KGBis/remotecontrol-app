package io.github.kgbis.remotecontrol.app.network.scanner

import android.content.Context
import android.net.nsd.NsdManager
import android.net.nsd.NsdServiceInfo
import android.util.Log
import io.github.kgbis.remotecontrol.app.data.Device

class MdnsDiscovery(
    private val context: Context,
    private val onServiceFound: (DiscoveredHost) -> Unit,
    private val onServiceLost: (String) -> Unit
) {

    private val nsdManager =
        context.getSystemService(Context.NSD_SERVICE) as NsdManager

    private val serviceType = "_rpcctl._tcp."

    private var discoveryListener: NsdManager.DiscoveryListener? = null

    fun start() {
        if (discoveryListener != null) return

        discoveryListener = object : NsdManager.DiscoveryListener {

            override fun onDiscoveryStarted(serviceType: String) {
                Log.d("mDNS", "Discovery started")
            }

            override fun onServiceFound(service: NsdServiceInfo) {
                // Android devuelve servicios incompletos → hay que resolver
                if (service.serviceType != serviceType) return

                resolveService(service)
            }

            override fun onServiceLost(service: NsdServiceInfo) {
                onServiceLost(service.serviceName)
            }

            override fun onDiscoveryStopped(serviceType: String) {
                Log.d("mDNS", "Discovery stopped")
            }

            override fun onStartDiscoveryFailed(serviceType: String, errorCode: Int) {
                Log.e("mDNS", "Start failed: $errorCode")
                stop()
            }

            override fun onStopDiscoveryFailed(serviceType: String, errorCode: Int) {
                Log.e("mDNS", "Stop failed: $errorCode")
                stop()
            }
        }

        nsdManager.discoverServices(
            serviceType,
            NsdManager.PROTOCOL_DNS_SD,
            discoveryListener
        )
    }

    fun stop() {
        discoveryListener?.let {
            nsdManager.stopServiceDiscovery(it)
        }
        discoveryListener = null
    }

    private fun resolveService(service: NsdServiceInfo) {
        nsdManager.resolveService(service, object : NsdManager.ResolveListener {

            override fun onServiceResolved(resolved: NsdServiceInfo) {
                val host = resolved.host ?: return
                val port = resolved.port
                val name = resolved.serviceName

                val txt = resolved.attributes.mapValues {
                    String(it.value, Charsets.UTF_8)
                }

                onServiceFound(
                    DiscoveredHost(
                        name = name,
                        host = host,
                        port = port,
                        txt = txt
                    )
                )
            }

            override fun onResolveFailed(serviceInfo: NsdServiceInfo, errorCode: Int) {
                Log.w("mDNS", "Resolve failed: $errorCode")
            }
        })
    }
}
