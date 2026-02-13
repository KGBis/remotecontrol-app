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
