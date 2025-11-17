package com.example.remote.shutdown.network

import android.util.Log
import com.example.remote.shutdown.data.Device
import com.example.remote.shutdown.data.DeviceStatus
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
                    if (checkPcStatus(ip)) {
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

    /**
     * Returns a [DeviceStatus] filled with info about:
     * 1. Device is online/offline
     * 2. Device can be shut down
     * 3. Device can be woken up
     */
    suspend fun deviceStatus(ip: String, timeout: Int = 500): DeviceStatus =
        withContext(Dispatchers.IO) {
            return@withContext DeviceStatus()
    }

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
