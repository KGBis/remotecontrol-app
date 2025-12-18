package io.github.kgbis.remotecontrol.app.ui.components

import android.util.Log
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.Button
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import io.github.kgbis.remotecontrol.app.network.scanner.MDNSDiscovery


@Composable
fun SimpleMDNSDiscovery() {
    val context = LocalContext.current
    val devices = remember { mutableStateListOf<String>() }

    val discovery = remember {
        MDNSDiscovery(context).apply {
            setDiscoveryListener(object : MDNSDiscovery.DiscoveryListener {
                override fun onServiceFound(service: MDNSDiscovery.DiscoveredService) {
                    Log.d("mDNS discovery", "service found")
                    devices.add("${service.name} (${service.host}:${service.port})")
                }
                override fun onServiceLost(serviceName: String) {
                    Log.d("mDNS discovery", "service lost")
                    devices.removeAll { it.startsWith(serviceName) }
                }
                override fun onDiscoveryStarted() {
                    Log.d("mDNS discovery", "discovery started")
                }
                override fun onDiscoveryStopped() {
                    Log.d("mDNS discovery", "discovery stopped")
                }
                override fun onError(message: String) {
                    Log.e("mDNS discovery", "discovery error!")
                }
            })
        }
    }

    Column {
        Button(onClick = { discovery.startDiscovery() }) {
            Text("Buscar mDNS")
        }

        Button(onClick = { discovery.stopDiscovery() }) {
            Text("Detener")
        }

        devices.forEach { device ->
            Text(device)
        }
    }

    DisposableEffect(Unit) {
        onDispose { discovery.stopDiscovery() }
    }
}