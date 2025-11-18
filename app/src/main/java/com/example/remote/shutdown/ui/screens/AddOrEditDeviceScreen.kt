package com.example.remote.shutdown.ui.screens

import android.content.Context
import android.util.Log
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.navigation.NavController
import com.example.remote.shutdown.R
import com.example.remote.shutdown.data.Device
import com.example.remote.shutdown.network.NetworkRangeDetector
import com.example.remote.shutdown.network.NetworkScanner.scanLocalNetwork
import com.example.remote.shutdown.ui.components.ValidatingTextField
import com.example.remote.shutdown.util.Validators
import com.example.remote.shutdown.viewmodel.MainViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddOrEditDeviceScreen(
    navController: NavController, viewModel: MainViewModel, context: Context,
    deviceToEdit: Device? = null
) {
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
    var scanning by remember { mutableStateOf(false) }
    var results by remember { mutableStateOf(listOf<Device>()) }
    val scope = rememberCoroutineScope()
    val networkRangeDetector = NetworkRangeDetector(context)

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
                .fillMaxWidth(0.5f)

            OutlinedTextField(
                value = name,
                textStyle = TextStyle(fontSize = 14.sp),
                onValueChange = { name = it },
                modifier = modifier,
                label = {
                    Text(stringResource(R.string.device_name))
                }
            )
            Spacer(Modifier.height(8.dp))

            ValidatingTextField(
                value = ip,
                onValueChange = { ip = it },
                label = stringResource(R.string.device_ip),
                modifier = modifier,
                validator = Validators::isValidIp,
                errorMessage = R.string.error_invalid_ip
            )

            Spacer(Modifier.height(8.dp))

            ValidatingTextField(
                value = mac,
                onValueChange = { mac = it },
                label = stringResource(R.string.device_mac),
                modifier = modifier,
                validator = Validators::isValidMac,
                errorMessage = R.string.error_invalid_mac
            )
            Spacer(Modifier.height(16.dp))

            Button(
                modifier = Modifier
                    .fillMaxWidth(0.75f)
                    .align(Alignment.CenterHorizontally),
                onClick = {
                    if (name.isNotBlank() && ip.isNotBlank()) {
                        if (deviceToEdit == null) {
                            // ADD
                            viewModel.addDevice(Device(name, ip, mac))
                        } else {
                            // UPDATE
                            val updated = deviceToEdit.copy(name = name, ip = ip, mac = mac)
                            Log.i(
                                "AddOrEditDevice-Update",
                                "deviceToEdit -> $deviceToEdit\nUpdated object -> $updated"
                            )
                            // TODO call the new update fun
                            viewModel.addDevice(updated)
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

            if (deviceToEdit == null) {
                Spacer(Modifier.height(24.dp))
                OutlinedButton(
                    enabled = !scanning,
                    onClick = {
                        scanning = true
                        scope.launch {
                            val pair = startScan(networkRangeDetector, viewModel)
                            results = pair.first
                            scanning = pair.second
                        }
                    },
                    modifier = Modifier
                        .fillMaxWidth(0.75f)
                        .align(Alignment.CenterHorizontally)
                ) {
                    if (scanning) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.dp
                        )
                        Spacer(Modifier.width(8.dp))
                    }
                    Text(
                        if (scanning) stringResource(R.string.network_scan_running) else stringResource(
                            R.string.network_scan_button
                        )
                    )
                }

                Spacer(Modifier.height(16.dp))

                if (results.isNotEmpty()) {
                    Text("Dispositivos detectados:", style = MaterialTheme.typography.titleSmall)
                    LazyColumn {
                        items(results.size) { i ->
                            val d = results[i]
                            ListItem(
                                headlineContent = { Text(d.name) },
                                supportingContent = { Text(d.ip) },
                                modifier = Modifier.clickable {
                                    viewModel.addDevice(d)
                                    navController.popBackStack()
                                }
                            )
                        }
                    }
                }
            }
        }
    }
}

private suspend fun startScan(
    networkRangeDetector: NetworkRangeDetector,
    viewModel: MainViewModel
): Pair<List<Device>, Boolean> {
    // list of router IPs
    val knownRouters = viewModel.routerIps

    // for time lapse calc
    val b = System.currentTimeMillis()

    // detect local netwotrk and scan it
    val networkRange = networkRangeDetector.getLocalNetworkRange()
    val results = scanLocalNetwork(baseIp = networkRange ?: "192.168.1", maxConcurrent = 30)

    // Try to find router(s) among the results
    results.forEach { device ->
        if (knownRouters.contains(device.ip)) {
            device.name = "Router"
        }
    }

    Log.i("Scan", "Time to scan subnet -> ${System.currentTimeMillis() - b} millis")
    return Pair(results, false)
}
