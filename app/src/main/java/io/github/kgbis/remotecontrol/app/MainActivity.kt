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
package io.github.kgbis.remotecontrol.app

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.lifecycle.viewmodel.compose.viewModel
import io.github.kgbis.remotecontrol.app.core.ViewModelFactory
import io.github.kgbis.remotecontrol.app.features.devices.DevicesViewModel
import io.github.kgbis.remotecontrol.app.features.settings.SettingsViewModel
import io.github.kgbis.remotecontrol.app.navigation.NavGraph
import io.github.kgbis.remotecontrol.app.ui.theme.RemotePcControlTheme
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Dispatchers

const val MIN_VERSION = "2.4.1"

class MainActivity : ComponentActivity() {

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            val app = application as RemotePcControlApp

            val dispatcher: CoroutineDispatcher = Dispatchers.Main

            val settingsVm: SettingsViewModel = viewModel(
                factory = ViewModelFactory {
                    SettingsViewModel(app, app.settingsRepository)
                }
            )

            val devicesVm: DevicesViewModel = viewModel(
                factory = ViewModelFactory {
                    DevicesViewModel(
                        application = app,
                        deviceRepository = app.devicesRepository,
                        settingsRepository = app.settingsRepository,
                        networkMonitor = app.networkMonitor,
                        appLifecycleObserver = app.appLifecycleObserver,
                        dispatcher = dispatcher
                    )
                }
            )

            val themeMode by settingsVm.colorScheme.collectAsState()

            RemotePcControlTheme(themeMode) {
                NavGraph(settingsVm, devicesVm)
            }
        }
    }
}
