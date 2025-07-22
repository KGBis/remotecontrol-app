package com.example.remote.shutdown.ui.composable

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.remote.shutdown.network.NetworkScanViewModel

@Composable
fun NetworkScannerScreen(
    viewModel: NetworkScanViewModel = viewModel(), modifier: Modifier
) {
    val activeIps by viewModel.activeIps.collectAsState()

    Column(modifier = modifier) {
        Button(onClick = { viewModel.scanNetwork() }) {
            Text("Scan Network")
        }

        Spacer(modifier = Modifier.height(16.dp))

        if (activeIps.isEmpty()) {
            Text("No active devices found yet.")
        } else {
            Text("Active devices:")

            LazyColumn {
                items(activeIps) { ip ->
                    Text("✅ $ip", style = MaterialTheme.typography.bodyLarge)
                }
            }
        }
    }
}
