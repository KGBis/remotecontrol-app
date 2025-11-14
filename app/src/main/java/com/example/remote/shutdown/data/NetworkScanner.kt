package com.example.remote.shutdown.data

import com.example.remote.shutdown.network.NetworkUtils
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import kotlinx.coroutines.sync.Semaphore
import kotlinx.coroutines.sync.withPermit

object NetworkScanner {

    const val DEFAULT_PORT = 6800

    /**
     * Escanea la red local (por ejemplo, 192.168.1.1..254) de forma concurrente.
     * Retorna la lista de IPs que están online.
     */
    suspend fun scanLocalNetwork(baseIp: String = "192.168.1", maxConcurrent: Int = 20): List<Device> = coroutineScope {
        val semaphore = Semaphore(maxConcurrent)
        val jobs = (1..254).map { i ->
            async(Dispatchers.IO) {
                val ip = "$baseIp.$i"

                // Limitar la concurrencia
                semaphore.withPermit {
                    /*val host = scanHostWithTimeout(ip)
                    if(host is NetworkScanner.ScanResult.Success && host.isOnline) {
                        Device(
                            name = host.ip,
                            ip = host.ip,
                            mac = ""
                        )
                    } else {
                        null
                    }*/
                    if (NetworkUtils.isPcOnline(ip, DEFAULT_PORT)) {
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
