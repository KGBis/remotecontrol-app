package com.example.remote.shutdown.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.remote.shutdown.R
import com.example.remote.shutdown.data.Device
import com.example.remote.shutdown.network.ScanState
import com.example.remote.shutdown.ui.components.DetectedDevicesList
import com.example.remote.shutdown.ui.components.ValidatingTextField
import com.example.remote.shutdown.util.Utils
import com.example.remote.shutdown.viewmodel.MainViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddOrEditDeviceScreen(
    navController: NavController, viewModel: MainViewModel,
    deviceToEdit: Device? = null
) {
    // val networkRangeDetector = NetworkRangeDetector()

    // Fields
    var name by remember {
        if (deviceToEdit == null) mutableStateOf("") else mutableStateOf(
            deviceToEdit.name
        )
    }
    var ip by remember {
        if (deviceToEdit == null) mutableStateOf("") else mutableStateOf(
            deviceToEdit.ip
        )
    }

    var mac by remember {
        if (deviceToEdit == null) mutableStateOf("") else mutableStateOf(
            deviceToEdit.mac
        )
    }

    // scanning
    val total = 255
    val scanProgress by viewModel.scanProgress.collectAsState()
    val results by viewModel.scanResults.collectAsState()
    val scanState by viewModel.scanState.collectAsState()

    val scanning = scanState == ScanState.Running

    // val scope = rememberCoroutineScope()

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        if (deviceToEdit == null) stringResource(R.string.add_device) else stringResource(
                            R.string.edit_device
                        )
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.back)
                        )
                    }
                }
            )
        }) { padding ->
        Column(
            Modifier
                .padding(padding)
                .padding(16.dp)
                .fillMaxWidth()
        ) {
            val modifier = Modifier
                .align(Alignment.CenterHorizontally)
                .fillMaxWidth(0.75f)

            OutlinedTextField(
                value = name,
                onValueChange = { name = it },
                modifier = modifier,
                label = {
                    Text(stringResource(R.string.device_name))
                }
            )

            ValidatingTextField(
                value = ip,
                onValueChange = { ip = it },
                label = stringResource(R.string.device_ip),
                modifier = modifier,
                validator = Utils::isValidIp,
                errorMessage = R.string.error_invalid_ip
            )

            ValidatingTextField(
                value = mac,
                onValueChange = { mac = it },
                label = stringResource(R.string.device_mac),
                modifier = modifier,
                validator = Utils::isValidMac,
                errorMessage = R.string.error_invalid_mac
            )
            Spacer(Modifier.height(8.dp))

            Button(
                modifier = Modifier
                    .fillMaxWidth(0.75f)
                    .align(Alignment.CenterHorizontally),
                onClick = {
                    if (name.isNotBlank() && ip.isNotBlank()) {
                        when (deviceToEdit) {
                            null -> viewModel.addDevice(Device(name, ip, mac))
                            else -> {
                                val updated = deviceToEdit.copy(name = name, ip = ip, mac = mac)
                                viewModel.updateDevice(deviceToEdit, updated)
                            }
                        }
                        navController.popBackStack()
                    }
                }) {
                Text(
                    if (deviceToEdit == null) stringResource(R.string.save_device) else stringResource(
                        R.string.update_device
                    )
                )
            }

            // Not editing -> Scan network part
            if (deviceToEdit == null) {
                Spacer(Modifier.height(8.dp))
                OutlinedButton(
                    onClick = {
                        if (!scanning) viewModel.startScan()
                        else viewModel.cancelScan()
                    },
                    enabled = true,
                    modifier = Modifier.fillMaxWidth(0.75f).align(Alignment.CenterHorizontally)
                ) {
                    if (scanning) {
                        CircularProgressIndicator(modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(8.dp))
                        // TODO Replace "click to cancel" with i18n!
                        Text("${stringResource(R.string.network_scan_running)} ${(scanProgress * 100 / total)}% - Click to cancel")
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
                        viewModel = viewModel
                    )
                }
            }
        }
    }
}
