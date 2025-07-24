package com.example.remote.shutdown.network

import android.content.Context
import android.util.Log
import com.example.remote.shutdown.common.StopWatch
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.IOException
import java.net.Inet4Address
import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.NetworkInterface
import java.net.Socket
import kotlin.sequences.forEach

class NetworkScanner {

    init {
        Log.d("SanityCheck", "NetworkScanner initialized")
    }

    private val _activeIps = MutableStateFlow<List<String>>(emptyList())
    val activeIps: StateFlow<List<String>> = _activeIps

    fun startScan() {
        Log.i("SanityCheck", "Hey, info log")
        var localIp = getLocalIpAddress() ?: return
        if(localIp.startsWith("10.")) {
            localIp = "192.168.1.23"
            Log.d("Scan", "IP changed to $localIp")
        }

        val subnet = localIp.substringBeforeLast(".")
        val foundIps = mutableListOf<String>()


        val watch = StopWatch.createStarted()
        CoroutineScope(Dispatchers.IO).launch {
            Log.d("Scan", "From coroutine IO thread")

            withContext(Dispatchers.Main) {
                Log.d("Scan", "From coroutine MAIN thread")
            }
        }

        CoroutineScope(Dispatchers.IO).launch {
            val jobs = (1..254).map { i ->
                async {
                    try {
                        val host = "$subnet.$i"
                        if (host == localIp) {
                            //Log.d("Scan", "Skipping local device $host")
                            println("Skipping local device $host")
                            return@async
                        }

                        val reachableWatch = StopWatch.createStarted()
                        isHostReachable(host)
                        reachableWatch.stop()
                        // Log.i("isHostReachable", "Took ${reachableWatch.getElapsedTimeMillis()} ms")
                        println("Took ${reachableWatch.getElapsedTimeMillis()} ms")

                        reachableWatch.start()
                        if (isHostAlive(host, port = 6800)) {
                            synchronized(foundIps) {
                                foundIps.add(host)
                                _activeIps.value = foundIps.toList()
                            }
                        }
                        reachableWatch.stop()
                        println("Took ${reachableWatch.getElapsedTimeMillis()} ms")
                    } catch (e: Exception) {
                        //Log.e("Scan", "Exception in async block: ${e.message}", e)
                        println("Exception in async block: ${e.message}")
                    }
                }
            }


            jobs.awaitAll()
            watch.stop()
            Log.d("Scan", "Full scan took ${watch.getElapsedTimeMillis()} ms")
        }


        /*Log.d("scan", "really finished?")
        if(watch.isRunning()) {
            Log.d("scan", "Still running")
        } else {
            Log.d("scan", "${watch.getElapsedTimeMillis()} milis")
        }*/
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

    private fun isHostReachable(ip: String): Boolean {
        return try {
            val address = InetAddress.getByName(ip)
            address.isReachable(100) // timeout in ms
        } catch (e: Exception) {
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

    fun readOuiFile(context: Context): List<String> {
        val lines = mutableListOf<String>()
        try {
            context.assets.open("oui.txt").bufferedReader().useLines { sequence ->
                sequence.forEach { line ->
                    lines.add(line)
                }
            }
        } catch (e: IOException) {
            e.printStackTrace()
        }
        return lines
    }
}
