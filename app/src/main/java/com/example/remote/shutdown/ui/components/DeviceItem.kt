package com.example.remote.shutdown.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
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
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.wear.compose.material.ContentAlpha
import com.example.remote.shutdown.R
import com.example.remote.shutdown.data.Device
import com.example.remote.shutdown.data.DeviceStatus
import com.example.remote.shutdown.data.State
import com.example.remote.shutdown.viewmodel.MainViewModel

@Composable
fun DeviceItem(
    device: Device,
    viewModel: MainViewModel,
    onShutdown: () -> Unit,
    onWake: () -> Unit,
    onDelete: () -> Unit,
    onEdit: () -> Unit
) {
    // get device status from viewmodel
    val deviceStatusMap by viewModel.deviceStatusMap.collectAsState()
    val deviceStatus = deviceStatusMap.getOrDefault(device.ip, DeviceStatus())

    // Online dot
    val onlineColor = when {
        deviceStatus.state == State.Unknown || deviceStatusMap.isEmpty() -> Color.Gray
        deviceStatus.state == State.HibernateOrStandby -> Color(0xFFFF9800)
        deviceStatus.state == State.Awake -> Color.Green
        else -> Color.Red
    }
    val onlineText = stringResource(
        when {
            deviceStatus.state == State.Unknown ||deviceStatusMap.isEmpty() -> R.string.status_unknown
            deviceStatus.state == State.Awake -> R.string.status_online
            else -> R.string.status_offline
        }
    )

    // shutdown (greyed and disabled?)
    // val canShutdown = deviceStatus.canShutdown == true && deviceStatus.isOnline == true
    val shutdowndColor = actionIconColor(deviceStatus.canShutdown ?: false)

    // Wake-on-LAN (greyed and disabled?)
    /*val canWoL = deviceStatus.canWakeup == true && deviceStatus.isOnline == false*/
    val wolColor = actionIconColor(deviceStatus.canWakeup ?: false)

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        elevation = CardDefaults.cardElevation(4.dp)
    ) {
        Row(
            Modifier
                .fillMaxWidth()
                .padding(10.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(modifier = Modifier.clickable { onEdit() }) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center
                ) {
                    Icon(Icons.Default.Circle, tint = onlineColor, contentDescription = onlineText)
                }

                Spacer(Modifier.width(1.dp))

                // Name & IP
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.Center,
                    modifier = Modifier.width(110.dp)
                ) {
                    Text(device.name,
                        overflow = TextOverflow.Ellipsis,
                        maxLines = 1,
                        style = MaterialTheme.typography.titleMedium
                    )
                    Text(device.ip, style = MaterialTheme.typography.bodySmall)
                    Text(
                        device.mac.ifBlank { stringResource(R.string.device_no_mac) },
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }

            Spacer(Modifier.width(8.dp))

            Row(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                // Shutdown icon + text
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    IconButton(
                        onClick = onShutdown,
                        enabled = deviceStatus.canShutdown ?: false
                    ) {
                        Icon(
                            Icons.Default.Power,
                            tint = shutdowndColor,
                            contentDescription = stringResource(R.string.shutdown_action)
                        )
                    }
                    Text(
                        stringResource(R.string.shutdown_action),
                        color = shutdowndColor,
                        style = MaterialTheme.typography.bodySmall
                    )
                }

                // WoL icon + text
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    IconButton(
                        onClick = onWake,
                        enabled = deviceStatus.canWakeup ?: false
                    ) {
                        Icon(
                            Icons.Default.PowerSettingsNew,
                            tint = wolColor,
                            contentDescription = stringResource(R.string.wol_action)
                        )
                    }
                    Text(
                        stringResource(R.string.wol_action),
                        color = wolColor,
                        style = MaterialTheme.typography.bodySmall
                    )
                }

                // Delete icon + texto
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    IconButton(onClick = onDelete) {
                        Icon(
                            Icons.Default.Delete,
                            contentDescription = stringResource(R.string.delete_action)
                        )
                    }
                    Text(
                        stringResource(R.string.delete_action),
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
        }
    }
}

@Composable
fun actionIconColor(value: Boolean): Color {
    return when (value) {
        true -> MaterialTheme.colorScheme.onBackground
        false -> MaterialTheme.colorScheme.onSurface.copy(alpha = ContentAlpha.disabled)
    }
}