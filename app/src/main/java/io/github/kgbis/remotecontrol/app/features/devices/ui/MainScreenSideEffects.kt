package io.github.kgbis.remotecontrol.app.features.devices.ui

import android.util.Log
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
    settingsVm: SettingsViewModel
) {
    val autorefreshEnabled by settingsVm.autoRefreshEnabled.collectAsState()
    val autorefreshInterval by settingsVm.autoRefreshInterval.collectAsState()
    val devices by devicesVm.devices.collectAsState()

    DisposableEffect(autorefreshEnabled) {
        if (autorefreshEnabled) {
            Log.d("DisposableEffect", "startAutoProbe() on autorefresh enabled")
            devicesVm.startAutoProbe(autorefreshInterval)
        }

        onDispose {
            Log.d("DisposableEffect", "stopAutoProbe() on dispose")
            devicesVm.stopAutoProbe()
        }
    }

    // to refresh status when changes occur in device list
    LaunchedEffect(devices) {
        if (autorefreshEnabled && devices.isNotEmpty()) {
            Log.d("LaunchedEffect", "probeDevices() on list change")
            devicesVm.probeDevices()
        }
    }
}


/*
@Composable
fun MainScreenSideEffects(
    devicesVm: DevicesViewModel,
    settingsVm: SettingsViewModel
) {
    val autorefreshEnabled by settingsVm.autoRefreshEnabled.collectAsState()
    val autorefreshInterval = 60 // by settingsVm.autoRefreshInterval.collectAsState()

    val devices by devicesVm.devices.collectAsState()

    // to refresh status when changes occur in device list
    if (autorefreshEnabled) {
        LaunchedEffect(devices) {
            if (devices.isNotEmpty()) {
                devicesVm.probeDevices()
            }
        }
    }

    // To refresh automatically device list every REFRESH_DELAY_MS
    val lifecycle = LocalLifecycleOwner.current.lifecycle
    LaunchedEffect(Unit) {
        lifecycle.repeatOnLifecycle(Lifecycle.State.STARTED) {
            while (true) {
                if (autorefreshEnabled) {
                    devicesVm.probeDevices()
                }
                Log.d("LaunchedEffect", "waiting $autorefreshInterval seconds after list refresh")
                delay(autorefreshInterval.toLong() * 1000) // millis here
            }
        }
    }
}*/
