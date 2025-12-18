package io.github.kgbis.remotecontrol.app.ui.components

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import io.github.kgbis.remotecontrol.app.R
import io.github.kgbis.remotecontrol.app.network.ScanState
import io.github.kgbis.remotecontrol.app.viewmodel.DevicesViewModel
import io.github.kgbis.remotecontrol.app.viewmodel.ScanViewModel

@Composable
fun NetworkScannerSection(
    navController: NavController,
    modifier: Modifier,
    scanVm: ScanViewModel,
    devicesVm: DevicesViewModel
) {
    // scanning
    val total = 255
    val scanProgress by scanVm.scanProgress.collectAsState()

    // TODO: Clean scan results when screen shows up
    val results by scanVm.scanResults.collectAsState()

    val scanState by scanVm.scanState.collectAsState()

    val scanning = scanState == ScanState.Running

    Spacer(Modifier.height(8.dp))
    OutlinedButton(
        onClick = {
            if (!scanning) scanVm.startScan()
            else scanVm.cancelScan()
        },
        enabled = true,
        modifier = modifier.fillMaxWidth(0.75f)//.align(Alignment.CenterHorizontally)
    ) {
        if (scanning) {
            CircularProgressIndicator(
                progress = { scanProgress / total.toFloat() },
                modifier = Modifier.size(20.dp)
            )
            Spacer(Modifier.width(8.dp))
            Text(
                stringResource(
                    R.string.network_scan_running,
                    (scanProgress * 100 / total)
                )
            )
        } else {
            Text(stringResource(R.string.network_scan_button))
        }
    }

    Spacer(Modifier.height(16.dp))

    if (results.isNotEmpty()) {
        Text(
            stringResource(R.string.devices_found),
            style = MaterialTheme.typography.titleSmall
        )
        DetectedDevicesList(
            results = results,
            navController = navController,
            devicesVm = devicesVm
        )
    }
}