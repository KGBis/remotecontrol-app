package io.github.kgbis.remotecontrol.app.features.settings.ui

import android.util.Log
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
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
    // var showAdvanced by remember { mutableStateOf(false) }

    val colorSchemeVm by settingsViewModel.colorScheme.collectAsState()
    var colorScheme by remember { mutableStateOf(colorSchemeVm) }

    val autoRefreshEnabledVm by settingsViewModel.autoRefreshEnabled.collectAsState()
    val autoRefreshIntervalVm by settingsViewModel.autoRefreshInterval.collectAsState()

    var autoRefreshEnabled by remember { mutableStateOf(autoRefreshEnabledVm) }
    var autoRefreshInterval by remember { mutableFloatStateOf(autoRefreshIntervalVm.toFloat()) }

    LaunchedEffect(autoRefreshEnabledVm, autoRefreshIntervalVm) {
        autoRefreshEnabled = autoRefreshEnabledVm
        autoRefreshInterval = autoRefreshIntervalVm.toFloat()
    }

    AlertDialog(
        onDismissRequest = { onClose() },
        title = { Text(stringResource(R.string.settings)) },
        text = {
            Column {
                ThemeModeSelector(selected = colorScheme) {
                    colorScheme = it
                    settingsViewModel.setColorScheme(it)
                }

                HorizontalDivider()
                Spacer(Modifier.height(8.dp))

                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(stringResource(R.string.settings_refresh_enabled))
                    Switch(
                        checked = autoRefreshEnabled,
                        onCheckedChange = {
                            autoRefreshEnabled = it
                            settingsViewModel.setAutoRefreshEnabled(it)
                        }
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
                    onValueChange = {
                        autoRefreshInterval = it
                        settingsViewModel.setAutoRefreshInterval(it)
                    },
                    valueRange = 10f..60f
                )
            }
        },
        confirmButton = {
            TextButton(onClick = {
                onClose()
            })
            { Text(stringResource(R.string.back)) }
        }
    )
}