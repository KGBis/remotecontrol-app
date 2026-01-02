package io.github.kgbis.remotecontrol.app.ui.components

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.stringResource
import androidx.navigation.NavController
import io.github.kgbis.remotecontrol.app.R
import io.github.kgbis.remotecontrol.app.features.settings.ui.SettingsDialog
import io.github.kgbis.remotecontrol.app.features.settings.SettingsViewModel

@Suppress("AssignedValueIsNeverRead")
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AppTopBar(navController: NavController, settingsVm: SettingsViewModel) {
    var showConfigDialog by remember { mutableStateOf(false) }

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
        SettingsDialog(settingsViewModel = settingsVm, onClose = { showConfigDialog = false })
    }
}
