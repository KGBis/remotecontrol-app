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
 *
 * SPDX-License-Identifier: GPL-3.0-or-later
 */
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
