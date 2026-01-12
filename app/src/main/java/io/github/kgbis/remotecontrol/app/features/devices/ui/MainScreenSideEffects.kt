package io.github.kgbis.remotecontrol.app.features.devices.ui

import android.util.Log
import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import io.github.kgbis.remotecontrol.app.features.devices.DevicesViewModel
import io.github.kgbis.remotecontrol.app.features.settings.SettingsViewModel

@Composable
fun MainScreenSideEffects(
    devicesVm: DevicesViewModel,
    settingsVm: SettingsViewModel,
    showSnackbar: String?,
    snackbarHostState: SnackbarHostState,
    onSnackbarShown: () -> Unit
) {
    val autorefreshEnabled by settingsVm.autoRefreshEnabled.collectAsState()
    val devices by devicesVm.devices.collectAsState()

    // to refresh status when changes occur in device list
    LaunchedEffect(devices) {
        if (autorefreshEnabled && devices.isNotEmpty()) {
            Log.d("LaunchedEffect", "probeDevices() on list change")
            devicesVm.probeDevices()
        }
    }

    // to start/stop autorefresh
    DisposableEffect(Unit) {
        devicesVm.setMainScreenVisible(true)
        onDispose {
            devicesVm.setMainScreenVisible(false)
        }
    }

    // Snackbar autoclose
    LaunchedEffect(showSnackbar) {
        showSnackbar?.let {
            snackbarHostState.showSnackbar(
                message = it,
                actionLabel = "OK",
                duration = SnackbarDuration.Long
            )
            onSnackbarShown()
        }
    }
}
