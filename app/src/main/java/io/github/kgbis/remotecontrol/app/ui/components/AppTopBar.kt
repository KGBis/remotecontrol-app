package io.github.kgbis.remotecontrol.app.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontVariation
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import io.github.kgbis.remotecontrol.app.R
import io.github.kgbis.remotecontrol.app.viewmodel.MainViewModel

@Suppress("AssignedValueIsNeverRead")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppTopBar(navController: NavController, viewModel: MainViewModel) {
    var showConfigDialog by remember { mutableStateOf(false) }
    var showAdvanced by remember { mutableStateOf(false) }

    val autoRefreshEnabled by viewModel.autoRefreshEnabled.collectAsState()
    val autoRefreshInterval by viewModel.autoRefreshInterval.collectAsState()
    val socketTimeout by viewModel.socketTimeout.collectAsState()

    TopAppBar(
        title = { Text("⚡ " + stringResource(R.string.app_name)) },
        actions = {
            var expanded by remember { mutableStateOf(false) }
            IconButton(onClick = { expanded = true }) {
                Icon(
                    Icons.Default.MoreVert,
                    contentDescription = stringResource(R.string.label_menu)
                )
            }
            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false }
            ) {
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.settings)) },
                    onClick = {
                        expanded = false
                        showConfigDialog = true
                    }
                )
                DropdownMenuItem(
                    text = { Text(stringResource(R.string.about)) },
                    onClick = {
                        expanded = false
                        navController.navigate("about_screen")
                    }
                )
            }
        }
    )

    // --- Configuration Dialog ---
    if (showConfigDialog) {
        SettingsDialog(viewModel = viewModel, onDismiss = { showConfigDialog = it })
        /*AlertDialog(
            onDismissRequest = { showConfigDialog = false },
            title = { Text(stringResource(R.string.settings)) },
            text = {
                Column {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(stringResource(R.string.settings_refresh_enabled))
                        Switch(
                            checked = autoRefreshEnabled,
                            onCheckedChange = { viewModel.setAutoRefreshEnabled(it) }
                        )
                    }

                    Spacer(Modifier.height(8.dp))

                    Text(
                        stringResource(
                            R.string.settings_refresh_rate,
                            autoRefreshInterval
                        )
                    )
                    Slider(
                        value = autoRefreshInterval.toFloat(),
                        onValueChange = { viewModel.setAutoRefreshInterval(it) },
                        valueRange = 5f..60f
                    )

                    Spacer(Modifier.height(8.dp))

                    TextButton(onClick = { showAdvanced = !showAdvanced }) {
                        Text(stringResource(R.string.settings_advanced))
                    }

                    if (showAdvanced) {
                        Spacer(Modifier.height(8.dp))
                        Text(stringResource(R.string.settings_advanced_timeout, socketTimeout))
                        Slider(
                            value = socketTimeout.toFloat(),
                            onValueChange = { viewModel.setSocketTimeout(it) },
                            valueRange = 100f..5000f
                        )
                        Text(
                            stringResource(R.string.settings_advanced_timeout_warning),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
            },
            confirmButton = {
                TextButton(onClick = { showConfigDialog = false }) { Text("OK") }
            }
        )*/
    }

    // --- Aquí iría tu lista de dispositivos / contenido principal ---
    // MainDeviceList(viewModel)
}
