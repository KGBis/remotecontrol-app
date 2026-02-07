package io.github.kgbis.remotecontrol.app.features.settings

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import io.github.kgbis.remotecontrol.app.RemotePcControlApp
import io.github.kgbis.remotecontrol.app.core.repository.SettingsRepositoryContract
import io.github.kgbis.remotecontrol.app.features.devices.model.shutdownDelayOptions
import io.github.kgbis.remotecontrol.app.ui.theme.ThemeMode
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.time.temporal.ChronoUnit

class SettingsViewModel(
    application: Application,
    val settingsRepo: SettingsRepositoryContract
) : AndroidViewModel(application)
 {

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