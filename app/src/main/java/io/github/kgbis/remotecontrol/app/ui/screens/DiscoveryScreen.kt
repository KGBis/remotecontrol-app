package io.github.kgbis.remotecontrol.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.Card
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import io.github.kgbis.remotecontrol.app.network.scanner.MDNSDiscovery
import io.github.kgbis.remotecontrol.app.viewmodel.DiscoveredDevice
import io.github.kgbis.remotecontrol.app.viewmodel.DiscoveryState
import io.github.kgbis.remotecontrol.app.viewmodel.DiscoveryViewModel

@Composable
fun rememberMDNSDiscovery(): MDNSDiscovery {
    val context = LocalContext.current
    return remember { MDNSDiscovery(context) }
}

@Composable
fun DiscoveryScreen(
    viewModel: DiscoveryViewModel = viewModel()
) {
    val context = LocalContext.current
    val discoveryState by viewModel.discoveryState.collectAsState()
    val discoveredDevices by viewModel.discoveredDevices.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
    ) {
        // Estado del discovery
        DiscoveryStatus(discoveryState)

        Spacer(modifier = Modifier.height(8.dp))

        // Controles
        DiscoveryControls(
            isDiscovering = discoveryState.isDiscovering,
            onStartDiscovery = { viewModel.startDiscovery() },
            onStopDiscovery = { viewModel.stopDiscovery() }
        )

        Spacer(modifier = Modifier.height(16.dp))

        // Lista de dispositivos
        DeviceList(devices = discoveredDevices)
    }
}

@Composable
private fun DiscoveryStatus(state: DiscoveryState) {
    val (icon, text, color) = when {
        state.isDiscovering -> Triple("🔍", "Buscando dispositivos...", Color.Blue)
        state.error != null -> Triple("❌", "Error: ${state.error}", Color.Red)
        else -> Triple("✅", "${state.deviceCount} dispositivos encontrados", Color.Green)
    }

    Row(
        verticalAlignment = Alignment.CenterVertically,
        modifier = Modifier.fillMaxWidth()
    ) {
        Text(
            text = icon,
            fontSize = 24.sp,
            modifier = Modifier.padding(end = 8.dp)
        )
        Text(
            text = text,
            color = color,
            fontSize = 16.sp,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
private fun DiscoveryControls(
    isDiscovering: Boolean,
    onStartDiscovery: () -> Unit,
    onStopDiscovery: () -> Unit
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        Button(
            onClick = onStartDiscovery,
            enabled = !isDiscovering,
            modifier = Modifier.weight(1f)
        ) {
            Icon(
                imageVector = Icons.Default.Search,
                contentDescription = null,
                modifier = Modifier.padding(end = 4.dp)
            )
            Text("Iniciar búsqueda")
        }

        Button(
            onClick = onStopDiscovery,
            enabled = isDiscovering,
            colors = ButtonDefaults.buttonColors(),
            modifier = Modifier.weight(1f)
        ) {
            Icon(
                imageVector = Icons.Default.Stop,
                contentDescription = null,
                modifier = Modifier.padding(end = 4.dp)
            )
            Text("Detener")
        }
    }
}

@Composable
private fun DeviceList(devices: List<DiscoveredDevice>) {
    if (devices.isEmpty()) {
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .height(200.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "No se encontraron dispositivos",
                color = Color.Gray,
                fontStyle = FontStyle.Italic
            )
        }
    } else {
        LazyColumn(
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            items(devices) { device -> DeviceCard(device = device) }
        }
    }
}

@Composable
private fun DeviceCard(device: DiscoveredDevice) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = 4.dp
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp)
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Icono según tipo
                val icon = when {
                    device.type.contains("remotecontrol", ignoreCase = true) -> "🎮"
                    device.type.contains("http", ignoreCase = true) -> "🌐"
                    device.type.contains("ssh", ignoreCase = true) -> "🔐"
                    device.type.contains("vnc", ignoreCase = true) -> "🖥️"
                    else -> "📱"
                }

                Text(
                    text = icon,
                    fontSize = 24.sp,
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
                }

                IconButton(
                    onClick = { /* Acción de conexión */ }
                ) {
                    Icon(Icons.Default.PlayArrow, contentDescription = "Conectar")
                }
            }

            // TXT records si existen
            if (device.txtRecords.isNotEmpty()) {
                Spacer(modifier = Modifier.height(8.dp))
                Text(
                    text = device.txtRecords.entries.joinToString { "${it.key}=${it.value}" },
                    fontSize = 12.sp,
                    color = Color.LightGray,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
    }
}