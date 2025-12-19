package io.github.kgbis.remotecontrol.app.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import io.github.kgbis.remotecontrol.app.R
import io.github.kgbis.remotecontrol.app.data.Device
import io.github.kgbis.remotecontrol.app.ui.components.ValidatingTextField
import io.github.kgbis.remotecontrol.app.util.Utils
import io.github.kgbis.remotecontrol.app.viewmodel.DevicesViewModel
import io.github.kgbis.remotecontrol.app.viewmodel.ScanViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddOrEditDeviceScreen(
    navController: NavController,
    devicesVm: DevicesViewModel,
    scanVm: ScanViewModel = viewModel(),
    ipToEdit: String? = null
) {
    val deviceToEdit = if (ipToEdit == null) null else devicesVm.getDeviceByIp(ipToEdit)

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

    fun saveUpdate() {
        if (name.isNotBlank() && ip.isNotBlank()) {
            when (deviceToEdit) {
                null -> devicesVm.addDevice(Device(name, ip, mac))
                else -> {
                    val updated = deviceToEdit.copy(name = name, ip = ip, mac = mac)
                    devicesVm.updateDevice(deviceToEdit, updated)
                }
            }
            navController.popBackStack()
        }
    }

    var showSnackbar by remember { mutableStateOf<String?>(null) }
    val snackbarHostState = remember { SnackbarHostState() }

    // Snackbar autoclose
    LaunchedEffect(showSnackbar) {
        showSnackbar?.let { msg ->
            snackbarHostState.showSnackbar(
                message = msg,
                actionLabel = "OK",
                duration = SnackbarDuration.Long
            )

            @Suppress("AssignedValueIsNeverRead")
            showSnackbar = null
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
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
                    IconButton(onClick = {
                        scanVm.cancelScan()
                        navController.popBackStack()
                    }) {
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
                onClick = { saveUpdate() }) {
                Text(
                    if (deviceToEdit == null) stringResource(R.string.save_device) else stringResource(
                        R.string.update_device
                    )
                )
            }

            // Not editing -> Scan network part
            if (deviceToEdit == null) {
                /*NetworkScannerSection(
                    navController = navController,
                    modifier = modifier,
                    scanVm = scanVm,
                    devicesVm = devicesVm
                )*/
                // SimpleMDNSDiscovery()
                MDNSDiscoveryScreen(
                    navController = navController,
                    devicesVm = devicesVm,
                    onShowMessage = { showSnackbar = it }
                )
            }
        }
    }
}
