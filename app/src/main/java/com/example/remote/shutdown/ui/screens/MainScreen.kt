package com.example.remote.shutdown.ui.screens

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Snackbar
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.pulltorefresh.PullToRefreshBox
import androidx.compose.material3.pulltorefresh.rememberPullToRefreshState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.remote.shutdown.ui.components.DeviceItem
import com.example.remote.shutdown.viewmodel.MainViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(navController: NavController, viewModel: MainViewModel) {
    val devices by viewModel.devices.collectAsState()
    var delay by remember { mutableStateOf("60") }
    var unit by remember { mutableStateOf("s") }
    var showSnackbar by remember { mutableStateOf<String?>(null) }

    var isRefreshing by remember { mutableStateOf(false) }
    val state = rememberPullToRefreshState()
    val scope = rememberCoroutineScope()

    val onRefresh: () -> Unit = {
        isRefreshing = true
        scope.launch {
            viewModel.loadDevices()
            isRefreshing = false
        }
    }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Remote Shutdown") },
                actions = {
                    IconButton(onClick = { navController.navigate("add_device") }) {
                        Icon(Icons.Default.Add, contentDescription = "Añadir")
                    }
                }
            )
        }
    ) { padding ->
        PullToRefreshBox(
            modifier = Modifier
                .padding(padding)
                .fillMaxSize(),
            state = state,
            isRefreshing = isRefreshing,
            onRefresh = onRefresh
        ) {
            Column(Modifier.padding(16.dp)) {
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    OutlinedTextField(
                        value = delay,
                        onValueChange = { delay = it },
                        label = { Text("Tiempo") },
                        modifier = Modifier.weight(1f)
                    )
                    Spacer(Modifier.width(8.dp))
                    OutlinedTextField(
                        value = unit,
                        onValueChange = { unit = it },
                        label = { Text("Unidad") },
                        modifier = Modifier.weight(1f)
                    )
                }

                Spacer(Modifier.height(16.dp))

                if (devices.isEmpty()) {
                    Text("No hay dispositivos aún.", style = MaterialTheme.typography.bodyMedium)
                } else {
                    LazyColumn {
                        items(devices.size) { index ->
                            val device = devices[index]
                            DeviceItem(
                                device = device,
                                onShutdown = {
                                    viewModel.sendShutdownCommand(device, delay.toIntOrNull() ?: 0, unit) { success ->
                                        showSnackbar = if (success) "Comando enviado a ${device.name}" else "Error con ${device.name}"
                                    }
                                },
                                onWake = {
                                    viewModel.wakeOnLan(device, "00:11:22:33:44:55") { success ->
                                        showSnackbar = if (success) "Wake-on-LAN enviado" else "Fallo en WOL"
                                    }
                                },
                                onDelete = { viewModel.removeDevice(device) }
                            )
                        }
                    }
                }
            }

            showSnackbar?.let {
                Snackbar(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .padding(16.dp),
                    action = {
                        TextButton(onClick = { showSnackbar = null }) { Text("OK") }
                    }
                ) { Text(it) }
            }
        }
    }
}
