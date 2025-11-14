package com.example.remote.shutdown.network

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.Inet4Address
import java.net.NetworkInterface

class NetworkRangeDetector(private val context: Context) {

    /**
     * Obtiene el rango de red local automáticamente
     */
    suspend fun getLocalNetworkRange(): String? = withContext(Dispatchers.IO) {
        return@withContext try {
            // Intentar diferentes métodos, ordenados por fiabilidad
            val methods = listOf(
                { getNetworkRangeFromNetworkInterfaces() },
                { getNetworkRangeFromConnectivityManager() },
                /*{ getNetworkRangeFromCommonRanges() }*/
            )

            var theRange: String? = null
            for (method in methods) {
                Log.i("getLocalNetworkRange", "Detecting with $method")
                val range = method.invoke()
                if (range != null) {
                    theRange = range
                    break
                    // return@withContext range
                }
            }

            if(theRange != null && theRange.contains("10.0")) {
                Log.i("getLocalNetworkRange", "Detected local address range $theRange")
                theRange = "192.168.1"
            }

            theRange
        } catch (_: Exception) {
            null
        }
    }

    /**
     * Método 1: Usar ConnectivityManager (más fiable en Android)
     */
    private fun getNetworkRangeFromConnectivityManager(): String? {
        return try {
            val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            val activeNetwork = connectivityManager.activeNetwork ?: return null

            // Obtener información de la red
            val linkProperties = connectivityManager.getLinkProperties(activeNetwork)
            val networkCapabilities = connectivityManager.getNetworkCapabilities(activeNetwork)

            // Verificar que es WiFi o Ethernet (no móvil)
            if (networkCapabilities?.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) == true ||
                networkCapabilities?.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) == true) {

                // linkProperties?.linkAddresses?.forEach { address -> Log.i("link", "Having address ${address.address}") }

                linkProperties?.linkAddresses?.firstOrNull { address ->
                    address.address is Inet4Address && address.address.isSiteLocalAddress
                }?.let { linkAddress ->
                    extractNetworkRange(linkAddress.address.hostAddress)
                }
            } else {
                null
            }
        } catch (_: Exception) {
            null
        }
    }

    /**
     * Método 2: Examinar interfaces de red del sistema
     */
    private fun getNetworkRangeFromNetworkInterfaces(): String? {
        return try {
            NetworkInterface.getNetworkInterfaces()?.toList()?.forEach { networkInterface ->
                networkInterface.inetAddresses?.toList()?.forEach { inetAddress ->
                    if (inetAddress is Inet4Address &&
                        inetAddress.isSiteLocalAddress &&
                        !inetAddress.isLoopbackAddress) {

                        val range = extractNetworkRange(inetAddress.hostAddress)
                        if (range != null) return range
                    }
                }
            }
            null
        } catch (_: Exception) {
            null
        }
    }

    /**
     * Método 3: Rangos comunes como fallback
     */
    /*private suspend fun getNetworkRangeFromCommonRanges(): String? = withContext(Dispatchers.IO) {
        // Probar rangos comunes localmente
        val commonRanges = listOf("192.168.1", "192.168.0", "10.0.0", "172.16.0")

        for (range in commonRanges) {
            if (isNetworkRangeReachable(range)) {
                return@withContext range
            }
        }
        return@withContext null
    }*/

    /**
     * Extrae el prefijo de red de una IP (192.168.1.100 → 192.168.1)
     */
    private fun extractNetworkRange(ip: String): String? {
        return try {
            val parts = ip.split(".")
            if (parts.size == 4) {
                "${parts[0]}.${parts[1]}.${parts[2]}"
            } else {
                null
            }
        } catch (_: Exception) {
            null
        }
    }

    /**
     * Verifica si un rango de red está activo haciendo ping al router común
     */
    /*private suspend fun isNetworkRangeReachable(range: String): Boolean = withContext(Dispatchers.IO) {
        val commonRouterIPs = listOf("$range.1", "$range.254", "$range.100")

        return@withContext commonRouterIPs.any { routerIP ->
            try {
                Runtime.getRuntime().exec("ping -c 1 -W 1000 $routerIP").waitFor() == 0
            } catch (_: Exception) {
                false
            }
        }
    }*/

    /**
     * Obtiene la IP local del dispositivo
     */
    /*
    suspend fun getLocalIP(): String? = withContext(Dispatchers.IO) {
        return@withContext try {
            // Método 1: ConnectivityManager
            val connectivityManager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            val activeNetwork = connectivityManager.activeNetwork ?: return@withContext null

            connectivityManager.getLinkProperties(activeNetwork)?.linkAddresses?.firstOrNull { address ->
                address.address is Inet4Address && address.address.isSiteLocalAddress
            }?.address?.hostAddress

                ?: // Método 2: Network interfaces
                NetworkInterface.getNetworkInterfaces()?.toList()?.firstOrNull { networkInterface ->
                    networkInterface.isUp && !networkInterface.isLoopback
                }?.inetAddresses?.toList()?.firstOrNull { inetAddress ->
                    inetAddress is Inet4Address && inetAddress.isSiteLocalAddress
                }?.hostAddress
        } catch (e: Exception) {
            null
        }
    }
    */
}