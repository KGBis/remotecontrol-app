package com.example.remote.shutdown.ui.components

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
import com.example.remote.shutdown.R
import kotlinx.coroutines.delay

@Composable
fun ValidatingTextField(
    value: String,
    onValueChange: (String) -> Unit,
    label: String,
    validator: (String) -> Boolean,
    debounceMillis: Long = 750,
    modifier: Modifier = Modifier,
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
