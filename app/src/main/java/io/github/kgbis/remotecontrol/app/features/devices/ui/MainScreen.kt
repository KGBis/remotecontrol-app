package io.github.kgbis.remotecontrol.app.features.devices.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Text
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import io.github.kgbis.remotecontrol.app.R
import io.github.kgbis.remotecontrol.app.core.model.isRenderable
import io.github.kgbis.remotecontrol.app.features.devices.model.shutdownDelayOptions
import io.github.kgbis.remotecontrol.app.ui.components.AppTopBar
import io.github.kgbis.remotecontrol.app.ui.components.ShutdownDelayDropdown
import io.github.kgbis.remotecontrol.app.features.devices.DevicesViewModel
import io.github.kgbis.remotecontrol.app.features.settings.SettingsViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    navController: NavController,
    devicesVm: DevicesViewModel,
    settingsVm: SettingsViewModel = viewModel()
) {
    val devices by devicesVm.devices.collectAsState()

    var showSnackbar by remember { mutableStateOf<String?>(null) }
    val snackbarHostState = remember { SnackbarHostState() }

    var pulltoRefreshIsRefreshing by remember { mutableStateOf(false) }
    val pulltoRefreshState = rememberPullToRefreshState()

    val context = LocalContext.current

    // for shutdown
    val delay by settingsVm.shutdownDelay.collectAsState()
    val unit by settingsVm.shutdownUnit.collectAsState()

    MainScreenSideEffects(devicesVm, settingsVm)

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
            AppTopBar(
                navController = navController,
                settingsVm = settingsVm
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { navController.navigate("add_device") }
            ) {
                Icon(Icons.Default.Add, contentDescription = stringResource(R.string.add_device))
            }
        }
    ) { padding ->
        PullToRefreshBox(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize(),
            state = pulltoRefreshState,
            isRefreshing = pulltoRefreshIsRefreshing,
            onRefresh = {
                @Suppress("AssignedValueIsNeverRead")
                pulltoRefreshIsRefreshing = true
                devicesVm.probeDevices() //refreshStatuses()
                @Suppress("AssignedValueIsNeverRead")
                pulltoRefreshIsRefreshing = false
            }
        ) {
            Column(
                Modifier
                    .padding(16.dp)
                    .fillMaxWidth()
                    .align(Alignment.TopCenter)
            ) {
                Text(
                    stringResource(R.string.shutdown_delay),
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier
                        .padding(bottom = 8.dp)
                        .fillMaxWidth(),
                    textAlign = TextAlign.Center
                )
                // Custom dropdown for the shutdown delay option list
                ShutdownDelayDropdown(viewModel = settingsVm, options = shutdownDelayOptions)

                Spacer(Modifier.height(16.dp))

                // List of added devices or "empty list" text
                if (devices.isEmpty()) {
                    Text(
                        stringResource(R.string.no_devices_yet),
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Center,
                        style = MaterialTheme.typography.bodyMedium
                    )
                } else {
                    LazyColumn {
                        items(devices.size) { index ->
                            val device = devices[index]
                            if (device.isRenderable())
                                DeviceListItem(
                                    device = device,
                                    devicesVm = devicesVm,
                                    onShutdown = {
                                        devicesVm.sendShutdownCommand(
                                            device,
                                            delay,
                                            unit.name
                                        ) { success ->
                                            showSnackbar =
                                                if (success) {
                                                    context.getString(
                                                        R.string.device_shutdown_sent,
                                                        device.hostname
                                                    )
                                                    //
                                                } else
                                                    context.getString(
                                                        R.string.device_shutdown_sent_error,
                                                        device.hostname
                                                    )
                                        }
                                    },
                                    onCancel = {
                                        devicesVm.sendCancelShutdownCommand(device) { success ->
                                            showSnackbar = if (success) {
                                                context.getString(
                                                    R.string.device_shutdown_cancel_sent,
                                                    device.hostname
                                                )
                                                //
                                            } else
                                                context.getString(
                                                    R.string.device_shutdown_sent_error,
                                                    device.hostname
                                                )
                                        }
                                    },
                                    onWake = {
                                        devicesVm.wakeOnLan(device) { success ->
                                            showSnackbar =
                                                if (success)
                                                    context.getString(
                                                        R.string.device_wol_sent,
                                                        device.hostname
                                                    )
                                                else
                                                    context.getString(
                                                        R.string.device_wol_sent_error,
                                                        device.hostname
                                                    )
                                        }
                                    },
                                    onDelete = {
                                        showSnackbar = context.getString(R.string.device_removed)
                                        devicesVm.removeDevice(device)
                                    },
                                    onEdit = {
                                        navController.navigate("edit_device/${device.id}")
                                    }
                                )
                        }
                    }
                }
            }
        }
    }
}
