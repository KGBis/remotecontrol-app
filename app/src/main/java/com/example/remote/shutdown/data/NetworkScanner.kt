package com.example.remote.shutdown.data

import com.example.remote.shutdown.network.NetworkUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit

object NetworkScanner {

    const val SHUTDOWN_PORT = 6800

    /**
     * Scans local network (i.e. 192.168.1.1 to 254 range) concurrently.
     * @return Device list of reachable (online) devices
     */
    suspend fun scanLocalNetwork(baseIp: String = "192.168.1", maxConcurrent: Int = 20): List<Device> = coroutineScope {
        val semaphore = Semaphore(maxConcurrent)
        val jobs = (1..254).map { i ->
            async(Dispatchers.IO) {
                val ip = "$baseIp.$i"

                // Limitar la concurrencia
                semaphore.withPermit {
                    if (NetworkUtils.isPcOnline(ip)) {
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

}
