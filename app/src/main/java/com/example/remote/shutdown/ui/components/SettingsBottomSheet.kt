package com.example.remote.shutdown.ui.components

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.Slider
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.remote.shutdown.R

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun SettingsBottomSheet(
    enabled: Boolean,
    interval: Float,
    onEnabledChange: (Boolean) -> Unit,
    onIntervalChange: (Float) -> Unit,
    onAboutClick: () -> Unit,
    onDismiss: () -> Unit
) {
    ModalBottomSheet(onDismissRequest = onDismiss) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp)
        ) {
            Text(
                stringResource(R.string.settings),
                style = MaterialTheme.typography.titleLarge,
                modifier = Modifier
                    .padding(bottom = 16.dp)
                    .fillMaxWidth(),
                // textAlign = TextAlign.Center
            )
            SectionCard(title = "Lista de dispositivos") {
                // Switch
                Row(
                    Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        stringResource(R.string.settings_refresh_enabled),
                        modifier = Modifier.weight(1f)
                    )
                    Switch(checked = enabled, onCheckedChange = onEnabledChange)
                }

                // Slider
                Text(
                    "${stringResource(R.string.settings_refresh_rate)}: ${interval.toInt()} ${
                        stringResource(
                            R.string.label_seconds
                        )
                    }"
                )
                Slider(
                    value = interval,
                    onValueChange = onIntervalChange,
                    valueRange = 10f..60f,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        Column(
            modifier = Modifier
                .padding(horizontal = 24.dp)
        ) {
            Text(
                // ${stringResource(R.string.settings_refresh_rate)}
                "Scan network Timeout: ${interval.toInt()} ${
                    stringResource(
                        R.string.label_milliseconds
                    )
                }"
            )
            val interval2 = 500f
            Slider(
                value = interval2,
                onValueChange = { /*onIntervalChange*/ },
                valueRange = 300f..5000f,
                modifier = Modifier.fillMaxWidth()
            )

            Spacer(Modifier.height(20.dp))

            // About
            TextButton(
                onClick = onAboutClick,
                modifier = Modifier.align(Alignment.End)
            ) {
                Text(stringResource(R.string.about))
            }

            Spacer(Modifier.height(8.dp))
        }
    }
}
