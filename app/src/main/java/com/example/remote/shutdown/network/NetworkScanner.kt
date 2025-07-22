package com.example.remote.shutdown.network

import android.util.Log
import com.example.remote.shutdown.common.StopWatch
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import java.io.IOException
import java.net.Inet4Address
import java.net.InetSocketAddress
import java.net.NetworkInterface
import java.net.Socket

class NetworkScanner {

    private val _activeIps = MutableStateFlow<List<String>>(emptyList())
    val activeIps: StateFlow<List<String>> = _activeIps

    fun startScan() {
        var localIp = getLocalIpAddress() ?: return
        if(localIp.startsWith("10.")) {
            localIp = "192.168.1.23"
            Log.d(null, "IP changed to $localIp")
        }

        val subnet = localIp.substringBeforeLast(".")
        val foundIps = mutableListOf<String>()


        val watch = StopWatch.createStarted()
        CoroutineScope(Dispatchers.IO).launch {
            val jobs = (1..254).map { i ->
                async {
                    val host = "$subnet.$i"

                    // exclude oneself IP
                    if(host == localIp) {
                        Log.d(null, "It's local device, skipping")
                        return@async
                    }

                    if(isHostAlive(host, port = 6800)) {
                        synchronized(foundIps) {
                            foundIps.add(host)
                            _activeIps.value = foundIps.toList()
                        }
                    }
                }
            }
            jobs.awaitAll()
            watch.stop()
            Log.d(null, "${watch.getElapsedTimeMillis()} milis")
        }
        Log.d(null, "really finished?")
        if(watch.isRunning()) {
            Log.d(null, "Still running")
        } else {
            Log.d(null, "${watch.getElapsedTimeMillis()} milis")
        }
    }

    fun isHostAlive(ip: String, port: Int = 80, timeout: Int = 200): Boolean {
        return try {
            Socket().use { socket ->
                Log.d(null, "Testing $ip:$port")
                socket.connect(InetSocketAddress(ip, port), timeout)
                Log.d(null, "$ip:$port is ALIVE")
                true
            }
        } catch (e: IOException) {
            Log.d(null, "$ip:$port is DEAD/SHUTDOWN")
            false
        }
    }

    private fun getLocalIpAddress(): String? {
        val interfaces = NetworkInterface.getNetworkInterfaces()
        for (intf in interfaces) {
            val addrs = intf.inetAddresses
            for (addr in addrs) {
                if (!addr.isLoopbackAddress && addr is Inet4Address) {
                    return addr.hostAddress
                }
            }
        }
        return null
    }
}
