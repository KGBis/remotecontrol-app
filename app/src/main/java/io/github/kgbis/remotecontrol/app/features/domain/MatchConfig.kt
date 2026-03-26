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
package io.github.kgbis.remotecontrol.app.features.domain

data class MatchConfig(
    val idExact: Int = 100,
    val macWeight: Int = 60,
    val hostnameExact: Int = 30,
    val hostnameFuzzyHigh: Int = 20,
    val hostnameFuzzyLow: Int = 10,
    val osMatch: Int = 10,
    val ipMatch: Int = 10,
    val osVersion: Int = 5,
    val interfaceSize: Int = 5,
    val threshold: Int = 55,
    val macConflictPenalty: Int = 40
)

