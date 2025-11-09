package com.example.remote.shutdown.ui.screens
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.remote.shutdown.ui.components.DeviceItem
import com.example.remote.shutdown.viewmodel.MainViewModel

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MainScreen(navController: NavController, viewModel: MainViewModel) {
    val devices by viewModel.devices.collectAsState()
    var delay by remember { mutableStateOf("60") }
    var unit by remember { mutableStateOf("s") }
    var showSnackbar by remember { mutableStateOf<String?>(null) }

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
        Box(Modifier.padding(padding)) {
            Column(Modifier.padding(16.dp)) {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
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
                                    // ejemplo de MAC hardcodeado
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
                    modifier = Modifier.align(Alignment.BottomCenter).padding(16.dp),
                    action = {
                        TextButton(onClick = { showSnackbar = null }) { Text("OK") }
                    }
                ) { Text(it) }
            }
        }
    }
}
