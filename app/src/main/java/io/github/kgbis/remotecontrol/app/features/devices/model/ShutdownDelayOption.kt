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
package io.github.kgbis.remotecontrol.app.features.devices.model

import io.github.kgbis.remotecontrol.app.R
import java.time.temporal.ChronoUnit

data class ShutdownDelayOption(
    val labelRes: Int,
    val amount: Int,
    val unit: ChronoUnit
)

val shutdownDelayOptions = listOf(
    ShutdownDelayOption(R.string.delay_now, 0, ChronoUnit.SECONDS),
    ShutdownDelayOption(R.string.delay_1m, 1, ChronoUnit.MINUTES),
    ShutdownDelayOption(R.string.delay_5m, 5, ChronoUnit.MINUTES),
    ShutdownDelayOption(R.string.delay_10m, 10, ChronoUnit.MINUTES),
    ShutdownDelayOption(R.string.delay_15m, 15, ChronoUnit.MINUTES),
    ShutdownDelayOption(R.string.delay_30m, 30, ChronoUnit.MINUTES),
    ShutdownDelayOption(R.string.delay_1h, 1, ChronoUnit.HOURS),
)
