package com.example.remote.shutdown.ui.components

import android.util.Log
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Circle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Power
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.remote.shutdown.R
import com.example.remote.shutdown.data.Device
import com.example.remote.shutdown.network.NetworkUtils

@Composable
fun DeviceItem(
    device: Device,
    onShutdown: () -> Unit,
    onWake: () -> Unit,
    onDelete: () -> Unit
) {
    // Wake-on-LAN flags and colors
    if(device.mac.isNullOrBlank()) {
        device.canWakeup = false
    }
    Log.i("Device Item", "Can wake up? ${device.canWakeup}")
    val wolColor = if (device.canWakeup == true) Color.Unspecified else Color.Gray

    Card(modifier = Modifier.fillMaxWidth().padding(vertical = 4.dp),
        elevation = CardDefaults.cardElevation(4.dp)) {
        Row(Modifier.fillMaxWidth().padding(12.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically) {
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                val status by produceState(initialValue = Color.Red, device.ip) {
                    value = NetworkUtils.getColorPcOnline(device.ip, 6800)
                }
                Icon(Icons.Default.Circle, tint = status, contentDescription = "Estado")
            }

            // Name & IP
            Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.Center) {
                Text(device.name, style = MaterialTheme.typography.titleMedium)
                Text(device.ip, style = MaterialTheme.typography.bodySmall)
                Text(if (device.mac.isNullOrBlank()) "[no MAC entered]" else device.mac!!, style = MaterialTheme.typography.bodySmall)
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
                    IconButton(onClick = onWake,
                        enabled = device.canWakeup == true
                    ) {
                        Icon(Icons.Default.PowerSettingsNew,
                            tint = wolColor,
                            contentDescription = stringResource(R.string.wol_action)
                        )
                    }
                    Text(stringResource(R.string.wol_action),
                        color = wolColor,
                        style = MaterialTheme.typography.bodySmall
                    )
                }

                // Delete icon + texto
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    IconButton(onClick = onDelete) {
                        Icon(Icons.Default.Delete, contentDescription = stringResource(R.string.delete_action))
                    }
                    Text(stringResource(R.string.delete_action), style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }
}