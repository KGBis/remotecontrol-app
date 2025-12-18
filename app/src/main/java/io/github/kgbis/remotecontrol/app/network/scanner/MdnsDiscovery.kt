package io.github.kgbis.remotecontrol.app.network.scanner

import android.content.Context
import android.net.nsd.NsdManager
import android.net.nsd.NsdServiceInfo
import android.util.Log

class MDNSDiscovery(context: Context) {

    companion object {
        private const val TAG = "MDNSDiscovery"

        // Tipos de servicio comunes
        const val SERVICE_REMOTECONTROL = "_rpcctl._tcp"
        const val SERVICE_HTTP = "_http._tcp"
        const val SERVICE_HTTPS = "_https._tcp"
        const val SERVICE_SSH = "_ssh._tcp"
        const val SERVICE_VNC = "_rfb._tcp"  // VNC/RFB
        const val SERVICE_PRINTER = "_ipp._tcp"
        const val SERVICE_SMB = "_smb._tcp"
        const val SERVICE_AIRPLAY = "_airplay._tcp"
        const val SERVICE_CHROMECAST = "_googlecast._tcp"

        // Para descubrimiento genérico (todos los servicios)
        const val SERVICE_ALL = "_services._dns-sd._udp"
    }

    private var nsdManager: NsdManager
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

    // Listener para el descubrimiento
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
            Log.d(TAG, "Servicio encontrado: ${serviceInfo.serviceName}")

            // Resolver el servicio para obtener detalles
            nsdManager.resolveService(serviceInfo, object : NsdManager.ResolveListener {
                override fun onResolveFailed(serviceInfo: NsdServiceInfo, errorCode: Int) {
                    Log.e(TAG, "Error resolviendo ${serviceInfo.serviceName}: $errorCode")
                }

                override fun onServiceResolved(serviceInfo: NsdServiceInfo) {
                    val discovered = DiscoveredService(
                        name = serviceInfo.serviceName,
                        type = serviceInfo.serviceType,
                        host = serviceInfo.host?.hostAddress ?: "Unknown",
                        port = serviceInfo.port,
                        txtRecords = parseTxtRecords(serviceInfo)
                    )

                    // Filtrar duplicados
                    if (!discoveredServices.any { it.host == discovered.host && it.port == discovered.port }) {
                        discoveredServices.add(discovered)
                        listener?.onServiceFound(discovered)

                        Log.i(TAG, "✅ Resuelto: ${discovered.name} " +
                                "(${discovered.host}:${discovered.port}) " +
                                "Tipo: ${discovered.type}")
                    }
                }
            })
        }

        override fun onServiceLost(serviceInfo: NsdServiceInfo) {
            Log.d(TAG, "Servicio perdido: ${serviceInfo.serviceName}")
            discoveredServices.removeAll { it.name == serviceInfo.serviceName }
            listener?.onServiceLost(serviceInfo.serviceName)
        }
    }

    private var listener: DiscoveryListener? = null

    init {
        nsdManager = context.getSystemService(Context.NSD_SERVICE) as NsdManager
    }

    fun setDiscoveryListener(listener: DiscoveryListener) {
        this.listener = listener
    }

    /**
     * Inicia el descubrimiento de servicios mDNS
     * @param serviceType Tipo de servicio a descubrir (ej: "_http._tcp")
     * @param protocol Protocolo a usar (NsdManager.PROTOCOL_DNS_SD para mDNS)
     */
    fun startDiscovery(serviceType: String = SERVICE_ALL) {
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
     * Inicia descubrimiento de múltiples tipos de servicios
     */
    fun startMultiServiceDiscovery(serviceTypes: List<String>) {
        discoveredServices.clear()

        serviceTypes.forEach { serviceType ->
            try {
                nsdManager.discoverServices(
                    serviceType,
                    NsdManager.PROTOCOL_DNS_SD,
                    discoveryListener
                )
                Log.d(TAG, "Buscando: $serviceType")
            } catch (e: Exception) {
                Log.e(TAG, "Error buscando $serviceType", e)
            }
        }
    }

    /**
     * Detiene el descubrimiento de servicios
     */
    fun stopDiscovery() {
        try {
            nsdManager.stopServiceDiscovery(discoveryListener)
            Log.d(TAG, "Discovery detenido")
        } catch (e: Exception) {
            Log.e(TAG, "Error deteniendo discovery", e)
        }
    }

    /**
     * Obtiene todos los servicios descubiertos
     */
    fun getDiscoveredServices(): List<DiscoveredService> {
        return discoveredServices.toList()
    }

    /**
     * Filtra servicios por tipo
     */
    fun filterByType(type: String): List<DiscoveredService> {
        return discoveredServices.filter { it.type.contains(type, ignoreCase = true) }
    }

    /**
     * Filtra servicios por nombre/host
     */
    fun filterByHost(hostname: String): List<DiscoveredService> {
        return discoveredServices.filter {
            it.name.contains(hostname, ignoreCase = true) ||
                    it.host.contains(hostname, ignoreCase = true)
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

    /**
     * Registra un servicio local (para testing)
     */
    fun registerLocalService(serviceName: String, port: Int, serviceType: String = SERVICE_REMOTECONTROL) {
        val serviceInfo = NsdServiceInfo().apply {
            this.serviceName = serviceName
            this.serviceType = serviceType
            this.port = port

            // Agregar atributos TXT opcionales
            val attributes = mapOf(
                "version" to "1.0",
                "os" to "Android",
                "app" to "RemoteControl"
            )

            this.setAttribute("version", "1.0")
            this.setAttribute("os", "Android")
        }

        nsdManager.registerService(
            serviceInfo,
            NsdManager.PROTOCOL_DNS_SD,
            object : NsdManager.RegistrationListener {
                override fun onRegistrationFailed(serviceInfo: NsdServiceInfo, errorCode: Int) {
                    Log.e(TAG, "Registro fallido: $errorCode")
                }

                override fun onUnregistrationFailed(serviceInfo: NsdServiceInfo, errorCode: Int) {
                    Log.e(TAG, "Desregistro fallido: $errorCode")
                }

                override fun onServiceRegistered(serviceInfo: NsdServiceInfo) {
                    Log.d(TAG, "Servicio registrado: ${serviceInfo.serviceName}")
                }

                override fun onServiceUnregistered(serviceInfo: NsdServiceInfo) {
                    Log.d(TAG, "Servicio desregistrado: ${serviceInfo.serviceName}")
                }
            }
        )
    }
}