package io.github.kgbis.remotecontrol.app.features.settings

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
