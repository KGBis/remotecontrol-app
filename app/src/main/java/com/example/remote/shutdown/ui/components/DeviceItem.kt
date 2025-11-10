package com.example.remote.shutdown.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Circle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Power
import androidx.compose.material.icons.filled.Wifi
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import com.example.remote.shutdown.data.Device
import com.example.remote.shutdown.network.NetworkUtils.isPcOnline

@Composable
fun DeviceItem(
    device: Device,
    onShutdown: () -> Unit,
    onWake: () -> Unit,
    onDelete: () -> Unit
) {
    Card(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        elevation = CardDefaults.cardElevation(4.dp)) {
        Row(Modifier.fillMaxWidth().padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                var color = Color.Red
                if(isPcOnline(device.ip, 6800)) {
                    color = Color.Green
                }

                Icon(Icons.Default.Circle, tint = color, contentDescription = "Estado")
            }

            // Name & IP
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                Text(device.name, style = MaterialTheme.typography.titleMedium)
                Text(device.ip, style = MaterialTheme.typography.bodySmall)
                Text(device.mac.ifBlank { "[no MAC]" }, style = MaterialTheme.typography.bodySmall)
            }

            Row(horizontalArrangement = Arrangement.spacedBy(16.dp), verticalAlignment = Alignment.CenterVertically) {
                // Shutdown icon + texto
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    IconButton(onClick = onShutdown) {
                        Icon(Icons.Default.Power, contentDescription = "Apagar")
                    }
                    Text("Shutdown", style = MaterialTheme.typography.bodySmall)
                }

                // WoL icon + texto
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    IconButton(onClick = onWake) {
                        Icon(Icons.Default.Wifi, contentDescription = "WoL")
                    }
                    Text("Wake", style = MaterialTheme.typography.bodySmall)
                }

                // Delete icon + texto
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    IconButton(onClick = onDelete) {
                        Icon(Icons.Default.Delete, contentDescription = "Eliminar")
                    }
                    Text("Borrar", style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }
}