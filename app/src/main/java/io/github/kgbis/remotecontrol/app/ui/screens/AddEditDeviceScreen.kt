package io.github.kgbis.remotecontrol.app.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuAnchorType
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import io.github.kgbis.remotecontrol.app.R
import io.github.kgbis.remotecontrol.app.features.devices.model.DeviceFormMode
import io.github.kgbis.remotecontrol.app.features.devices.model.DeviceFormState
import io.github.kgbis.remotecontrol.app.features.devices.model.InterfaceFormState
import io.github.kgbis.remotecontrol.app.core.model.InterfaceType
import io.github.kgbis.remotecontrol.app.ui.components.ValidatingTextField
import io.github.kgbis.remotecontrol.app.core.util.Utils
import io.github.kgbis.remotecontrol.app.core.util.Utils.options

@Composable
fun AddEditDeviceScreen(
    mode: DeviceFormMode,
    state: DeviceFormState,
    onStateChange: (DeviceFormState) -> Unit,
    onSave: () -> Unit,
) {
    // ---------- GENERAL INFO ----------
    Text(
        text = stringResource(R.string.device_general_info),
        style = MaterialTheme.typography.titleMedium
    )

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
        ) {

            OutlinedTextField(
                modifier = Modifier.fillMaxWidth(),
                value = state.hostname,
                onValueChange = {
                    onStateChange(state.copy(hostname = it.trim()))
                },
                label = { Text(stringResource(R.string.device_name)) },
                isError = state.hostname.isBlank(),
                singleLine = true
            )

            OsDropdown(
                onValueChange = {
                    onStateChange(state.copy(osName = it))
                },
                modifier = Modifier.fillMaxWidth(),
                value = state.osName
            )

            Spacer(Modifier.height(8.dp))

        }
    }

    // ---------- INTERFACES ----------
    Text(
        text = stringResource(R.string.network_interfaces),
        style = MaterialTheme.typography.titleMedium
    )

    state.interfaces.forEachIndexed { index, iface ->
        InterfaceEditorCard(
            state = iface,
            onChange = { updated ->
                val newList = state.interfaces.toMutableList()
                newList[index] = updated
                onStateChange(state.copy(interfaces = newList))
            },
            onRemove = {
                val newList = state.interfaces.toMutableList()
                newList.removeAt(index)
                onStateChange(state.copy(interfaces = newList))
            },
            canRemove = state.interfaces.size > 1
        )

        Spacer(Modifier.height(8.dp))
    }

    OutlinedButton(
        onClick = {
            onStateChange(
                state.copy(
                    interfaces = state.interfaces + InterfaceFormState()
                )
            )
        }
    ) {
        Icon(Icons.Default.Add, contentDescription = null)
        Spacer(Modifier.width(8.dp))
        Text(stringResource(R.string.add_interface))
    }

    Spacer(Modifier.height(32.dp))

    // ---------- SAVE ----------

    val allIpsValid =
        state.interfaces.isNotEmpty() && state.interfaces.all { Utils.isValidIpv4(it.ip) }

    Button(
        modifier = Modifier.fillMaxWidth(),
        enabled = state.hostname.isNotBlank() && allIpsValid,
        onClick = onSave
    ) {
        Text(
            if (mode == DeviceFormMode.CREATE)
                stringResource(R.string.add_device)
            else
                stringResource(R.string.save_device)
        )
    }

}

@Composable
fun InterfaceEditorCard(
    state: InterfaceFormState,
    onChange: (InterfaceFormState) -> Unit,
    onRemove: () -> Unit,
    canRemove: Boolean
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp)
    ) {
        Column(
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 4.dp),
        ) {

            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(
                    text = stringResource(R.string.interface_name),
                    style = MaterialTheme.typography.titleSmall,
                    modifier = Modifier
                        .weight(1f)
                        .padding(12.dp)
                )

                if (canRemove) {
                    IconButton(onClick = onRemove) {
                        Icon(
                            imageVector = Icons.Default.Delete,
                            contentDescription = stringResource(R.string.remove_interface)
                        )
                    }
                }
            }

            ValidatingTextField(
                value = state.ip.trim(),
                validator = { it.isNotEmpty() && Utils.isValidIpv4(it) },
                onValueChange = { onChange(state.copy(ip = it.trim())) },
                label = stringResource(R.string.device_ip),
                modifier = Modifier.fillMaxWidth(),
                errorMessage = R.string.error_invalid_ip
            )

            ValidatingTextField(
                value = state.mac,
                validator = Utils::isValidMacOptional,
                onValueChange = { onChange(state.copy(mac = it)) },
                label = stringResource(R.string.device_mac),
                modifier = Modifier.fillMaxWidth(),
                errorMessage = R.string.error_invalid_mac
            )

            InterfaceTypeDropdown(
                value = state.type,
                modifier = Modifier.fillMaxWidth(),
                onValueChange = { onChange(state.copy(type = it)) },
            )

            Spacer(Modifier.height(8.dp))
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun OsDropdown(
    value: String?,
    onValueChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded },
        modifier = modifier
    ) {
        OutlinedTextField(
            modifier = Modifier
                .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable, true)
                .fillMaxWidth(),
            readOnly = true,
            value = value ?: "",
            onValueChange = {},
            label = { Text(stringResource(R.string.os_name)) },
            trailingIcon = {
                ExposedDropdownMenuDefaults.TrailingIcon(expanded)
            }
        )

        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            options.forEach { os ->
                DropdownMenuItem(
                    text = { Text(os) },
                    onClick = {
                        onValueChange(os)
                        expanded = false
                    }
                )
            }
        }
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun InterfaceTypeDropdown(
    value: InterfaceType,
    onValueChange: (InterfaceType) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }

    ExposedDropdownMenuBox(
        expanded = expanded,
        onExpandedChange = { expanded = !expanded },
        modifier = modifier
    ) {
        OutlinedTextField(
            modifier = Modifier
                .menuAnchor(ExposedDropdownMenuAnchorType.PrimaryNotEditable, true)
                .fillMaxWidth(),
            readOnly = true,
            value = stringResource(value.labelRes),
            onValueChange = {},
            label = { Text(stringResource(R.string.interface_type)) },
            trailingIcon = {
                ExposedDropdownMenuDefaults.TrailingIcon(expanded)
            }
        )

        ExposedDropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            InterfaceType.entries.forEach { type ->
                DropdownMenuItem(
                    text = { Text(stringResource(type.labelRes)) },
                    onClick = {
                        onValueChange(type)
                        expanded = false
                    }
                )
            }
        }
    }
}
