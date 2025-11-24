package com.example.remote.shutdown.network

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import android.util.Log
import com.example.remote.shutdown.util.Constants.DEFAULT_SUBNET
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.Inet4Address
import java.net.NetworkInterface

class NetworkRangeDetector(private val context: Context) {

    fun getScanSubnet(): String {
        return if (isEmulator()) {
            // En emulador, usar subred por defecto
            "192.168.1"
        } else {
            // En dispositivo real, usar la subred real
            getLocalSubnet()
        }
    }

    fun isEmulator(): Boolean {
        return (Build.BRAND.startsWith("generic") && Build.DEVICE.startsWith("generic")) ||
                Build.FINGERPRINT.startsWith("generic") ||
                Build.FINGERPRINT.startsWith("unknown") ||
                Build.HARDWARE.contains("goldfish") ||
                Build.HARDWARE.contains("ranchu") ||
                Build.MODEL.contains("google_sdk") ||
                Build.MODEL.contains("Emulator") ||
                Build.MODEL.contains("Android SDK built for") ||
                Build.PRODUCT.contains("sdk_google") ||
                Build.PRODUCT.contains("emulator") ||
                Build.PRODUCT.contains("sdk")
    }

    fun getLocalSubnet(): String {
        return try {
            val networkInterfaces = NetworkInterface.getNetworkInterfaces()
            while (networkInterfaces.hasMoreElements()) {
                val networkInterface = networkInterfaces.nextElement()

                // Ignorar interfaces loopback y no activas
                if (networkInterface.isLoopback || !networkInterface.isUp) continue

                val addresses = networkInterface.inetAddresses
                while (addresses.hasMoreElements()) {
                    val address = addresses.nextElement()

                    // Filtrar solo IPv4 privadas
                    if (!address.isLoopbackAddress &&
                        address is Inet4Address &&
                        isPrivateIP(address.hostAddress!!)) {

                        return extractSubnet(address.hostAddress!!)
                    }
                }
            }
            // Fallback si no encuentra IP local
            "192.168.1"
        } catch (e: Exception) {
            Log.w("getLocalSubnet", "Exception $e")
            "192.168.1" // Fallback por defecto
        }
    }

    private fun isPrivateIP(ip: String): Boolean {
        return ip.startsWith("192.168.") ||
                ip.startsWith("10.") ||
                ip.startsWith("172.16.") ||
                ip.startsWith("172.17.") ||
                ip.startsWith("172.18.") ||
                ip.startsWith("172.19.") ||
                ip.startsWith("172.20.") ||
                ip.startsWith("172.21.") ||
                ip.startsWith("172.22.") ||
                ip.startsWith("172.23.") ||
                ip.startsWith("172.24.") ||
                ip.startsWith("172.25.") ||
                ip.startsWith("172.26.") ||
                ip.startsWith("172.27.") ||
                ip.startsWith("172.28.") ||
                ip.startsWith("172.29.") ||
                ip.startsWith("172.30.") ||
                ip.startsWith("172.31.")
    }

    private fun extractSubnet(ip: String): String {
        return when {
            ip.startsWith("192.168.") -> ip.substring(0, ip.lastIndexOf('.'))
            ip.startsWith("10.") -> {
                val parts = ip.split('.')
                if (parts.size >= 2) "${parts[0]}.${parts[1]}" else "10.0"
            }
            ip.startsWith("172.") -> {
                val parts = ip.split('.')
                if (parts.size >= 2) "${parts[0]}.${parts[1]}" else "172.16"
            }
            else -> "192.168.1"
        }
    }

    /**
     * Get local network range. It tries by two different methods:
     * 1. from NetworkInterfaces
     * 2. from ConnectivityManager as fallback
     * @return a String like _10.0.1_ without the last _.xxx_ part or null
     */
    suspend fun getLocalNetworkRange(): String? = withContext(Dispatchers.IO) {
        return@withContext try {
            val methods = listOf(
                { getNetworkRangeFromNetworkInterfaces() },
                { getNetworkRangeFromConnectivityManager() }
            )

            var theRange: String? = null
            for (method in methods) {
                Log.i("getLocalNetworkRange", "Detecting with $method")
                val range = method.invoke()
                if (range != null) {
                    theRange = range
                    break
                }
            }

            if (theRange != null && theRange.startsWith("10.0")) {
                Log.i("getLocalNetworkRange", "Detected local address range $theRange")
                theRange = DEFAULT_SUBNET
            }

            // return network range
            theRange
        } catch (_: Exception) {
            null
        }
    }

    /**
     * Network range using ConnectivityManager (most reliable in Android)
     */
    private fun getNetworkRangeFromConnectivityManager(): String? {
        return try {
            val connectivityManager =
                context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            val activeNetwork = connectivityManager.activeNetwork ?: return null

            // Obtener información de la red
            val linkProperties = connectivityManager.getLinkProperties(activeNetwork)
            val networkCapabilities = connectivityManager.getNetworkCapabilities(activeNetwork)

            // Verificar que es WiFi o Ethernet (no móvil)
            if (networkCapabilities?.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) == true ||
                networkCapabilities?.hasTransport(NetworkCapabilities.TRANSPORT_ETHERNET) == true
            ) {
                linkProperties?.linkAddresses?.firstOrNull { address ->
                    address.address is Inet4Address && address.address.isSiteLocalAddress
                }?.let { linkAddress ->
                    extractNetworkRange(linkAddress.address.hostAddress!!)
                }
            } else {
                null
            }
        } catch (_: Exception) {
            null
        }
    }

    /**
     * Netowrk range by checking network interfaces
     */
    private fun getNetworkRangeFromNetworkInterfaces(): String? {
        return try {
            NetworkInterface.getNetworkInterfaces()?.toList()?.forEach { networkInterface ->
                networkInterface.inetAddresses?.toList()?.forEach { inetAddress ->
                    if (inetAddress is Inet4Address &&
                        inetAddress.isSiteLocalAddress &&
                        !inetAddress.isLoopbackAddress
                    ) {
                        val range = extractNetworkRange(inetAddress.hostAddress!!)
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
     * gets the IP number up to the third octet (i.e. 192.168.1.100 → 192.168.1)
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
}