package io.github.kgbis.remotecontrol.app.features.discovery.mdns

import android.content.Context
import android.net.nsd.NsdManager
import android.net.nsd.NsdServiceInfo
import android.os.Build
import android.os.ext.SdkExtensions
import android.util.Log
import androidx.annotation.RequiresExtension
import java.net.Inet4Address

class MDNSDiscovery(context: Context) {

    companion object {
        private const val TAG = "MDNSDiscovery"

        const val SERVICE_REMOTECONTROL = "_rpcctl._tcp"
    }

    private var nsdManager: NsdManager = context.getSystemService(Context.NSD_SERVICE) as NsdManager
    private val discoveredServices = mutableListOf<DiscoveredService>()

    // Callbacks para resultados
    interface DiscoveryListener {
        fun onServiceFound(service: DiscoveredService)
        fun onServiceLost(serviceName: String)
        fun onDiscoveryStarted()
        fun onDiscoveryStopped()
        fun onError(message: String)
    }

    data class DiscoveredService(
        val name: String,
        val type: String,
        val host: String,
        val port: Int,
        val txtRecords: Map<String, String> = emptyMap()
    )

    // Discovering Listener
    private val discoveryListener = object : NsdManager.DiscoveryListener {
        override fun onStartDiscoveryFailed(serviceType: String, errorCode: Int) {
            Log.e(TAG, "Error al iniciar discovery: $errorCode")
            listener?.onError("Error al iniciar: $errorCode")
            nsdManager.stopServiceDiscovery(this)
        }

        override fun onStopDiscoveryFailed(serviceType: String, errorCode: Int) {
            Log.e(TAG, "Error al detener discovery: $errorCode")
            listener?.onError("Error al detener: $errorCode")
        }

        override fun onDiscoveryStarted(serviceType: String) {
            Log.d(TAG, "Discovery iniciado para: $serviceType")
            listener?.onDiscoveryStarted()
        }

        override fun onDiscoveryStopped(serviceType: String) {
            Log.d(TAG, "Discovery detenido para: $serviceType")
            listener?.onDiscoveryStopped()
        }

        override fun onServiceFound(serviceInfo: NsdServiceInfo) {
            Log.d(TAG, "Service encontrado: ${serviceInfo.serviceName}")

            // Resolve to get details
            // Using resolveService intentionally: discovery snapshot only.
            @Suppress("DEPRECATION")
            nsdManager.resolveService(serviceInfo, object : NsdManager.ResolveListener {
                override fun onResolveFailed(serviceInfo: NsdServiceInfo, errorCode: Int) {
                    Log.e(TAG, "Error resolving ${serviceInfo.serviceName}: $errorCode")
                }

                @RequiresExtension(extension = Build.VERSION_CODES.TIRAMISU, version = 7)
                override fun onServiceResolved(serviceInfo: NsdServiceInfo) {
                    val discovered = DiscoveredService(
                        name = serviceInfo.serviceName,
                        type = serviceInfo.serviceType,
                        host = serviceInfo.safeHostAddress(),
                        port = serviceInfo.port,
                        txtRecords = parseTxtRecords(serviceInfo)
                    )

                    Log.d("onServiceResolved", "Resolved to $discovered")

                    // Filtrar duplicados
                    if (!discoveredServices.any { it.host == discovered.host && it.port == discovered.port }) {
                        discoveredServices.add(discovered)
                        listener?.onServiceFound(discovered)

                        Log.i(TAG, "✅ Resolved: ${discovered.name} " +"(${discovered.host}:${discovered.port}) " +"Type: ${discovered.type}")
                    }
                }
            })
        }

        override fun onServiceLost(serviceInfo: NsdServiceInfo) {
            Log.d(TAG, "Service lost: ${serviceInfo.serviceName}")
            discoveredServices.removeAll { it.name == serviceInfo.serviceName }
            listener?.onServiceLost(serviceInfo.serviceName)
        }
    }

    private var listener: DiscoveryListener? = null

    fun setDiscoveryListener(listener: DiscoveryListener) {
        this.listener = listener
    }

    /**
     * Inicia el descubrimiento de servicios mDNS
     * @param serviceType Tipo de servicio a descubrir (ej: "_http._tcp")
     */
    fun startDiscovery(serviceType: String = SERVICE_REMOTECONTROL) {
        discoveredServices.clear()

        try {
            nsdManager.discoverServices(
                serviceType,
                NsdManager.PROTOCOL_DNS_SD,
                discoveryListener
            )
            Log.d(TAG, "Buscando servicios: $serviceType")
        } catch (e: Exception) {
            Log.e(TAG, "Error iniciando discovery", e)
            listener?.onError(e.message ?: "Error desconocido")
        }
    }

    /**
     * Stops discovery service
     */
    fun stopDiscovery() {
        try {
            nsdManager.stopServiceDiscovery(discoveryListener)
            Log.d(TAG, "Discovery stopped")
        } catch (e: Exception) {
            Log.d(TAG, "Error stopping discovery: ${e.message}")
        }
    }


    /**
     * Parsea los registros TXT del servicio
     */
    private fun parseTxtRecords(serviceInfo: NsdServiceInfo): Map<String, String> {
        val records = mutableMapOf<String, String>()

        try {
            val attributes = serviceInfo.attributes
            attributes?.let { attr ->
                attr.keys.forEach { key ->
                    val value = attr[key]
                    records[key] = value?.toString(Charsets.UTF_8) ?: ""
                }
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error parseando TXT records", e)
        }

        return records
    }

    @RequiresExtension(extension = Build.VERSION_CODES.TIRAMISU, version = 7)
    fun NsdServiceInfo.safeHostAddress(): String {
        val nsdExtVersion = SdkExtensions.getExtensionVersion(Build.VERSION_CODES.TIRAMISU)
        return if (nsdExtVersion >= 7) {
            hostAddresses.firstOrNull()?.hostAddress
        } else {
            @Suppress("DEPRECATION")
            host?.hostAddress
        } ?: "Unknown"
    }
}