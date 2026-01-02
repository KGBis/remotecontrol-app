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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavHostController
import io.github.kgbis.remotecontrol.app.R
import io.github.kgbis.remotecontrol.app.features.devices.model.DeviceFormMode
import io.github.kgbis.remotecontrol.app.features.devices.model.DeviceFormState
import io.github.kgbis.remotecontrol.app.features.devices.model.toDevice
import io.github.kgbis.remotecontrol.app.core.model.toFormState
import io.github.kgbis.remotecontrol.app.features.devices.DevicesViewModel
import java.util.UUID

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun EditDeviceScreen(
    navController: NavHostController,
    devicesVm: DevicesViewModel,
    idToEdit: String
) {
    val device = devicesVm.getDeviceById(UUID.fromString(idToEdit))
    var formState by remember { mutableStateOf(device?.toFormState() ?: DeviceFormState()) }

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
            AddEditDeviceScreen(
                mode = DeviceFormMode.EDIT,
                state = formState,
                onStateChange = { formState = it },
                onSave = {
                    val updatedDevice = formState.toDevice()
                    devicesVm.updateDevice(device!!, updatedDevice)
                    navController.popBackStack()
                }
            )
        }
    }
}