package com.example.remote.shutdown.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import com.example.remote.shutdown.data.ShutdownDelayOption
import com.example.remote.shutdown.viewmodel.MainViewModel

@Composable
fun ShutdownDelayDropdown(
    viewModel: MainViewModel,
    options: List<ShutdownDelayOption>
) {
    var expanded by remember { mutableStateOf(false) }

    // Flujos actuales
    val currentDelay by viewModel.shutdownDelay.collectAsState()
    val currentUnit by viewModel.shutdownUnit.collectAsState()

    // El label del item seleccionado
    val selectedOption = options.find {
        it.amount == currentDelay.toLong() && it.unit == currentUnit
    } ?: options.first()

    Box {
        OutlinedButton(
            onClick = { expanded = true },
            modifier = Modifier.fillMaxWidth()
        ) {
            Text(stringResource(selectedOption.labelRes))
            Spacer(modifier = Modifier.width(8.dp))
            Icon(
                imageVector = Icons.Default.ArrowDropDown,
                contentDescription = "Seleccionar tiempo"
            )
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier.fillMaxWidth()
        ) {
            options.forEach { option ->
                DropdownMenuItem(
                    text = { Text(stringResource(option.labelRes)) },
                    onClick = {
                        viewModel.changeDelay(option.amount.toInt())
                        viewModel.changeUnit(option.unit)
                        expanded = false
                    }
                )
            }
        }
    }
}
