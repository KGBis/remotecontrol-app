package io.github.kgbis.remotecontrol.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import io.github.kgbis.remotecontrol.app.network.scanner.MDNSDiscovery
import io.github.kgbis.remotecontrol.app.viewmodel.DiscoveredDevice
import kotlinx.coroutines.launch

@Composable
fun MDNSDiscoveryScreen() {
    val context = LocalContext.current
    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
    val coroutineScope = rememberCoroutineScope()

    // Estados
    var isDiscovering by remember { mutableStateOf(false) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    val discoveredDevices = remember { mutableStateListOf<DiscoveredDevice>() }

    // Inicializar MDNSDiscovery
    val mdnsDiscovery = remember {
        MDNSDiscovery(context).apply {
            setDiscoveryListener(object : MDNSDiscovery.DiscoveryListener {
                override fun onServiceFound(service: MDNSDiscovery.DiscoveredService) {
                    coroutineScope.launch {
                        discoveredDevices.add(
                            DiscoveredDevice(
                                name = service.name,
                                ip = service.host,
                                port = service.port,
                                type = service.type,
                                txtRecords = service.txtRecords
                            )
                        )
                    }
                }

                override fun onServiceLost(serviceName: String) {
                    coroutineScope.launch {
                        discoveredDevices.removeAll { it.name == serviceName }
                    }
                }

                override fun onDiscoveryStarted() {
                    isDiscovering = true
                    errorMessage = null
                }

                override fun onDiscoveryStopped() {
                    isDiscovering = false
                }

                override fun onError(message: String) {
                    errorMessage = message
                    isDiscovering = false
                }
            })
        }
    }

    // Efecto para manejar el ciclo de vida
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_PAUSE,
                Lifecycle.Event.ON_STOP -> {
                    mdnsDiscovery.stopDiscovery()
                }
                else -> Unit
            }
        }

        lifecycleOwner.lifecycle.addObserver(observer)

        onDispose {
            mdnsDiscovery.stopDiscovery()
            lifecycleOwner.lifecycle.removeObserver(observer)
        }
    }

    // UI
    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Cabecera
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(
                text = "🔍 Discovery mDNS",
                fontSize = 20.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.weight(1f)
            )

            Text(
                text = "${discoveredDevices.size}",
                fontSize = 24.sp,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary,
                modifier = Modifier.padding(8.dp)
            )
        }

        Spacer(modifier = Modifier.height(16.dp))

        // Controles
        Row(
            horizontalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            Button(
                onClick = {
                    discoveredDevices.clear()
                    mdnsDiscovery.startDiscovery()
                },
                enabled = !isDiscovering,
                modifier = Modifier.weight(1f)
            ) {
                Icon(Icons.Default.Search, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Buscar servicios")
            }

            Button(
                onClick = { mdnsDiscovery.stopDiscovery() },
                enabled = isDiscovering,
                colors = ButtonDefaults.buttonColors(),
                modifier = Modifier.weight(1f)
            ) {
                Icon(Icons.Default.Stop, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Detener")
            }
        }

        // Buscar servicios específicos
        Spacer(modifier = Modifier.height(8.dp))
        Row(
            horizontalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            Button(
                onClick = {
                    mdnsDiscovery.startDiscovery("_rpcctl._tcp")
                },
                enabled = !isDiscovering,
                modifier = Modifier.weight(1f)
            ) {
                Text("RemoteControl")
            }

            Button(
                onClick = {
                    mdnsDiscovery.startDiscovery("_http._tcp")
                },
                enabled = !isDiscovering,
                modifier = Modifier.weight(1f)
            ) {
                Text("HTTP")
            }

            Button(
                onClick = {
                    mdnsDiscovery.startDiscovery("_ssh._tcp")
                },
                enabled = !isDiscovering,
                modifier = Modifier.weight(1f)
            ) {
                Text("SSH")
            }
        }

        // Estado/Error
        Spacer(modifier = Modifier.height(16.dp))

        if (errorMessage != null) {
            Card(
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(
                    text = "Error: $errorMessage",
                    color = Color.Red,
                    modifier = Modifier.padding(8.dp)
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
        }

        if (isDiscovering) {
            LinearProgressIndicator(
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Buscando servicios mDNS en la red...",
                fontStyle = FontStyle.Italic,
                color = Color.Blue
            )
            Spacer(modifier = Modifier.height(8.dp))
        }

        // Lista de dispositivos
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(8.dp),
            modifier = Modifier.weight(1f)
        ) {
            items(discoveredDevices.size) { device ->
                ServiceCard(device = discoveredDevices[device])
            }
        }
    }
}

@Composable
fun ServiceCard(device: DiscoveredDevice) {
    Card(
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Icono basado en tipo de servicio
                val (icon, color) = when {
                    device.type.contains("remotecontrol") -> Pair("🎮", Color.Magenta)
                    device.type.contains("http") -> Pair("🌐", Color.Blue)
                    device.type.contains("ssh") -> Pair("🔐", Color.Green)
                    device.type.contains("printer") -> Pair("🖨️", Color.Cyan)
                    else -> Pair("📱", Color.Gray)
                }

                Text(
                    text = icon,
                    fontSize = 30.sp,
                    modifier = Modifier.padding(end = 12.dp)
                )

                Column(
                    modifier = Modifier.weight(1f)
                ) {
                    Text(
                        text = device.name,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                    Text(
                        text = "${device.ip}:${device.port}",
                        color = Color.Gray,
                        fontSize = 14.sp
                    )
                    Text(
                        text = device.type,
                        color = color,
                        fontSize = 12.sp,
                        fontStyle = FontStyle.Italic
                    )
                }

                IconButton(
                    onClick = {
                        // Conectar a este servicio
                        // viewModel.connectToDevice(device)
                    }
                ) {
                    Icon(
                        Icons.Default.PlayArrow,
                        contentDescription = "Conectar",
                        tint = Color.Green
                    )
                }
            }

            // Mostrar TXT records si existen
            if (device.txtRecords.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                HorizontalDivider()
                Spacer(modifier = Modifier.height(8.dp))
                Column {
                    Text(
                        text = "Metadatos:",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.Gray
                    )
                    device.txtRecords.forEach { (key, value) ->
                        Text(
                            text = "  $key: $value",
                            fontSize = 11.sp,
                            color = Color.DarkGray
                        )
                    }
                }
            }
        }
    }
}