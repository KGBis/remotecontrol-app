package io.github.kgbis.remotecontrol.app.features.discovery.ui

import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import io.github.kgbis.remotecontrol.app.features.devices.DevicesViewModel
import io.github.kgbis.remotecontrol.app.features.discovery.DiscoveryViewModel

@Composable
fun ColumnScope.MDNSDiscoveryScreen(
    modifier: Modifier,
    navController: NavController,
    devicesVm: DevicesViewModel,
) {
    val discoveryVm: DiscoveryViewModel = viewModel()

    val devices by discoveryVm.devices.collectAsState()

    // observable state
    val state by discoveryVm.state.collectAsState()

    DiscoveryScreenContent(
        modifier = modifier,
        isDiscovering = state.isDiscovering,
        state = state,
        devices = devices, // transformDiscoveredToDevices(state.devices),
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
