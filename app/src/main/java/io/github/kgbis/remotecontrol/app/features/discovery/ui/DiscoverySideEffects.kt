/*
 * Remote PC Control
 * Copyright (C) 2026 Enrique García (https://github.com/KGBis)
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 */
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