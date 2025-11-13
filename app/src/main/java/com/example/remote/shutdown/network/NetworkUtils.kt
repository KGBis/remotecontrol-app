package com.example.remote.shutdown.network

import android.util.Log
import androidx.compose.ui.graphics.Color
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.Socket

object NetworkUtils {
    suspend fun isHostReachable(ip: String, port: Int, timeoutMs: Int = 300): Boolean =
        withContext(Dispatchers.IO) {
            try {
                Log.d("NetworkUtils", "isHostReachable(ip: $ip, port: $port, timeoutMs: $timeoutMs) INIT")
                Socket().use { socket ->
                    socket.connect(InetSocketAddress(ip, port), timeoutMs)

                    val writer = socket.getOutputStream().bufferedWriter()
                    val reader = socket.getInputStream().bufferedReader()

                    Log.i("NET", "got Socket reader/writter")

                    writer.write("CONN")
                    writer.flush() // Muy importante para que se envíe

                    Log.i("NET", "wrote msg to writter")

                    // Leer respuesta (ACK)
                    val response = reader.readLine() // Bloquea hasta que llegue dato o timeout
                    Log.d("NET", "isHostReachable(ip: $ip, port: $port, timeoutMs: $timeoutMs) -> true, response: $response")
                    true
                }
            } catch (e: Exception) {
                Log.d("NetworkUtils", "isHostReachable(ip: $ip, port: $port, timeoutMs: $timeoutMs) -> false ($e)")
                false
            }
        }

    suspend fun pingInetAddress(ip: String, timeoutMs: Int = 500): Boolean =
        withContext(Dispatchers.IO) {
            try {
                Log.d("NetworkUtils", "pingInetAddress(ip: $ip, timeoutMs: $timeoutMs) INIT")
                val isReachable = InetAddress.getByName(ip).isReachable(timeoutMs)
                Log.d("NetworkUtils", "pingInetAddress(ip: $ip, timeoutMs: $timeoutMs) RESULT = $isReachable")
                return@withContext isReachable
            } catch (e: Exception) {
                Log.d("NetworkUtils", "pingInetAddress(ip: $ip, timeoutMs: $timeoutMs) RESULT = false ($e)")
                false
            }
        }

    suspend fun pingCommand(ip: String, count: Int = 1, timeoutSec: Int = 1): Boolean =
        withContext(Dispatchers.IO) {
            try {
                Log.d("NetworkUtils", "pingCommand(ip: $ip, count: $count timeoutSec: $timeoutSec) INIT")
                val process = Runtime.getRuntime()
                    .exec("/system/bin/ping -c $count -W $timeoutSec $ip")

                val output = process.inputStream.bufferedReader().use { it.readText() }
                val error  = process.errorStream.bufferedReader().use { it.readText() }

                val exitCode = process.waitFor()

                Log.d("NetworkUtils", "pingCommand(ip: $ip, count: $count timeoutSec: $timeoutSec) EXITCODE=$exitCode")
                exitCode == 0
            } catch (e: Exception) {
                Log.d("NetworkUtils", "pingCommand(ip: $ip, count: $count timeoutSec: $timeoutSec) false ($e)")
                false
            }
        }

    suspend fun isPcOnline(ip: String, port: Int = 5000): Boolean =
        withContext(Dispatchers.IO) {
            if (isHostReachable(ip, port)) return@withContext true
            if (pingCommand(ip)) return@withContext true
            pingInetAddress(ip)
        }

    suspend fun getColorPcOnline(ip: String, port: Int = 5000): Color {
        return if(isPcOnline(ip, port)) { Color.Green } else {Color.Red}
    }

    // Obtiene el nombre del dispositivo desde la IP
    fun getDeviceName(ip: String): String {
        return try {
            val hostName = InetAddress.getByName(ip).hostName
            if (hostName == ip) "<unknown>" else hostName
        } catch (e: Exception) {
            "<unknown>"
        }
    }

    // Obtiene la MAC del dispositivo desde la tabla ARP
    fun getMac(ip: String): String {
        Log.d("NetworkUtils", "get MAC for IP $ip")
        try {
            val arpFile = File("/proc/net/arp")
            if (!arpFile.exists()) return ""
            for (line in arpFile.readLines()) {
                val parts = line.split(Regex("\\s+"))
                if (parts.size >= 4 && parts[0] == ip) {
                    val mac = parts[3]
                    if (mac.matches(Regex("..:..:..:..:..:.."))) {
                        return mac // ✅ Esto ahora devuelve de la función
                    }
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        return ""
    }

    fun discoverMacAddress(ipAddress: String): String? {
        return try {
            // Intentar obtener de la tabla ARP
            getMacFromArpTable(ipAddress) ?: scanNetworkForMac(ipAddress)
        } catch (e: Exception) {
            null
        }
    }

    @Deprecated(message = "arp scan not working sinde API level 30", level = DeprecationLevel.WARNING)
    private fun getMacFromArpTable(ipAddress: String): String? {
        return try {
            val arpTable = File("/proc/net/arp").readText()
            val lines = arpTable.split("\n")

            for (line in lines.drop(1)) {
                val parts = line.trim().split("\\s+".toRegex())
                if (parts.size >= 4 && parts[0] == ipAddress) {
                    val mac = parts[3]
                    if (mac != "00:00:00:00:00:00") {
                        return mac
                    }
                }
            }
            null
        } catch (e: Exception) {
            null
        }
    }

    private fun scanNetworkForMac(ipAddress: String): String? {
        // Esto requiere que el dispositivo esté activo y responda
        // Puedes intentar un ping o conexión para poblar la tabla ARP
        return try {
            Runtime.getRuntime().exec("ping -c 1 $ipAddress").waitFor()
            // Esperar y revisar ARP nuevamente
            Thread.sleep(1000)
            getMacFromArpTable(ipAddress)
        } catch (e: Exception) {
            null
        }
    }
}
