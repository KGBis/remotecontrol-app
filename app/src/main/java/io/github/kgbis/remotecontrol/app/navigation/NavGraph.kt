package io.github.kgbis.remotecontrol.app.navigation

import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import io.github.kgbis.remotecontrol.app.features.about.AboutScreen
import io.github.kgbis.remotecontrol.app.ui.screens.AddDeviceEntryScreen
import io.github.kgbis.remotecontrol.app.ui.screens.EditDeviceScreen
import io.github.kgbis.remotecontrol.app.features.devices.ui.MainScreen
import io.github.kgbis.remotecontrol.app.features.devices.DevicesViewModel
import io.github.kgbis.remotecontrol.app.features.settings.SettingsViewModel

@Composable
fun NavGraph(settingsVm: SettingsViewModel) {
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = "main") {
        composable("main") {
            val devicesVm: DevicesViewModel = viewModel(navController.getBackStackEntry("main"))
            MainScreen(
                navController = navController,
                devicesVm = devicesVm,
                settingsVm = settingsVm
            )
        }

        composable("add_device") {
            val devicesVm: DevicesViewModel = viewModel(navController.getBackStackEntry("main"))
            AddDeviceEntryScreen(
                navController = navController,
                devicesVm = devicesVm
            )
        }

        composable("edit_device/{id}") { backStackEntry ->
            val id = backStackEntry.arguments?.getString("id")!!
            val devicesVm: DevicesViewModel = viewModel(navController.getBackStackEntry("main"))
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
