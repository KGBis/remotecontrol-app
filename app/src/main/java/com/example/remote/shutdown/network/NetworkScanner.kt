package com.example.remote.shutdown.network

import android.util.Log
import com.example.remote.shutdown.data.Device
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit
import kotlinx.coroutines.withContext
import java.net.InetSocketAddress
import java.net.Socket

object NetworkScanner {

    private val portsToScan = listOf(445, 135, 22, 80, 443, 139, 3389, 6800)

    /**
     * Scans local network (i.e. 192.168.1.1 to 254 range) concurrently.
     * @return Device list of reachable (online) devices
     */
    suspend fun scanLocalNetwork(
        baseIp: String = "192.168.1",
        maxConcurrent: Int = 20
    ): List<Device> = coroutineScope {
        val semaphore = Semaphore(maxConcurrent)
        val jobs = (1..254).map { i ->
            async(Dispatchers.IO) {
                val ip = "$baseIp.$i"

                // Limitar la concurrencia
                semaphore.withPermit {
                    if (/*isPcOnline(ip)*/checkPcStatus(ip)) {
                        Device(
                            name = ip,
                            ip = ip,
                            mac = ""
                        )
                    } else null
                }
            }
        }

        jobs.awaitAll().filterNotNull()
    }

    /*suspend fun isHostReachable(ip: String, port: Int, timeoutMs: Int = 300): Boolean =
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
        }*/

    /*suspend fun pingInetAddress(ip: String, timeoutMs: Int = 500): Boolean =
        withContext(Dispatchers.IO) {
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
            return@withContext isReachable
        } catch (e: Exception) {
            Log.w("NetworkUtils", "pingInetAddress(ip: $ip, timeoutMs: $timeoutMs) EXCP -> $e")
            return@withContext false
        }
    }*/

    /*suspend fun pingCommand(ip: String, count: Int = 1, timeoutSec: Int = 1): Boolean =
        withContext(Dispatchers.IO) {
        try {
            // Log.d("NetworkUtils", "pingCommand(ip: $ip, count: $count timeoutSec: $timeoutSec) INIT")
            val process = Runtime.getRuntime()
                .exec("ping -c $count -W $timeoutSec $ip")

            // val output = process.inputStream.bufferedReader().use { it.readText() }
            // val error  = process.errorStream.bufferedReader().use { it.readText() }

            val exitCode = process.waitFor()

            // Log.d("NetworkUtils", "pingCommand(ip: $ip, count: $count timeoutSec: $timeoutSec) EXITCODE=$exitCode")
            return@withContext exitCode == 0
        } catch (e: Exception) {
            Log.d(
                "NetworkUtils",
                "pingCommand(ip: $ip, count: $count timeoutSec: $timeoutSec) EXCP -> $e"
            )
            return@withContext false
        }
    }*/

    /**
     * Returns `true` if an IP address can be reached connecting to any of the [portsToScan].
     * If none of the ports connect, `false` is returned
     */
    suspend fun isPcOnline(ip: String): Boolean =
        withContext(Dispatchers.IO) {
            val init = System.currentTimeMillis()
            if (checkPcStatus(ip)) {
                Log.i(
                    "isPcOnline",
                    "$ip <== STOP pingInetAddress in ${System.currentTimeMillis() - init} ms"
                )
                return@withContext true
            } else {
                return@withContext false
            }
        }

    /**
     * Returns `true` if an IP address can be reached connecting to any of the [portsToScan].
     * If none of the ports connect, `false` is returned
     */
    suspend fun checkPcStatus(ip: String, timeout: Int = 500): Boolean {
        return portsToScan.any { port ->
            withContext(Dispatchers.IO) {
                try {
                    Socket().use { socket ->
                        socket.connect(InetSocketAddress(ip, port), timeout)
                        Log.i("checkPcStatus", "for ip $ip:$port connected")
                    }
                    true
                } catch (_: Exception) {
                    false
                }
            }
        }
    }

}
