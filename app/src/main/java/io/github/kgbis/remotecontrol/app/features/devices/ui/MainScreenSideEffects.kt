package io.github.kgbis.remotecontrol.app.features.devices.ui

import androidx.compose.material3.SnackbarDuration
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import io.github.kgbis.remotecontrol.app.features.devices.DevicesViewModel

@Composable
fun MainScreenSideEffects(
    devicesVm: DevicesViewModel,
    showSnackbar: String?,
    snackbarHostState: SnackbarHostState,
    onSnackbarShown: () -> Unit
) {
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
