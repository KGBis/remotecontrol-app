/*
 * Remote PC Control
 * Copyright (C) 2026 Enrique García (https://github.com/KGBis)
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 */
package io.github.kgbis.remotecontrol.app.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import io.github.kgbis.remotecontrol.app.R
import kotlinx.coroutines.delay

@Composable
fun ValidatingTextField(
    value: String,
    modifier: Modifier = Modifier,
    onValueChange: (String) -> Unit,
    label: String,
    validator: (String) -> Boolean,
    debounceMillis: Long = 750,
    errorMessage: Int = R.string.generic_validation_error
) {
    var isValid by remember { mutableStateOf(true) }
    var showError by remember { mutableStateOf(false) }

    // Debounce validation
    LaunchedEffect(value) {
        delay(debounceMillis)
        isValid = validator(value)
        showError = value.isNotEmpty()
    }

    Column(modifier) {
        OutlinedTextField(
            value = value,
            onValueChange = { onValueChange(it) },
            label = { Text(label) },
            isError = showError && !isValid,
            singleLine = true,
            modifier = Modifier.fillMaxWidth()
        )

        if (showError && !isValid) {
            Text(
                text = stringResource(errorMessage),
                color = MaterialTheme.colorScheme.error,
                style = MaterialTheme.typography.bodySmall
            )
        }
    }
}
