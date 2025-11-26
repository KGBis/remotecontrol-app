package com.example.remote.shutdown.ui.components

import android.util.Log
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckBoxOutlineBlank
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Checkbox
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.ListItem
import androidx.compose.material3.ListItemDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import com.example.remote.shutdown.R
import com.example.remote.shutdown.data.Device
import com.example.remote.shutdown.viewmodel.MainViewModel

@Composable
fun DetectedDevicesList(
    results: List<Device>,
    navController: NavController,
    viewModel: MainViewModel
) {
    var multiSelectMode by remember { mutableStateOf(false) }
    val selected = remember { mutableStateListOf<String>() } // IP as id

    Column(Modifier.fillMaxSize()) {

        // Button when multiselect is true
        if (multiSelectMode && selected.isNotEmpty()) {
            Button(
                modifier = Modifier
                    .fillMaxWidth(0.75f)
                    .padding(vertical = 8.dp)
                    .align(Alignment.CenterHorizontally),
                onClick = {
                    val devicesToAdd = results.filter { it.ip in selected }
                    Log.i("DetectedDevicesList", "Devices to add -> $devicesToAdd")
                    viewModel.addDevices(devicesToAdd)
                    selected.clear()
                    multiSelectMode = false
                    navController.popBackStack()
                }
            ) {
                Text(stringResource(R.string.devices_found_add_button, selected.size))
            }
        }

        LazyColumn {
            items(results.size) { i ->
                val d = results[i]
                val isSelected = d.ip in selected

                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(vertical = 4.dp)
                        .clickable {
                            if (multiSelectMode) {
                                // MULTISELECTION
                                if (isSelected) selected.remove(d.ip) else selected.add(d.ip)
                                if (selected.isEmpty()) multiSelectMode = false
                            } else {
                                // NORMAL MODE → add one
                                viewModel.addDevice(d)
                                navController.popBackStack()
                            }
                        },
                    colors = CardDefaults.cardColors(
                        containerColor = if (isSelected)
                            MaterialTheme.colorScheme.secondaryContainer
                        else
                            MaterialTheme.colorScheme.surfaceVariant
                    ),
                    shape = RoundedCornerShape(12.dp),
                    elevation = CardDefaults.cardElevation(4.dp)
                ) {
                    ListItem(
                        overlineContent = { Text(d.name) },
                        headlineContent = { Text(d.ip) },
                        supportingContent = { Text(d.mac) },
                        colors = ListItemDefaults.colors(
                            containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.90f)
                        ),
                        trailingContent = {
                            Row(verticalAlignment = Alignment.CenterVertically) {

                                // ✨ autodetected
                                if (d.mac.isNotBlank()) {
                                    Text(
                                        if (d.mac.isNotBlank()) "✨ ${stringResource(R.string.app_name)}" else "",
                                        Modifier.padding(end = 6.dp)
                                    )
                                }

                                // ✔️ Checkbox
                                if (multiSelectMode) {
                                    Checkbox(
                                        checked = isSelected,
                                        onCheckedChange = { checked ->
                                            if (checked) selected.add(d.ip)
                                            else selected.remove(d.ip)

                                            if (selected.isEmpty()) multiSelectMode = false
                                        }
                                    )
                                } else {
                                    // "checkbox" icon. When any is clicked, enter in multiselect mode
                                    IconButton(onClick = {
                                        multiSelectMode = true
                                        selected.add(d.ip)
                                    }) {
                                        Icon(
                                            Icons.Default.CheckBoxOutlineBlank,
                                            contentDescription = stringResource(R.string.select)
                                        )
                                    }
                                }
                            }
                        }
                    )
                }

                Spacer(Modifier.height(4.dp))
            }
        }
    }
}
