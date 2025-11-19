package com.example.remote.shutdown.network

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.util.Log
import com.example.remote.shutdown.util.Constants.DEFAULT_SUBNET
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.Inet4Address
import java.net.NetworkInterface

class NetworkRangeDetector(private val context: Context) {

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