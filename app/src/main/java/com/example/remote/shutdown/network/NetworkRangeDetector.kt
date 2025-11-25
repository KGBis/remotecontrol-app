package com.example.remote.shutdown.network

import android.os.Build
import android.util.Log
import java.net.Inet4Address
import java.net.NetworkInterface

class NetworkRangeDetector() {

    fun getScanSubnet(): String {
        return if (isEmulator()) {
            // En emulador, usar subred por defecto
            "192.168.1"
        } else {
            // En dispositivo real, usar la subred real
            val subnet = getLocalSubnet()
            Log.i("getScanSubnet", subnet)
            return subnet
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

                // Ignore loopback and non-active interfaces
                if (networkInterface.isLoopback || !networkInterface.isUp) continue

                val addresses = networkInterface.inetAddresses
                while (addresses.hasMoreElements()) {
                    val address = addresses.nextElement()

                    // Filter by private IPv4 addresses
                    if (!address.isLoopbackAddress &&
                        address is Inet4Address &&
                        isPrivateIP(address.hostAddress!!)) {

                        return extractSubnet(address.hostAddress!!)
                    }
                }
            }
            // Fallback if local IP is not found
            "192.168.1"
        } catch (e: Exception) {
            Log.e("getLocalSubnet", "Exception $e")
            "0.0.0" // Fallback if error
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
            ip.startsWith("192.168.") -> {
                Log.i("extractSubnet", "ip: $ip -> subnet: ${ip.take(ip.lastIndexOf('.'))}")
                ip.take(ip.lastIndexOf('.'))
            }
            ip.startsWith("10.") -> {
                val parts = ip.split('.')
                if (parts.size >= 2) "${parts[0]}.${parts[1]}" else "10.0"
            }
            ip.startsWith("172.") -> {
                val parts = ip.split('.')
                if (parts.size >= 2) "${parts[0]}.${parts[1]}" else "172.16"
            }
            else -> "0.0.0"
        }
    }

}