package io.github.kgbis.remotecontrol.app.ui.screens

import android.util.Log
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
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import androidx.navigation.NavController
import io.github.kgbis.remotecontrol.app.R
import io.github.kgbis.remotecontrol.app.data.shutdownDelayOptions
import io.github.kgbis.remotecontrol.app.ui.components.AppTopBar
import io.github.kgbis.remotecontrol.app.ui.components.DeviceItem
import io.github.kgbis.remotecontrol.app.ui.components.ShutdownDelayDropdown
import io.github.kgbis.remotecontrol.app.viewmodel.MainViewModel
import kotlinx.coroutines.delay

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(navController: NavController, viewModel: MainViewModel) {
    val devices by viewModel.devices.collectAsState()

    var showSnackbar by remember { mutableStateOf<String?>(null) }
    val snackbarHostState = remember { SnackbarHostState() }

    var pulltoRefreshIsRefreshing by remember { mutableStateOf(false) }
    val pulltoRefreshState = rememberPullToRefreshState()

    val context = LocalContext.current

    // for shutdown
    val delay by viewModel.shutdownDelay.collectAsState()
    val unit by viewModel.shutdownUnit.collectAsState()

    // for about/settings
    /*var sheetOpen by remember { mutableStateOf(false) }*/

    // for auto-refresh
    val autorefreshEnabled by viewModel.autoRefreshEnabled.collectAsState()
    val autorefreshInterval by viewModel.autoRefreshInterval.collectAsState()

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

    // to refresh status when changes occur in device list
    LaunchedEffect(devices) {
        if (devices.isNotEmpty()) {
            viewModel.refreshStatuses()
        }
    }

    // To refresh automatically device list every REFRESH_DELAY_MS
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    LaunchedEffect(Unit) {
        lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
            while (true) {
                if (autorefreshEnabled) {
                    Log.d("LaunchedEffect", "Triggered refresh statuses...")
                    val ini = System.currentTimeMillis()
                    viewModel.refreshStatuses()
                    val end = System.currentTimeMillis()
                    Log.d("LaunchedEffect", "Refresh took ${end - ini} milliseconds")
                }
                Log.d("LaunchedEffect", "waiting $autorefreshInterval seconds")
                delay(autorefreshInterval.toLong() * 1000) // millis here
            }
        }
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            AppTopBar(
                navController = navController,
                viewModel = viewModel
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
                viewModel.refreshStatuses()
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
                ShutdownDelayDropdown(viewModel = viewModel, options = shutdownDelayOptions)

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
                            DeviceItem(
                                device = device,
                                viewModel = viewModel,
                                onShutdown = {
                                    viewModel.sendShutdownCommand(
                                        device,
                                        delay,
                                        unit.name
                                    ) { success ->
                                        showSnackbar =
                                            if (success)
                                                context.getString(
                                                    R.string.device_shutdown_sent,
                                                    device.name
                                                )
                                            else
                                                context.getString(
                                                    R.string.device_shutdown_sent_error,
                                                    device.name
                                                )
                                    }
                                },
                                onWake = {
                                    viewModel.wakeOnLan(device) { success ->
                                        showSnackbar =
                                            if (success)
                                                context.getString(
                                                    R.string.device_wol_sent,
                                                    device.name
                                                )
                                            else
                                                context.getString(
                                                    R.string.device_wol_sent_error,
                                                    device.name
                                                )
                                    }
                                },
                                onDelete = {
                                    showSnackbar = context.getString(R.string.device_removed)
                                    viewModel.removeDevice(device)
                                },
                                onEdit = {
                                    navController.navigate("edit_device/${device.ip}")
                                }
                            )
                        }
                    }
                }

                /*if (sheetOpen) {
                    SettingsBottomSheet(
                        enabled = autorefreshEnabled,
                        interval = autorefreshInterval.toFloat(),
                        onEnabledChange = { viewModel.setAutoRefreshEnabled(it) },
                        onIntervalChange = { viewModel.setAutoRefreshInterval(it) },
                        onAboutClick = {
                            sheetOpen = false
                            navController.navigate("about")
                        },
                        onDismiss = { sheetOpen = false }
                    )
                }*/
            }
        }
    }
}
