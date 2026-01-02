package io.github.kgbis.remotecontrol.app.features.devices.ui

import android.util.Log
import androidx.compose.foundation.Image
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.text.TextAutoSize
// import androidx.compose.material.ContentAlpha
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Circle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Lan
import androidx.compose.material.icons.filled.NetworkPing
import androidx.compose.material.icons.filled.Power
import androidx.compose.material.icons.filled.PowerSettingsNew
import androidx.compose.material.icons.filled.Wifi
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
import androidx.compose.ui.graphics.painter.ColorPainter
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import io.github.kgbis.remotecontrol.app.R
import io.github.kgbis.remotecontrol.app.core.model.Device
import io.github.kgbis.remotecontrol.app.core.model.DeviceStatus
import io.github.kgbis.remotecontrol.app.core.model.InterfaceType
import io.github.kgbis.remotecontrol.app.core.model.DeviceState
import io.github.kgbis.remotecontrol.app.features.devices.DevicesViewModel

@Composable
fun DeviceItem(
    device: Device,
    devicesVm: DevicesViewModel,
    onShutdown: () -> Unit,
    onWake: () -> Unit,
    onDelete: () -> Unit,
    onEdit: () -> Unit
) {
    // get device status from viewmodel
    val deviceStatusMap by devicesVm.deviceStatusMap.collectAsState()
    val deviceStatus =
        deviceStatusMap.getOrDefault(device.id, DeviceStatus(device, trayReachable = false))

    Log.d("DeviceItem", "Device to paint -> $device")

    // Online dot
    val onlineColor = when {
        deviceStatus.state == DeviceState.UNKNOWN || deviceStatusMap.isEmpty() -> Color.Gray
        deviceStatus.state == DeviceState.ONLINE -> Color.Green
        else -> Color.Red
    }
    val onlineText = stringResource(
        when {
            deviceStatus.state == DeviceState.UNKNOWN || deviceStatusMap.isEmpty() -> R.string.status_unknown
            deviceStatus.state == DeviceState.ONLINE -> R.string.status_online
            else -> R.string.status_offline
        }
    )

    // shutdown (greyed and disabled?)
    val shutdowndColor = actionIconColor(deviceStatus.canShutdown)

    // Wake-on-LAN (greyed and disabled?)
    val wolColor = actionIconColor(deviceStatus.canWakeup)

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
                    Row {
                        val hostnameLength = device.hostname.length
                        Text(
                            device.hostname.substring(0, if(hostnameLength > 8) 8 else hostnameLength),
                            overflow = TextOverflow.Ellipsis,
                            maxLines = 1,
                            style = MaterialTheme.typography.titleMedium
                        )
                        Spacer(modifier = Modifier.width(2.dp))
                        OsIcon(device.deviceInfo?.osName ?: "")
                    }
                    val interfaces = device.interfaces
                    @Suppress("SENSELESS_COMPARISON")
                    if (interfaces == null)
                        Text(
                            stringResource(R.string.no_interfaces),
                            style = MaterialTheme.typography.bodySmall
                        )
                    else
                        device.interfaces.forEach {
                            Column {
                                val imageVector = when (it.type) {
                                    InterfaceType.WIFI -> Icons.Default.Wifi
                                    InterfaceType.ETHERNET -> Icons.Default.Lan
                                    else -> Icons.Default.NetworkPing
                                }
                                Row {
                                    Icon(
                                        imageVector,
                                        contentDescription = null,
                                        modifier = Modifier.size(16.dp)
                                    )
                                    Text(
                                        it.ip ?: "noip",
                                        style = MaterialTheme.typography.bodySmall
                                    )

                                }
                                Text(
                                    it.mac ?: stringResource(R.string.device_no_mac),
                                    style = MaterialTheme.typography.bodySmall
                                )
                            }
                        }
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
        false -> MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f /*ContentAlpha.disabled*/)
    }
}

@Composable
fun OsIcon(os: String) {
    val iconId = when {
        os.lowercase().contains("win") -> R.drawable.win_logo
        os.lowercase().contains("linux") -> R.drawable.linux_logo
        os.lowercase().contains("mac") -> R.drawable.mac_logo
        else -> Int.MIN_VALUE
    }

    if (iconId == Int.MIN_VALUE) {
        return Image(
            painter = ColorPainter(Color.Transparent),
            contentDescription = null,
            modifier = Modifier.size(24.dp)
        )
    }

    return Image(
        painter = painterResource(id = iconId),
        contentDescription = os,
        modifier = Modifier.size(24.dp) // Set icon size
    )
}