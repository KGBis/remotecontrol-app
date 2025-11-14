package com.example.remote.shutdown.network

import android.util.Log
import androidx.compose.ui.graphics.Color
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.Socket

object NetworkUtils {

    private val portsToScan = listOf(445, 135, 22, 80, 443, 139, 3389, 6800)

    suspend fun isHostReachable(ip: String, port: Int, timeoutMs: Int = 300): Boolean =
        withContext(Dispatchers.IO) {
            try {
                // Log.d("NetworkUtils", "isHostReachable(ip: $ip, port: $port, timeoutMs: $timeoutMs) INIT")
                Socket().use { socket ->
                    socket.connect(InetSocketAddress(ip, port), timeoutMs)

                    val writer = socket.getOutputStream().bufferedWriter()
                    val reader = socket.getInputStream().bufferedReader()

                    // Log.i("NET", "got Socket reader/writter")

                    writer.write("CONN")
                    writer.flush() // Muy importante para que se envíe

                    // Log.i("NET", "wrote msg to writter")

                    // Leer respuesta (ACK)
                    val response = reader.readLine() // Bloquea hasta que llegue dato o timeout
                    // Log.d("NET", "isHostReachable(ip: $ip, port: $port, timeoutMs: $timeoutMs) -> true, response: $response")
                    return@use true
                }
            } catch (e: Exception) {
                Log.d(
                    "NetworkUtils",
                    "isHostReachable(ip: $ip, port: $port, timeoutMs: $timeoutMs) -> false ($e)"
                )
                if (ip.contains("1.43")) {
                    Log.i(
                        "pingInetAddress",
                        "socket.connect(InetSocketAddress($ip, $port), $timeoutMs) = false"
                    )
                }
                return@withContext false
            }
        }

    suspend fun pingInetAddress(ip: String, timeoutMs: Int = 500): Boolean /*=
        withContext(Dispatchers.IO)*/ {
        try {
            // Log.d("NetworkUtils", "pingInetAddress(ip: $ip, timeoutMs: $timeoutMs) INIT")
            val isReachable = InetAddress.getByName(ip).isReachable(timeoutMs)
            // Log.d("NetworkUtils", "pingInetAddress(ip: $ip, timeoutMs: $timeoutMs) RESULT = $isReachable")
            if (ip.contains("1.43")) {
                Log.i(
                    "pingInetAddress",
                    "InetAddress.getByName($ip).isReachable($timeoutMs) = $isReachable"
                )
            }
            return isReachable
        } catch (e: Exception) {
            Log.w("NetworkUtils", "pingInetAddress(ip: $ip, timeoutMs: $timeoutMs) EXCP -> $e")
            return false
        }
    }

    suspend fun pingCommand(ip: String, count: Int = 1, timeoutSec: Int = 1): Boolean /*=
        withContext(Dispatchers.IO)*/ {
        try {
            // Log.d("NetworkUtils", "pingCommand(ip: $ip, count: $count timeoutSec: $timeoutSec) INIT")
            val process = Runtime.getRuntime()
                .exec("ping -c $count -W $timeoutSec $ip")

            // val output = process.inputStream.bufferedReader().use { it.readText() }
            // val error  = process.errorStream.bufferedReader().use { it.readText() }

            val exitCode = process.waitFor()

            // Log.d("NetworkUtils", "pingCommand(ip: $ip, count: $count timeoutSec: $timeoutSec) EXITCODE=$exitCode")
            return exitCode == 0
        } catch (e: Exception) {
            Log.d(
                "NetworkUtils",
                "pingCommand(ip: $ip, count: $count timeoutSec: $timeoutSec) EXCP -> $e"
            )
            return false
        }
    }

    suspend fun isPcOnline(ip: String, port: Int = 5000): Boolean =
        withContext(Dispatchers.IO) {
            val init = System.currentTimeMillis()

            /*
            if(pingInetAddress(ip)) {
                Log.i("isPcOnline", "$ip <== STOP pingInetAddress in ${System.currentTimeMillis() - init} ms"
                )
                return@withContext true
            }

            if (isHostReachable(ip, port)) {
                Log.i("isPcOnline", "$ip <== STOP isHostReachable in ${System.currentTimeMillis() - init} ms")
                return@withContext true
            }

            if (pingCommand(ip)) {
                Log.i("isPcOnline", "$ip <== STOP pingCommand in ${System.currentTimeMillis() - init} ms")
                return@withContext true
            }

            // Log.i("isPcOnline", "$ip <== NOTHiNG FOuND in ${System.currentTimeMillis() - init} ms")
            return@withContext false
             */

            if (checkPcStatus(ip)) {
                Log.i(
                    "isPcOnline",
                    "$ip <== STOP pingInetAddress in ${System.currentTimeMillis() - init} ms"
                )
                return@withContext true
            } else {
                /*Log.i(
                    "isPcOnline",
                    "$ip <== NOTHiNG FOuND in ${System.currentTimeMillis() - init} ms"
                )*/
                return@withContext false
            }
        }

    suspend fun checkPcStatus(ip: String, timeout: Int = 500): Boolean {
        return portsToScan.any { port ->
            withContext(Dispatchers.IO) {
                try {
                    Socket().connect(InetSocketAddress(ip, port), timeout)
                    Log.i("checkPcStatus", "for ip $ip:$port connected")
                    true
                } catch (e: Exception) {
                    false
                }
            }
        }

        /*if (portResponse) return true

        // 2. Si no responde a puertos, intentar ping
        return withContext(Dispatchers.IO) {
            try {
                Log.i("checkPcStatus", "for ip $ip trying PING")
                val process = ProcessBuilder("ping", "-c", "1", "-W", "$timeout", ip).start()
                process.waitFor() == 0
            } catch (e: Exception) {
                Log.i("checkPcStatus", "for ip $ip trying PING. Exception $e")
                false
            }
        }*/
    }

    suspend fun getColorPcOnline(ip: String, port: Int = 5000): Color {
        return if (isPcOnline(ip, port)) {
            Color.Green
        } else {
            Color.Red
        }
    }

}
