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
 *
 * SPDX-License-Identifier: GPL-3.0-or-later
 */
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
