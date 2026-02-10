package io.github.kgbis.remotecontrol.app.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import io.github.kgbis.remotecontrol.app.R
import io.github.kgbis.remotecontrol.app.core.model.Device
import io.github.kgbis.remotecontrol.app.core.model.toFormState
import io.github.kgbis.remotecontrol.app.features.devices.DevicesViewModel
import io.github.kgbis.remotecontrol.app.features.devices.model.DeviceFormMode
import io.github.kgbis.remotecontrol.app.features.devices.model.DeviceFormState
import io.github.kgbis.remotecontrol.app.features.devices.model.applyTo
import io.github.kgbis.remotecontrol.app.features.domain.ConflictResult
import io.github.kgbis.remotecontrol.app.features.domain.DeviceConflictChecker
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditDeviceScreen(
    navController: NavHostController,
    devicesVm: DevicesViewModel,
    idToEdit: String
) {
    // Device list
    val devices by devicesVm.devices.collectAsState()

    val device = devicesVm.getDeviceById(UUID.fromString(idToEdit))
    var formState by remember { mutableStateOf(device?.toFormState() ?: DeviceFormState()) }

    var conflict by remember { mutableStateOf<ConflictResult?>(null) }
    var pendingDevice by remember { mutableStateOf<Device?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(stringResource(R.string.edit_device))
                },
                navigationIcon = {
                    IconButton(onClick = {
                        navController.popBackStack()
                    }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = stringResource(R.string.back)
                        )
                    }
                }
            )
        }
    ) { paddingValues ->
        Column(
            modifier = Modifier
                .padding(paddingValues)
                .fillMaxWidth()
                .verticalScroll(rememberScrollState()),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            val conflictChecker = DeviceConflictChecker(devices)

            AddEditDeviceScreen(
                mode = DeviceFormMode.EDIT,
                state = formState,
                onStateChange = { formState = it },
                onSave = {
                    val updatedDevice = formState.applyTo(device!!)
                    val result = conflictChecker.check(formState, device.id)

                    if (result == ConflictResult.None) {
                        devicesVm.updateDevice(device, updatedDevice)
                        navController.popBackStack()
                    } else {
                        pendingDevice = updatedDevice
                        conflict = result
                    }
                }

            )

            conflict?.let { result ->
                DeviceConflictDialog(
                    conflict = result,
                    onConfirm = {
                        pendingDevice?.let {
                            devicesVm.updateDevice(device!!, it)
                            navController.popBackStack()
                        }
                        pendingDevice = null
                        conflict = null
                    },
                    onDismiss = {
                        pendingDevice = null
                        conflict = null
                    }
                )
            }


        }
    }
}