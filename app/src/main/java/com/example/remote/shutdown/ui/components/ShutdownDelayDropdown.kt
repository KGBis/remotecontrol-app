package com.example.remote.shutdown.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material.icons.filled.ArrowDropUp
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
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.DpOffset
import androidx.compose.ui.unit.dp
import com.example.remote.shutdown.R
import com.example.remote.shutdown.data.ShutdownDelayOption
import com.example.remote.shutdown.viewmodel.MainViewModel

@Composable
fun ShutdownDelayDropdown(
    viewModel: MainViewModel,
    options: List<ShutdownDelayOption>
) {
    var expanded by remember { mutableStateOf(false) }

    // Current delay and time unit
    val currentDelay by viewModel.shutdownDelay.collectAsState()
    val currentUnit by viewModel.shutdownUnit.collectAsState()

    // Selected option in dropdown which matches delay + time unit
    val selectedOption = options.find {
        it.amount == currentDelay.toLong() && it.unit == currentUnit
    } ?: options.first()

    // to center drop down
    var parentWidth by remember { mutableStateOf(0.dp) }
    var menuWidth by remember { mutableStateOf(0.dp) }
    val density = LocalDensity.current

    // Horizontal offset for dropdown
    val horizontalOffset = (parentWidth - menuWidth) / 2

    Box(
        /*modifier = Modifier
            .fillMaxWidth()
            .onGloballyPositioned { coords ->
                @Suppress("AssignedValueIsNeverRead")
                parentWidth = coords.size.width.toDp()
            },*/
        modifier = Modifier
            .fillMaxWidth()
            .onGloballyPositioned { coords ->
                // coords.size.width está en píxeles (Int)
                parentWidth = with(density) { coords.size.width.toDp() }
            },
        contentAlignment = Alignment.Center
    ) {
        OutlinedButton(
            onClick = { expanded = true },
            modifier = Modifier
                .wrapContentWidth()
        ) {
            Text(stringResource(selectedOption.labelRes), textAlign = TextAlign.Center)
            Spacer(modifier = Modifier.width(8.dp))
            Icon(
                imageVector = if (expanded) Icons.Default.ArrowDropUp else Icons.Default.ArrowDropDown,
                contentDescription = stringResource(R.string.shutdown_delay_desc)
            )
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = Modifier
                .width(IntrinsicSize.Max)
                .onGloballyPositioned { layout ->
                    @Suppress("AssignedValueIsNeverRead")
                    menuWidth = with(density) { layout.size.width.toDp() }
                },
            offset = DpOffset(horizontalOffset, 0.dp)
        ) {
            options.forEach { option ->
                DropdownMenuItem(
                    text = { Text(stringResource(option.labelRes), textAlign = TextAlign.Center) },
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
