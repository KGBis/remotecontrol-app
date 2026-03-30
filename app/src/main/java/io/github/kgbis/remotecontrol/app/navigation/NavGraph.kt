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
package io.github.kgbis.remotecontrol.app.navigation

import androidx.compose.runtime.Composable
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import io.github.kgbis.remotecontrol.app.features.about.AboutScreen
import io.github.kgbis.remotecontrol.app.features.devices.DevicesViewModel
import io.github.kgbis.remotecontrol.app.features.devices.ui.MainScreen
import io.github.kgbis.remotecontrol.app.features.settings.SettingsViewModel
import io.github.kgbis.remotecontrol.app.ui.screens.AddDeviceEntryScreen
import io.github.kgbis.remotecontrol.app.ui.screens.EditDeviceScreen

@Composable
fun NavGraph(settingsVm: SettingsViewModel, devicesVm: DevicesViewModel) {
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = "main") {
        composable("main") {
            MainScreen(
                navController = navController,
                devicesVm = devicesVm,
                settingsVm = settingsVm
            )
        }

        composable("add_device") {
            AddDeviceEntryScreen(
                navController = navController,
                devicesVm = devicesVm
            )
        }

        composable("edit_device/{id}") { backStackEntry ->
            val id = backStackEntry.arguments?.getString("id")!!
            EditDeviceScreen(
                navController = navController,
                devicesVm = devicesVm,
                idToEdit = id
            )
        }

        composable("about_screen") {
            AboutScreen(navController)
        }
    }
}
