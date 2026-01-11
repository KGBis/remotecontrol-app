package io.github.kgbis.remotecontrol.app.features.discovery.ui

import android.util.Log
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import io.github.kgbis.remotecontrol.app.features.discovery.model.DeviceTransformResult
import io.github.kgbis.remotecontrol.app.features.discovery.DeviceTransformer
import io.github.kgbis.remotecontrol.app.features.discovery.DiscoveryViewModel
import io.github.kgbis.remotecontrol.app.features.discovery.model.DiscoveredDevice
import io.github.kgbis.remotecontrol.app.features.devices.DevicesViewModel

@Composable
fun ColumnScope.MDNSDiscoveryScreen(
    modifier: Modifier,
    navController: NavController,
    devicesVm: DevicesViewModel,
) {
    val discoveryVm: DiscoveryViewModel = viewModel()

    // observable state
    val state by discoveryVm.state.collectAsState()

    DiscoveryScreenContent(
        modifier = modifier,
        isDiscovering = state.isDiscovering,
        state = state,
        devices = transformDiscoveredToDevices(state.devices),
        navController = navController,
        devicesVm = devicesVm,
        discoveryVm = discoveryVm
    )

    // Side effects
    DiscoverySideEffects(
        state = state,
        discoveryVm = discoveryVm
    )
}


private fun transformDiscoveredToDevices(
    discoveredServices: List<DiscoveredDevice>
): List<DeviceTransformResult> {
    Log.d("transformDiscoveredToDevices", "List<DiscoveredDevice> = $discoveredServices")
    return discoveredServices.map { discovered ->
        DeviceTransformer.transformToDevice(discovered)
    }
}
