package com.example.remote.shutdown.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.remote.shutdown.data.Device
import com.example.remote.shutdown.data.NetworkScanner
import com.example.remote.shutdown.viewmodel.MainViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddDeviceScreen(navController: NavController, viewModel: MainViewModel) {
    var name by remember { mutableStateOf("") }
    var ip by remember { mutableStateOf("") }
    var scanning by remember { mutableStateOf(false) }
    var results by remember { mutableStateOf(listOf<Device>()) }

    val scope = rememberCoroutineScope()

    Scaffold(
        topBar = { TopAppBar(title = { Text("Añadir dispositivo") }) }
    ) { padding ->
        Column(Modifier.padding(padding).padding(16.dp)) {
            OutlinedTextField(value = name, onValueChange = { name = it }, label = { Text("Nombre") })
            Spacer(Modifier.height(8.dp))
            OutlinedTextField(value = ip, onValueChange = { ip = it }, label = { Text("Dirección IP") })

            Spacer(Modifier.height(16.dp))

            Button(onClick = {
                if (ip.isNotBlank() && name.isNotBlank()) {
                    viewModel.addDevice(Device(name = name, ip = ip))
                    navController.popBackStack()
                }
            }, modifier = Modifier.fillMaxWidth()) {
                Text("Guardar")
            }

            Spacer(Modifier.height(24.dp))

            OutlinedButton(
                onClick = {
                    scanning = true
                    scope.launch {
                        results = NetworkScanner.scanLocalNetwork()
                        scanning = false
                    }
                },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text(if (scanning) "Escaneando..." else "Escanear red local")
            }

            Spacer(Modifier.height(16.dp))

            if (results.isNotEmpty()) {
                Text("Dispositivos detectados:", style = MaterialTheme.typography.titleSmall)
                LazyColumn {
                    items(results.size) { i ->
                        val d = results[i]
                        ListItem(
                            headlineContent = { Text(d.name) },
                            supportingContent = { Text(d.ip) },
                            modifier = Modifier.clickable {
                                viewModel.addDevice(d)
                                navController.popBackStack()
                            }
                        )
                    }
                }
            }
        }
    }
}
