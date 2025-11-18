package com.example.remote.shutdown.navigation

import android.content.Context
import android.util.Log
import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.remote.shutdown.ui.screens.MainScreen
import com.example.remote.shutdown.ui.screens.AddOrEditDeviceScreen
import com.example.remote.shutdown.viewmodel.MainViewModel

@Composable
fun NavGraph(context: Context) {
    val navController = rememberNavController()
    // Creamos un ViewModel compartido para toda la NavGraph
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
            Log.i("NavGraph", "Editing card with IP $ip")
            val device = mainViewModel.getDeviceByIp(ip)
            Log.i("NavGraph", "Device to edit is $device")
            AddOrEditDeviceScreen(
                navController = navController,
                viewModel = mainViewModel,
                context = LocalContext.current,
                deviceToEdit = device
            )
        }

    }
}
