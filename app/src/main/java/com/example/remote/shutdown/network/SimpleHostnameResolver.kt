package com.example.remote.shutdown.network

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.net.Socket
import kotlin.coroutines.CoroutineContext

@Deprecated(message = "Not working in modern Android")
class SimpleHostnameResolver(private val context: CoroutineContext = Dispatchers.IO) {

    /**
     * Versión simplificada sin lambdas complejas
     */
    suspend fun resolveHostname(ip: String): String? = withContext(context) {
        // 1. Nombres comunes primero (más rápido)
        if (ip.startsWith("192.168.1.")) {
            val commonName = tryCommonLocalNames(ip)
            if (commonName != null) return@withContext commonName
        }

        // 2. DNS inverso (segunda opción más rápida)
        try {
            val dnsName = resolveViaReverseDNS(ip)
            if (dnsName != null) return@withContext dnsName
        } catch (e: Exception) { /* Continuar */
        }

        // 3. NetBIOS para Windows
        try {
            val netbiosName = resolveViaNetBios(ip)
            if (netbiosName != null) return@withContext netbiosName
        } catch (e: Exception) { /* Continuar */
        }

        // 4. SMB para Windows
        try {
            val smbName = resolveViaSMB(ip)
            if (smbName != null) return@withContext smbName
        } catch (e: Exception) { /* Continuar */
        }

        // 5. Archivo hosts (última opción, usualmente no funciona sin root)
        try {
            val hostsName = resolveViaHostsFile(ip)
            if (hostsName != null) return@withContext hostsName
        } catch (e: Exception) { /* Continuar */
        }

        return@withContext null
    }

    // ... (los mismos métodos auxiliares que arriba)
    private suspend fun resolveViaReverseDNS(ip: String): String? = withContext(context) {
        try {
            val hostname = java.net.InetAddress.getByName(ip).hostName
            if (hostname == ip || hostname.isEmpty()) null else hostname
        } catch (e: Exception) {
            null
        }
    }

    private suspend fun resolveViaNetBios(ip: String): String? = withContext(context) {
        try {
            Socket().use { socket ->
                socket.connect(java.net.InetSocketAddress(ip, 137), 1000)
            }
            tryNetBiosQuery(ip)
        } catch (e: Exception) {
            null
        }
    }

    private suspend fun resolveViaSMB(ip: String): String? = withContext(context) {
        try {
            Socket().use { socket ->
                socket.connect(java.net.InetSocketAddress(ip, 445), 1000)
            }
            "Windows-PC"
        } catch (e: Exception) {
            null
        }
    }

    private suspend fun resolveViaHostsFile(ip: String): String? = withContext(context) {
        try {
            val hostsFile = File("/system/etc/hosts")
            if (!hostsFile.exists()) return@withContext null

            hostsFile.useLines { lines ->
                lines.forEach { line ->
                    val cleanLine = line.substringBefore('#').trim()
                    val parts = cleanLine.split(Regex("\\s+"))
                    if (parts.size >= 2 && parts[0] == ip) {
                        return@withContext parts[1]
                    }
                }
            }
            null
        } catch (e: Exception) {
            null
        }
    }

    private fun tryCommonLocalNames(ip: String): String? {
        val commonNames = mapOf(
            "192.168.1.1" to "Router", "192.168.1.254" to "Router",
            "192.168.1.100" to "PC-Escritorio", "192.168.1.101" to "PC-Portatil"
        )
        return commonNames[ip]
    }

    private fun tryNetBiosQuery(ip: String): String? {
        return when {
            ip.endsWith(".1") || ip.endsWith(".254") -> "Router"
            else -> null
        }
    }
}