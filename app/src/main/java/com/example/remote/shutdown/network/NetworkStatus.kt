package com.example.remote.shutdown.network

import com.example.remote.shutdown.data.DeviceStatus

object NetworkStatus {
    const val SHUTDOWN_PORT = 6800

   /* suspend fun isOnline(ip: String, timeout: Int = 1200): Boolean =
        NetworkUtils.canConnect(ip, 80, timeout) // o ICMP si lo quieres

    suspend fun isShutdownPortOpen(ip: String, timeout: Int = 1000): Boolean =
        NetworkUtils.canConnect(ip, SHUTDOWN_PORT, timeout)*/

    suspend fun deviceStatus(ip: String, timeout: Int = 500): DeviceStatus {
        return DeviceStatus()
    }
}
