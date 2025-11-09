package com.example.remote.shutdown.data

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.net.InetAddress

object NetworkScanner {

    suspend fun scanLocalNetwork(baseIp: String = "192.168.1."): List<Device> =
        withContext(Dispatchers.IO) {
            val found = mutableListOf<Device>()
            (1..254).forEach { i ->
                val host = "$baseIp$i"
                try {
                    val inet = InetAddress.getByName(host)
                    if (inet.isReachable(100)) {
                        found.add(Device(name = inet.hostName, ip = host))
                    }
                } catch (_: Exception) { }
            }
            found
        }
}
