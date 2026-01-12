package io.github.kgbis.remotecontrol.app.features.discovery

import android.util.Log
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
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
import androidx.compose.runtime.snapshots.SnapshotStateList
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.navigation.NavController
import io.github.kgbis.remotecontrol.app.MIN_VERSION
import io.github.kgbis.remotecontrol.app.R
import io.github.kgbis.remotecontrol.app.core.model.Device
import io.github.kgbis.remotecontrol.app.features.discovery.model.DeviceTransformResult
import io.github.kgbis.remotecontrol.app.features.devices.DevicesViewModel
import io.github.kgbis.remotecontrol.app.features.discovery.model.OutdatedWarn
import java.util.UUID

@Composable
fun DetectedDevicesList(
    results: List<DeviceTransformResult>,
    navController: NavController,
    devicesVm: DevicesViewModel
) {
    var multiSelectMode by remember { mutableStateOf(false) }
    val selected = remember { mutableStateListOf<UUID>() } // IP as id

    Log.d(
        "Composabe DetectedDevicesList",
        "List of detected devices (List<DeviceTransformResult>): $results"
    )

    Column(Modifier.fillMaxSize()) {

        // Button when multiselect is true
        if (multiSelectMode && selected.isNotEmpty()) {
            Button(
                modifier = Modifier
                    .fillMaxWidth(0.75f)
                    .padding(vertical = 8.dp)
                    .align(Alignment.CenterHorizontally),
                onClick = {
                    val devicesToAdd = results.mapNotNull {
                        when (it) {
                            is DeviceTransformResult.Ok ->
                                if (it.device.id in selected) it.device else null

                            is DeviceTransformResult.Outdated ->
                                if (it.device?.id in selected) it.device else null

                            else -> null
                        }
                    }

                    devicesVm.addDevices(devicesToAdd)
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
                when (val transformed = results[i]) {
                    is DeviceTransformResult.Invalid -> InvalidDevice(transformed)
                    is DeviceTransformResult.Ok ->
                        CompatibleDevice(
                            selected = selected,
                            multi = multiSelectMode,
                            devicesVm = devicesVm,
                            navController = navController,
                            device = transformed.device,
                            onSelected = {
                                Log.i("onSelected", "result = $it")
                                multiSelectMode = it
                            })

                    is DeviceTransformResult.Outdated -> {
                        if (transformed.device != null) {
                            CompatibleDevice(
                                selected = selected,
                                multi = multiSelectMode,
                                devicesVm = devicesVm,
                                navController = navController,
                                device = transformed.device,
                                outdatedWarn = OutdatedWarn(
                                    transformed.reason,
                                    transformed.reasonText
                                ),
                                onSelected = {
                                    Log.i("onSelected", "result = $it")
                                    multiSelectMode = it
                                })
                        }
                    }
                }


                Spacer(Modifier.height(4.dp))
            }
        }
    }
}

@Composable
fun InvalidDevice(
    invalid: DeviceTransformResult.Invalid
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        border = BorderStroke(
            1.dp,
            MaterialTheme.colorScheme.error
        ),
        onClick = {},
        shape = RoundedCornerShape(12.dp),
        elevation = CardDefaults.cardElevation(4.dp)
    ) {
        ListItem(
            colors = ListItemDefaults.colors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.90f)
            ),
            headlineContent = {
                val hostname = invalid.discovered.txtRecords["host-name"]
                    ?: invalid.discovered.txtRecords["hostname"] ?: ""
                Text(hostname)
            },
            overlineContent = {
                val osName =
                    invalid.discovered.txtRecords["os-name"] ?: invalid.discovered.txtRecords["os"]
                    ?: ""
                Text(osName)
            },
            supportingContent = {
                if (invalid.reasonText != "NO_INTERFACES") {
                    val ips =
                        invalid.discovered.endpoints.joinToString(separator = "\n") { it.ip }
                    Text(ips)
                }
            },
            trailingContent = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(horizontalAlignment = Alignment.End) {
                        Log.d(
                            "",
                            "discover_warn_old_version = ${R.string.discover_warn_old_version}"
                        )
                        listOf(
                            "${stringResource(R.string.discover_upgrade_to, MIN_VERSION)}‼\uFE0F"
                        ).forEach { Text(it, fontWeight = FontWeight.Bold) }
                    }
                }
            }
        )
    }
}

@Composable
fun CompatibleDevice(
    selected: SnapshotStateList<UUID>,
    multi: Boolean,
    devicesVm: DevicesViewModel,
    navController: NavController,
    device: Device,
    outdatedWarn: OutdatedWarn = OutdatedWarn(),
    onSelected: (Boolean) -> Unit
) {
    val isSelected = device.id in selected
    var multiSelectMode by remember { mutableStateOf(multi) }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp)
            .clickable {
                Log.i("CARD", "multiselectmode = $multiSelectMode, multi = $multi")
                if (multiSelectMode) {
                    // MULTISELECTION
                    if (isSelected) selected.remove(device.id) else selected.add(device.id!!)
                    if (selected.isEmpty()) {
                        multiSelectMode = false
                        onSelected(false)
                    }
                } else {
                    // NORMAL MODE → add one
                    devicesVm.addDiscoveredDevice(device)
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
            headlineContent = {
                Text(device.hostname)
            },
            overlineContent = { Text(device.deviceInfo!!.osName) },
            supportingContent = {
                val ips =
                    device.interfaces.joinToString(separator = "\n") { it.ip ?: "" }
                Text(ips)
            },
            colors = ListItemDefaults.colors(
                containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.90f)
            ),
            trailingContent = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Column(horizontalAlignment = Alignment.End) {
                        listOf(
                            "✨ ${stringResource(R.string.app_name)}",
                            "v.${device.deviceInfo!!.trayVersion}"
                        ).forEach { Text(it) }

                        if (outdatedWarn.reason != -1) {
                            listOf(
                                stringResource(outdatedWarn.reason),
                                stringResource(R.string.discover_upgrade_to, outdatedWarn.param),
                                stringResource(R.string.discover_upgrade_later)
                            ).forEach {
                                Text(
                                    it,
                                    fontWeight = FontWeight.Bold,
                                    color = Color(0xFFFF9800)
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    if (multiSelectMode) {
                        Checkbox(
                            checked = isSelected,
                            onCheckedChange = { checked ->
                                if (checked) selected.add(device.id!!)
                                else selected.remove(device.id)
                                if (selected.isEmpty()) multiSelectMode = false
                            }
                        )
                    } else {
                        IconButton(
                            onClick = {
                                Log.i("CARD", "multiselectmode = $multiSelectMode, multi = $multi")
                                multiSelectMode = true
                                onSelected(true)
                                selected.add(device.id!!)
                            }
                        ) {
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
}
