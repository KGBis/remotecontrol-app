package com.example.remote.shutdown.navigation

import androidx.compose.runtime.Composable
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.rememberNavController
import com.example.remote.shutdown.ui.screens.MainScreen
import com.example.remote.shutdown.ui.screens.AddDeviceScreen
import com.example.remote.shutdown.viewmodel.MainViewModel

@Composable
fun NavGraph() {
    val navController = rememberNavController()
    // Creamos un ViewModel compartido para toda la NavGraph
    val mainViewModel: MainViewModel = viewModel()

    NavHost(navController = navController, startDestination = "main") {
        composable("main") {
            MainScreen(navController, mainViewModel)
        }
        composable("add_device") {
            AddDeviceScreen(navController, mainViewModel)
        }
    }
}
