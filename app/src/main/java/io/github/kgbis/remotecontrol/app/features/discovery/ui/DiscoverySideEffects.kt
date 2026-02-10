package io.github.kgbis.remotecontrol.app.features.discovery.ui

import android.util.Log
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import io.github.kgbis.remotecontrol.app.features.discovery.DiscoveryViewModel
import io.github.kgbis.remotecontrol.app.features.discovery.model.DiscoveryState
import kotlinx.coroutines.delay

@Composable
fun DiscoverySideEffects(
    state: DiscoveryState,
    discoveryVm: DiscoveryViewModel
) {
    // Autostart
    DisposableEffect(Unit) {
        discoveryVm.startDiscovery()

        onDispose {
            discoveryVm.stopDiscovery()
        }
    }

    // Auto-stop after 5 secs
    LaunchedEffect(state.isDiscovering) {
        if (state.isDiscovering) {
            delay(5000L)
            discoveryVm.stopDiscovery()
        }
    }

    // Debug logging
    LaunchedEffect(state.discoveredServices.size) {
        if (state.discoveredServices.isNotEmpty()) {
            Log.d("Discovery", "Found ${state.discoveredServices.size} services")
        }
    }
}