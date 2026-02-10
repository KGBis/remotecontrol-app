package io.github.kgbis.remotecontrol.app.features.devices.ui

import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavController
import io.github.kgbis.remotecontrol.app.R
import io.github.kgbis.remotecontrol.app.features.devices.DevicesViewModel
import io.github.kgbis.remotecontrol.app.features.settings.SettingsViewModel
import io.github.kgbis.remotecontrol.app.ui.components.AppTopBar

@Suppress("AssignedValueIsNeverRead")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(
    navController: NavController,
    devicesVm: DevicesViewModel,
    settingsVm: SettingsViewModel
) {
    var showSnackbar by remember { mutableStateOf<String?>(null) }
    val snackbarHostState = remember { SnackbarHostState() }

    var pullToRefreshIsRefreshing by remember { mutableStateOf(false) }
    val pullToRefreshState = rememberPullToRefreshState()

    // All mainscreen side and disposable effects
    MainScreenSideEffects(devicesVm, showSnackbar, snackbarHostState)
    {
        @Suppress("AssignedValueIsNeverRead")
        showSnackbar = null
    }

    Scaffold(
        snackbarHost = { SnackbarHost(snackbarHostState) },
        topBar = {
            AppTopBar(
                navController = navController,
                settingsVm = settingsVm
            )
        },
        floatingActionButton = {
            FloatingActionButton(
                onClick = { navController.navigate("add_device") }
            ) {
                Icon(Icons.Default.Add, contentDescription = stringResource(R.string.add_device))
            }
        }
    ) { padding ->
        PullToRefreshBox(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize(),
            state = pullToRefreshState,
            isRefreshing = pullToRefreshIsRefreshing,
            onRefresh = {

                pullToRefreshIsRefreshing = true
                devicesVm.probeDevices()
                pullToRefreshIsRefreshing = false
            }
        ) {
            MainScreenContent(
                navController = navController,
                devicesVm = devicesVm,
                settingsVm = settingsVm
            ) {
                showSnackbar = it
            }
        }
    }
}
