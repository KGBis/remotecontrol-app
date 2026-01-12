package io.github.kgbis.remotecontrol.app.features.devices.ui

import android.content.Context
import androidx.annotation.StringRes
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import io.github.kgbis.remotecontrol.app.R
import io.github.kgbis.remotecontrol.app.core.model.Device
import io.github.kgbis.remotecontrol.app.core.model.isRenderable
import io.github.kgbis.remotecontrol.app.features.devices.DevicesViewModel
import io.github.kgbis.remotecontrol.app.features.devices.model.shutdownDelayOptions
import io.github.kgbis.remotecontrol.app.features.settings.SettingsViewModel
import io.github.kgbis.remotecontrol.app.ui.components.ShutdownDelayDropdown
import java.time.temporal.ChronoUnit

@Composable
fun BoxScope.MainScreenContent(
    navController: NavController,
    devicesVm: DevicesViewModel,
    settingsVm: SettingsViewModel,
    onShowSnackbar: (String) -> Unit
) {
    // Device list
    val devices by devicesVm.devices.collectAsState()

    // if connected to same network as devices
    val notInSameNetwork by devicesVm.notInSameNetwork.collectAsState()

    // Shutdown delay and unit. i.e. 20 SECONDS
    val delay by settingsVm.shutdownDelay.collectAsState()
    val unit by settingsVm.shutdownUnit.collectAsState()

    Column(
        Modifier
            .padding(16.dp)
            .fillMaxWidth()
            .align(Alignment.TopCenter)
    ) {
        // Custom dropdown for the shutdown delay option list
        Text(
            stringResource(R.string.shutdown_delay),
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier
                .padding(bottom = 8.dp)
                .fillMaxWidth(),
            textAlign = TextAlign.Center
        )
        ShutdownDelayDropdown(viewModel = settingsVm, options = shutdownDelayOptions)

        Spacer(Modifier.height(16.dp))

        if (devices.isEmpty()) {
            Text(
                stringResource(R.string.no_devices_yet),
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.bodyMedium
            )
        } else {
            DevicesList(
                devices = devices,
                devicesVm = devicesVm,
                delay = delay,
                unit = unit,
                navController = navController,
                onShowSnackbar = onShowSnackbar
            )

            if (notInSameNetwork) {
                Text(
                    stringResource(R.string.not_in_same_network),
                    textAlign = TextAlign.Center,
                    fontWeight = FontWeight.Bold
                )
            }
        }
    }
}

data class ActionMessage(
    val success: Boolean,
    @param:StringRes val successMsg: Int,
    @param:StringRes val errorMsg: Int,
    val param: String
)

fun ActionMessage.resolve(context: Context): String =
    if (success)
        context.getString(successMsg, param)
    else
        context.getString(errorMsg, param)


@Composable
private fun DevicesList(
    devices: List<Device>,
    devicesVm: DevicesViewModel,
    delay: Int,
    unit: ChronoUnit,
    navController: NavController,
    onShowSnackbar: (String) -> Unit
) {
    val context = LocalContext.current

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
                            val msg = ActionMessage(
                                success = success,
                                successMsg = R.string.device_shutdown_sent,
                                errorMsg = R.string.device_shutdown_sent_error,
                                param = device.hostname
                            ).resolve(context)

                            onShowSnackbar(msg)
                        }
                    },
                    onCancel = {
                        devicesVm.sendCancelShutdownCommand(device) { success ->
                            val msg = ActionMessage(
                                success = success,
                                successMsg = R.string.device_shutdown_cancel_sent,
                                errorMsg = R.string.device_shutdown_sent_error,
                                param = device.hostname
                            ).resolve(context)

                            onShowSnackbar(msg)
                        }
                    },
                    onWake = {
                        devicesVm.wakeOnLan(device) { success ->
                            val msg = ActionMessage(
                                success = success,
                                successMsg = R.string.device_wol_sent,
                                errorMsg = R.string.device_wol_sent_error,
                                param = device.hostname
                            ).resolve(context)

                            onShowSnackbar(msg)
                        }
                    },
                    onDelete = {
                        onShowSnackbar(context.getString(R.string.device_removed))
                        devicesVm.removeDevice(device)
                    },
                    onEdit = {
                        navController.navigate("edit_device/${device.id}")
                    }
                )
        }
    }
}