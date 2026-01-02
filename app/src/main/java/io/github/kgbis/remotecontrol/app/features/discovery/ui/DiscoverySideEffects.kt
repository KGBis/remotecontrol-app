package io.github.kgbis.remotecontrol.app.features.discovery.ui

import androidx.compose.runtime.*
import kotlinx.coroutines.delay
import android.util.Log
import io.github.kgbis.remotecontrol.app.features.discovery.DiscoveryViewModel
import io.github.kgbis.remotecontrol.app.features.discovery.model.DiscoveryState

@Composable
fun DiscoverySideEffects(
    state: DiscoveryState,
    onShowMessage: (String) -> Unit,
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
            onShowMessage("Discovery stopped after 5 seconds")
        }
    }

    // Show errors
    LaunchedEffect(state.error) {
        state.error?.let { error ->
            onShowMessage(error)
            // Limpiar el error después de mostrarlo
            delay(1000)
            discoveryVm.clearError()
        }
    }

    // Debug logging
    LaunchedEffect(state.discoveredServices.size) {
        if (state.discoveredServices.isNotEmpty()) {
            Log.d("Discovery", "Found ${state.discoveredServices.size} services")
        }
    }
}