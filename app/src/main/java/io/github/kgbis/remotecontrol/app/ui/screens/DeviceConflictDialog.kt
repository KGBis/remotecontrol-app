package io.github.kgbis.remotecontrol.app.ui.screens

import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import io.github.kgbis.remotecontrol.app.R
import io.github.kgbis.remotecontrol.app.features.domain.ConflictResult

@Composable
fun DeviceConflictDialog(
    conflict: ConflictResult,
    onConfirm: () -> Unit,
    onDismiss: () -> Unit
) {
    when (conflict) {
        is ConflictResult.MacConflict -> {
            AlertDialog(
                title = { Text(stringResource(R.string.device_same_mac)) },
                text = {
                    Text(
                        stringResource(
                            R.string.device_same_mac_text,
                            conflict.device.hostname,
                            "\n${conflict.macs}"
                        )
                    )
                },
                confirmButton = {
                    TextButton(onClick = onConfirm) {
                        Text(stringResource(R.string.save_anyway))
                    }
                },
                dismissButton = {
                    TextButton(onClick = onDismiss) {
                        Text(stringResource(R.string.cancel))
                    }
                },
                onDismissRequest = onDismiss
            )
        }

        is ConflictResult.IpConflict -> {
            AlertDialog(
                title = { Text(stringResource(R.string.device_same_ip)) },
                text = {
                    Text(
                        stringResource(
                            R.string.device_same_ip_text,
                            conflict.device.hostname,
                            "\n${conflict.ips}"
                        )
                    )
                },
                confirmButton = {
                    TextButton(onClick = onConfirm) {
                        Text(stringResource(R.string.save_anyway))
                    }
                },
                dismissButton = {
                    TextButton(onClick = onDismiss) {
                        Text(stringResource(R.string.cancel))
                    }
                },
                onDismissRequest = onDismiss
            )
        }

        is ConflictResult.PossibleDuplicate -> {
            AlertDialog(
                title = { Text(stringResource(R.string.device_duplicate)) },
                text = {
                    Text(stringResource(R.string.device_duplicate_text, conflict.device.hostname))
                },
                confirmButton = {
                    TextButton(onClick = onConfirm) {
                        Text(stringResource(R.string.save_anyway))
                    }
                },
                dismissButton = {
                    TextButton(onClick = onDismiss) {
                        Text(stringResource(R.string.cancel))
                    }
                },
                onDismissRequest = onDismiss
            )
        }

        ConflictResult.None -> Unit
    }
}
