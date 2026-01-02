package io.github.kgbis.remotecontrol.app.features.settings.ui

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import io.github.kgbis.remotecontrol.app.ui.theme.ThemeMode

@Composable
fun ThemeModeSelector(
    selected: ThemeMode,
    onSelected: (ThemeMode) -> Unit
) {
    Column {
        Text("Modo de tema", style = MaterialTheme.typography.titleMedium)

        ThemeMode.entries.forEach { mode ->
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onSelected(mode) }
                    .padding(vertical = 8.dp)
            ) {
                RadioButton(
                    selected = selected == mode,
                    onClick = { onSelected(mode) }
                )
                Text(
                    text = when (mode) {
                        ThemeMode.LIGHT -> "Claro"
                        ThemeMode.DARK -> "Oscuro"
                        ThemeMode.SYSTEM -> "Automático"
                    },
                    modifier = Modifier.padding(start = 8.dp)
                )
            }
        }
    }
}
