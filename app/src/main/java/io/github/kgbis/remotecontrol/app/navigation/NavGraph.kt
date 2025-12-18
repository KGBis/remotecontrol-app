package io.github.kgbis.remotecontrol.app.navigation

import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import io.github.kgbis.remotecontrol.app.ui.screens.AboutScreen
import io.github.kgbis.remotecontrol.app.ui.screens.AddOrEditDeviceScreen
import io.github.kgbis.remotecontrol.app.ui.screens.MainScreen
import io.github.kgbis.remotecontrol.app.viewmodel.DevicesViewModel
import io.github.kgbis.remotecontrol.app.viewmodel.KeysViewModel

@Composable
fun NavGraph() {
    val navController = rememberNavController()

    NavHost(navController = navController, startDestination = "main") {
        composable("main") {
            val devicesVm: DevicesViewModel = viewModel(navController.getBackStackEntry("main"))
            MainScreen(navController, devicesVm)
        }
        composable("add_device") {
            val devicesVm: DevicesViewModel = viewModel(navController.getBackStackEntry("main"))
            AddOrEditDeviceScreen(navController = navController, devicesVm = devicesVm)
        }

        composable("edit_device/{ip}") { backStackEntry ->
            val ip = backStackEntry.arguments?.getString("ip")!!
            val devicesVm: DevicesViewModel = viewModel(navController.getBackStackEntry("main"))
            AddOrEditDeviceScreen(
                navController = navController,
                devicesVm = devicesVm,
                ipToEdit = ip
            )
        }

        composable("about_screen") {
            AboutScreen(navController)
        }


    }
}
