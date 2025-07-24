package com.example.remote.shutdown.ui.composable

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.navigation.NavController
import com.example.remote.shutdown.R
import com.example.remote.shutdown.network.NetworkScanViewModel
import com.example.remote.shutdown.network.NetworkScanner


@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun NetworkScannerScreen(scanner: NetworkScanner, navController: NavController) {
    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Text(
                        stringResource(R.string.scan_devices)
                    )
                },
                navigationIcon = {
                    IconButton(onClick = { navController.popBackStack() }) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "Back")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.primary,
                    titleContentColor = Color.White
                )
            )
        },
        content = { padding ->
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(padding),
                contentAlignment = Alignment.TopCenter
            ) {
                Text("Device scanner UI goes here")
            }
        }
    )
}


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
