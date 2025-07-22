package com.example.remote.shutdown.network

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.*
import java.net.InetSocketAddress
import java.net.Socket

class NetworkScanViewModel : ViewModel() {
    private val _activeIps = MutableStateFlow<List<String>>(emptyList())

    val activeIps: StateFlow<List<String>> = _activeIps.asStateFlow()

    private val commonPorts = listOf(22, 80, 139, 445, 631, 3389)

    @OptIn(ExperimentalCoroutinesApi::class)
    fun scanNetwork(subnet: String = "192.168.1.") {
        _activeIps.value = emptyList()

        viewModelScope.launch {
            val results = mutableListOf<String>()
            val dispatcher = Dispatchers.IO.limitedParallelism(100)

            coroutineScope {
                (1..254).forEach { i ->
                    val ip = "$subnet$i"
                    launch(dispatcher) {
                        var foundOpenPort = false

                        for (port in commonPorts) {
                            if (foundOpenPort) break
                            try {
                                Socket().use { socket ->
                                    socket.connect(InetSocketAddress(ip, port), 300)
                                    foundOpenPort = true
                                }
                            } catch (_: Exception) { }
                        }

                        if (foundOpenPort) {
                            synchronized(results) {
                                if (!results.contains(ip)) results.add(ip)
                            }
                            _activeIps.value = results.toList()
                        }
                    }
                }
            }
        }
    }

}
