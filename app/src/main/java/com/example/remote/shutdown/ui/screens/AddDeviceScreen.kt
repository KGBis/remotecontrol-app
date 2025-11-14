package com.example.remote.shutdown.ui.screens

import android.content.Context
import android.util.Log
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.remote.shutdown.data.Device
import com.example.remote.shutdown.data.NetworkScanner
import com.example.remote.shutdown.network.NetworkRangeDetector
import com.example.remote.shutdown.viewmodel.MainViewModel
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddDeviceScreen(navController: NavController, viewModel: MainViewModel, context: Context) {
    var name by remember { mutableStateOf("") }
    var ip by remember { mutableStateOf("") }
    var scanning by remember { mutableStateOf(false) }
    var results by remember { mutableStateOf(listOf<Device>()) }

    val scope = rememberCoroutineScope()

    val networkRangeDetector = NetworkRangeDetector(context)

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Añadir dispositivo") },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Volver"
                        )
                    }
                }
            )
        }) { padding ->
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
                        val b = System.currentTimeMillis()
                        val networkRange = networkRangeDetector.getLocalNetworkRange()
                        results = NetworkScanner.scanLocalNetwork(baseIp = networkRange?:"192.168.1", maxConcurrent = 30)
                        // val scanNetworkRange = fastNetworkScan()
                        Log.i("Scan", "Time to scan subnet -> ${System.currentTimeMillis() - b} millis")
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
