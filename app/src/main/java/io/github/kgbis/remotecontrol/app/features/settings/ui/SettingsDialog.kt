package io.github.kgbis.remotecontrol.app.features.settings.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import io.github.kgbis.remotecontrol.app.R
import io.github.kgbis.remotecontrol.app.features.settings.SettingsViewModel

@Composable
fun SettingsDialog(
    settingsViewModel: SettingsViewModel,
    onClose: () -> Unit,
) {
    // advanced options
    var showAdvanced by remember { mutableStateOf(false) }

    val autoRefreshEnabledVm by settingsViewModel.autoRefreshEnabled.collectAsState()
    val autoRefreshIntervalVm by settingsViewModel.autoRefreshInterval.collectAsState()
    val socketTimeoutVm by settingsViewModel.socketTimeout.collectAsState()

    var autoRefreshEnabled by remember { mutableStateOf(autoRefreshEnabledVm) }
    var autoRefreshInterval by remember { mutableStateOf(autoRefreshIntervalVm.toFloat()) }
    var socketTimeout by remember { mutableStateOf(socketTimeoutVm.toFloat()) }

    LaunchedEffect(autoRefreshEnabledVm, autoRefreshIntervalVm, socketTimeoutVm) {
        autoRefreshEnabled = autoRefreshEnabledVm
        autoRefreshInterval = autoRefreshIntervalVm.toFloat()
        socketTimeout = socketTimeoutVm.toFloat()
    }

    AlertDialog(
        onDismissRequest = { onClose() },
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
                        onCheckedChange = { autoRefreshEnabled = it }
                    )
                }

                Spacer(Modifier.height(8.dp))

                Text(
                    stringResource(
                        R.string.settings_refresh_rate,
                        autoRefreshInterval.toInt()
                    )
                )
                Slider(
                    value = autoRefreshInterval,
                    onValueChange = { autoRefreshInterval = it },
                    valueRange = 5f..60f
                )

                Spacer(Modifier.height(8.dp))

                TextButton(onClick = { showAdvanced = !showAdvanced }) {
                    Text(stringResource(R.string.settings_advanced))
                }

                if (showAdvanced) {
                    Spacer(Modifier.height(8.dp))
                    Text(stringResource(R.string.settings_advanced_timeout, socketTimeout.toInt()))
                    Slider(
                        value = socketTimeout,
                        onValueChange = { socketTimeout = it },
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
            TextButton(onClick = {
                settingsViewModel.setAutoRefreshEnabled(autoRefreshEnabled)
                settingsViewModel.setAutoRefreshInterval(autoRefreshInterval)
                settingsViewModel.setSocketTimeout(socketTimeout)
                onClose()
            })
            { Text(stringResource(R.string.save_device)) }
        }
    )
}