package com.example.remote.shutdown.navigation

import android.content.Context
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.remote.shutdown.ui.screens.AboutScreen
import com.example.remote.shutdown.ui.screens.AddOrEditDeviceScreen
import com.example.remote.shutdown.ui.screens.MainScreen
import com.example.remote.shutdown.viewmodel.MainViewModel

@Composable
fun NavGraph(context: Context) {
    val navController = rememberNavController()

    // shared ViewModel
    val mainViewModel: MainViewModel = viewModel()

    NavHost(navController = navController, startDestination = "main") {
        composable("main") {
            MainScreen(navController, mainViewModel)
        }
        composable("add_device") {
            AddOrEditDeviceScreen(navController, mainViewModel, context)
        }

        composable("edit_device/{ip}") { backStackEntry ->
            val ip = backStackEntry.arguments?.getString("ip")!!
            val device = mainViewModel.getDeviceByIp(ip)
            AddOrEditDeviceScreen(
                navController = navController,
                viewModel = mainViewModel,
                context = LocalContext.current,
                deviceToEdit = device
            )
        }

        composable("about") {
            AboutScreen(navController)
        }


    }
}
