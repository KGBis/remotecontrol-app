package com.example.remote.shutdown.network

import android.util.Log
import com.example.remote.shutdown.data.Device
import com.example.remote.shutdown.data.DeviceStatus
import com.example.remote.shutdown.network.NetworkActions.sendInfoRequest
import com.example.remote.shutdown.network.NetworkScanner.portsToScan
import com.example.remote.shutdown.util.Constants.DEFAULT_SUBNET
import com.example.remote.shutdown.util.Constants.SHUTDOWN_PORT
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
        baseIp: String = DEFAULT_SUBNET,
        maxConcurrent: Int = 20
    ): List<Device> = coroutineScope {
        val semaphore = Semaphore(maxConcurrent)
        val jobs = (1..254).map { i ->
            async(Dispatchers.IO) {
                val ip = "$baseIp.$i"

                // Limitar la concurrencia
                semaphore.withPermit {
                    if(ip.endsWith(".43")) {
                        Log.i("scanLocalNetwork", "What happens with 1.43?")
                    }
                    if (checkPcStatus(ip)) {
                        val device = Device(ip, ip, "")
                        /*
                        val now = System.currentTimeMillis()
                        val (success, hostname, macAddress) = sendInfoRequest(device)
                        device.name = hostname
                        device.mac = macAddress
                        Log.i("scanLocalNetwork", "Took ${System.currentTimeMillis() - now} ms. success? $success, Device -> $device")
                        */
                        device
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
     *
     * The steps to achieve these are:
     * 1. Check if [SHUTDOWN_PORT] is reachable. If so, device is online and can be shut down. Go to 3.
     * If [SHUTDOWN_PORT] is not reachable, could be by shutdown computer application not being installed
     * or device offline. Go to next.
     * 2. Check if any of the [portsToScan] can be reached. If so, device is online.
     * 3. Check if device has its MAC filled in the app. If so, Wake-on-LAN can be used.
     */
    suspend fun deviceStatus(device: Device, timeout: Int = 500): DeviceStatus =
        withContext(Dispatchers.IO) {
            val deviceStatus = DeviceStatus(isOnline = false, canWakeup = false, canShutdown = false)

            // Check for SHUTDOWN_PORT
            val result: Boolean = canConnect(device.ip, SHUTDOWN_PORT, timeout)
            if(result) {
                deviceStatus.isOnline = true
                deviceStatus.canShutdown = true
            } else {
                deviceStatus.isOnline = isPcOnline(device.ip)
            }

            if(device.mac.isNotBlank()) {
                deviceStatus.canWakeup = true
            }

            return@withContext deviceStatus
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
                canConnect(ip, port, timeout)
            }
        }
    }

    private fun canConnect(ip: String, port: Int, timeout: Int): Boolean = try {
        Socket().use { socket ->
            socket.connect(InetSocketAddress(ip, port), timeout)
            Log.i("canConnect", "for ip $ip:$port connected")
        }
        true
    } catch (_: Exception) {
        false
    }

}
