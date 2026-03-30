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
package io.github.kgbis.remotecontrol.app.features.settings

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import io.github.kgbis.remotecontrol.app.core.repository.SettingsRepository
import io.github.kgbis.remotecontrol.app.features.devices.model.shutdownDelayOptions
import io.github.kgbis.remotecontrol.app.ui.theme.ThemeMode
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.temporal.ChronoUnit

class SettingsViewModel(
    application: Application,
    val settingsRepo: SettingsRepository
) : AndroidViewModel(application) {

    /* Color scheme */

    val colorScheme =
        settingsRepo.theme.stateIn(viewModelScope, SharingStarted.Eagerly, ThemeMode.SYSTEM)

    /* Shutdown delay and time unit */

    val shutdownDelay = settingsRepo.shutdownDelayFlow.stateIn(
        viewModelScope,
        SharingStarted.Eagerly,
        shutdownDelayOptions[0].amount
    )

    val shutdownUnit = settingsRepo.shutdownUnitFlow.stateIn(
        viewModelScope,
        SharingStarted.Eagerly,
        shutdownDelayOptions[0].unit
    )


    /* Auto-refresh settings */

    val autoRefreshEnabled =
        settingsRepo.autoRefreshEnabledFlow
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), true)

    val autoRefreshInterval =
        settingsRepo.autoRefreshIntervalFlow
            .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5_000), 30)

    /* Functions */

    fun changeDelay(newDelay: Int) {
        viewModelScope.launch {
            settingsRepo.saveShutdownDelay(newDelay)
        }
    }

    fun changeUnit(newUnit: ChronoUnit) {
        viewModelScope.launch {
            settingsRepo.saveShutdownUnit(newUnit)
        }
    }

    fun setAutoRefreshEnabled(value: Boolean) {
        viewModelScope.launch {
            settingsRepo.saveAutorefreshEnabled(value)
        }
    }

    fun setAutoRefreshInterval(value: Float) {
        viewModelScope.launch {
            settingsRepo.saveAutorefreshDelay(value)
        }
    }

    /* Color scheme (LIGHT, DARK, SYSTEM) */

    fun setColorScheme(theme: ThemeMode) {
        viewModelScope.launch {
            settingsRepo.saveTheme(theme)
        }
    }
}