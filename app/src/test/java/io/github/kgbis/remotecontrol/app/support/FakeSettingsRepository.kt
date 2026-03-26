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
package io.github.kgbis.remotecontrol.app.support

import io.github.kgbis.remotecontrol.app.core.repository.SettingsRepository
import io.github.kgbis.remotecontrol.app.features.devices.model.shutdownDelayOptions
import io.github.kgbis.remotecontrol.app.ui.theme.ThemeMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import java.time.temporal.ChronoUnit

class FakeSettingsRepository : SettingsRepository {

    private val _delay = MutableStateFlow(shutdownDelayOptions[0].amount)
    private val _unit = MutableStateFlow(shutdownDelayOptions[0].unit)

    private val _theme = MutableStateFlow(ThemeMode.SYSTEM)

    private val _autoRefreshEnabled = MutableStateFlow(true)
    private val _autoRefreshDelay = MutableStateFlow(15)

    override val shutdownDelayFlow: Flow<Int> = _delay
    override val shutdownUnitFlow: Flow<ChronoUnit> = _unit

    override val theme: Flow<ThemeMode> = _theme

    override val autoRefreshEnabledFlow: Flow<Boolean> = _autoRefreshEnabled
    override val autoRefreshIntervalFlow: Flow<Int> = _autoRefreshDelay

    override suspend fun saveShutdownDelay(delay: Int) {
        _delay.value = delay
    }

    override suspend fun saveShutdownUnit(unit: ChronoUnit) {
        _unit.value = unit
    }

    override suspend fun saveTheme(theme: ThemeMode) {
        _theme.value = theme
    }

    override suspend fun saveAutorefreshEnabled(status: Boolean) {
        _autoRefreshEnabled.value = status
    }

    override suspend fun saveAutorefreshDelay(delay: Float) {
        _autoRefreshDelay.value = delay.toInt()
    }
}