package io.github.kgbis.remotecontrol.app.features.discovery.ui

import android.util.Log
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.RestartAlt
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import io.github.kgbis.remotecontrol.app.R
import io.github.kgbis.remotecontrol.app.features.discovery.model.DiscoveringState
import io.github.kgbis.remotecontrol.app.features.discovery.DetectedDevicesList
import io.github.kgbis.remotecontrol.app.features.discovery.model.DeviceTransformResult
import io.github.kgbis.remotecontrol.app.features.discovery.DiscoveryViewModel
import io.github.kgbis.remotecontrol.app.features.discovery.model.DiscoveryState
import io.github.kgbis.remotecontrol.app.features.devices.DevicesViewModel

@Composable
fun ColumnScope.DiscoveryScreenContent(
    modifier: Modifier,
    isDiscovering: Boolean,
    devices: List<DeviceTransformResult>,
    navController: NavController,
    devicesVm: DevicesViewModel,
    state: DiscoveryState,
    discoveryVm: DiscoveryViewModel
) {
    val discoveryModifier = modifier
        .fillMaxWidth(0.75f)
        .align(Alignment.CenterHorizontally)

    if (isDiscovering || state.discoveringState == DiscoveringState.DISCOVERING) {
        Log.i("DiscoveryScreenContent", "DISCOVERING")
        DiscoveryInProgress(modifier = discoveryModifier)
    }

    if (state.discoveringState == DiscoveringState.FINISHED && devices.isEmpty()) {
        Log.i("DiscoveryScreenContent", "FINISHED and Empty")
        MessageAndRescan(
            text = stringResource(R.string.discovery_no_devices_found),
            modifier = modifier,
            onClick = { discoveryVm.startDiscovery() }
        )
    }

    // Found devices
    if (devices.isNotEmpty()) {
        if (state.discoveringState == DiscoveringState.FINISHED)
            MessageAndRescan(
                text = stringResource(R.string.discovery_devices_found),
                modifier = modifier,
                onClick = { discoveryVm.startDiscovery() }
            )
        DetectedDevicesList(
            results = devices,
            navController = navController,
            devicesVm = devicesVm
        )
    }

    if (state.error != null) {
        MessageAndRescan(
            text = stringResource(R.string.discovery_devices_found),
            modifier = modifier,
            onClick = {
                discoveryVm.clearError()
                discoveryVm.startDiscovery()
            }
        )
    }
}

@Composable
fun MessageAndRescan(
    text: String,
    modifier: Modifier,
    onClick: () -> Unit
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable { onClick() }) {
        Text(
            text = text,
            textAlign = TextAlign.End,
            modifier = modifier
                .align(Alignment.CenterVertically)
                .fillMaxWidth(0.85f)
        )
        Icon(
            imageVector = Icons.Default.RestartAlt, contentDescription = "", modifier = modifier
                .align(Alignment.CenterVertically)
                .fillMaxWidth()
        )
    }

}

@Composable
private fun DiscoveryInProgress(
    modifier: Modifier,
) {
    Text(
        modifier = modifier,
        text = stringResource(R.string.network_scan_running_2),
        fontStyle = FontStyle.Italic
    )
    LinearProgressIndicator(modifier = modifier)
    Spacer(modifier = Modifier.height(8.dp))
}

