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
    application: Application
) : AndroidViewModel(application) {
    private val settingsRepo = SettingsRepository(application)

    /* Color scheme */
    val colorScheme = settingsRepo.theme.stateIn(
        viewModelScope,
        SharingStarted.Companion.WhileSubscribed(5_000), ThemeMode.SYSTEM
    )

    /* Shutdown delay and time unit */

    val shutdownDelay = settingsRepo.shutdownDelayFlow.stateIn(
        viewModelScope,
        SharingStarted.Companion.WhileSubscribed(5_000), shutdownDelayOptions[0].amount
    )

    val shutdownUnit = settingsRepo.shutdownUnitFlow.stateIn(
        viewModelScope,
        SharingStarted.Companion.WhileSubscribed(5_000), shutdownDelayOptions[0].unit
    )
    /* Auto-refresh settings */

    val autoRefreshEnabled = settingsRepo.autoRefreshEnabledFlow.stateIn(
        viewModelScope,
        SharingStarted.Companion.WhileSubscribed(5_000), true
    )

    val autoRefreshInterval = settingsRepo.autoRefreshIntervalFlow.stateIn(
        viewModelScope,
        SharingStarted.Companion.WhileSubscribed(5_000), 30
    )

    /* Network scan (socket) Timeout */

    val socketTimeout = settingsRepo.socketTimeoutFlow.stateIn(
        viewModelScope,
        SharingStarted.Companion.WhileSubscribed(5_000), 500
    )

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

    fun setSocketTimeout(value: Float) {
        viewModelScope.launch {
            settingsRepo.saveSocketTimeout(value)
        }
    }

    fun setColorScheme(theme: ThemeMode) {
        viewModelScope.launch {
            settingsRepo.saveTheme(theme)
        }
    }
}