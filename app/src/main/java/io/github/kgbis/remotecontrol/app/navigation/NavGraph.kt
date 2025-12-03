package io.github.kgbis.remotecontrol.app.navigation

import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import io.github.kgbis.remotecontrol.app.ui.screens.AboutScreen
import io.github.kgbis.remotecontrol.app.ui.screens.AddOrEditDeviceScreen
import io.github.kgbis.remotecontrol.app.ui.screens.MainScreen
import io.github.kgbis.remotecontrol.app.viewmodel.MainViewModel

@Composable
fun NavGraph() {
    val navController = rememberNavController()

    // shared ViewModel
    val mainViewModel: MainViewModel = viewModel()

    NavHost(navController = navController, startDestination = "main") {
        composable("main") {
            MainScreen(navController, mainViewModel)
        }
        composable("add_device") {
            AddOrEditDeviceScreen(navController, mainViewModel)
        }

        composable("edit_device/{ip}") { backStackEntry ->
            val ip = backStackEntry.arguments?.getString("ip")!!
            val device = mainViewModel.getDeviceByIp(ip)
            AddOrEditDeviceScreen(
                navController = navController,
                viewModel = mainViewModel,
                deviceToEdit = device
            )
        }

        composable("about_screen") {
            AboutScreen(navController, mainViewModel)
        }


    }
}
