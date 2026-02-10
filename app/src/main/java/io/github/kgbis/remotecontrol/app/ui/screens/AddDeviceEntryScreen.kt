package io.github.kgbis.remotecontrol.app.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.WifiFind
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import io.github.kgbis.remotecontrol.app.R
import io.github.kgbis.remotecontrol.app.core.model.Device
import io.github.kgbis.remotecontrol.app.features.devices.DevicesViewModel
import io.github.kgbis.remotecontrol.app.features.devices.model.AddDeviceMode
import io.github.kgbis.remotecontrol.app.features.devices.model.DeviceFormMode
import io.github.kgbis.remotecontrol.app.features.devices.model.DeviceFormState
import io.github.kgbis.remotecontrol.app.features.devices.model.toNewDevice
import io.github.kgbis.remotecontrol.app.features.discovery.ui.MDNSDiscoveryScreen
import io.github.kgbis.remotecontrol.app.features.domain.ConflictResult
import io.github.kgbis.remotecontrol.app.features.domain.DeviceConflictChecker

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddDeviceEntryScreen(
    navController: NavController,
    devicesVm: DevicesViewModel,
) {
    var mode by remember { mutableStateOf(AddDeviceMode.NONE) }
    var formState by remember { mutableStateOf(DeviceFormState()) }

    // Device list
    val devices by devicesVm.devices.collectAsState()

    var conflict by remember { mutableStateOf<ConflictResult?>(null) }
    var pendingDevice by remember { mutableStateOf<Device?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(stringResource(R.string.add_device))
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
    ) { padding ->
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(padding)
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {

            // ---------- DISCOVER (main) ----------
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { mode = AddDeviceMode.DISCOVER },
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.primaryContainer
                ),
                elevation = CardDefaults.cardElevation(6.dp),
                shape = RoundedCornerShape(16.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.WifiFind,
                        contentDescription = null,
                        modifier = Modifier.size(40.dp),
                        tint = MaterialTheme.colorScheme.onPrimaryContainer
                    )

                    Spacer(modifier = Modifier.width(16.dp))

                    Column {
                        Text(
                            text = stringResource(R.string.network_scan_title),
                            style = MaterialTheme.typography.titleMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = stringResource(R.string.network_scan_desc),
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onPrimaryContainer.copy(alpha = 0.8f)
                        )
                    }
                }
            }


            // ---------- MANUAL (secondary) ----------
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { mode = AddDeviceMode.MANUAL },
                colors = CardDefaults.cardColors(
                    containerColor = MaterialTheme.colorScheme.surfaceVariant
                ),
                elevation = CardDefaults.cardElevation(2.dp),
                shape = RoundedCornerShape(16.dp)
            ) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(20.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = null,
                        modifier = Modifier.size(32.dp),
                        tint = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    Spacer(modifier = Modifier.width(16.dp))

                    Column {
                        Text(
                            text = stringResource(R.string.manual_edit_title),
                            style = MaterialTheme.typography.titleSmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = stringResource(R.string.manual_edit_desc),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.8f)
                        )
                    }
                }
            }

            when (mode) {
                AddDeviceMode.DISCOVER -> {
                    MDNSDiscoveryScreen(
                        modifier = Modifier.align(Alignment.CenterHorizontally),
                        navController = navController,
                        devicesVm = devicesVm,
                    )
                }

                AddDeviceMode.MANUAL -> {
                    val conflictChecker = DeviceConflictChecker(devices)

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .verticalScroll(rememberScrollState()),
                        verticalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        AddEditDeviceScreen(
                            mode = DeviceFormMode.CREATE,
                            state = formState,
                            onStateChange = { formState = it }
                        ) {
                            val result = conflictChecker.check(formState, currentId = null)
                            val device = formState.toNewDevice()

                            if (result == ConflictResult.None) {
                                devicesVm.addDevice(device)
                                navController.popBackStack()
                            } else {
                                conflict = result
                                pendingDevice = device
                            }

                        }

                        conflict?.let { result ->
                            DeviceConflictDialog(
                                conflict = result,
                                onConfirm = {
                                    pendingDevice?.let {
                                        devicesVm.addDevice(it)
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

                AddDeviceMode.NONE -> {
                    // Do nothing
                }
            }
        }
    }
}
