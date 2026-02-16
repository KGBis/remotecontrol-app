package io.github.kgbis.remotecontrol.app.core.repository

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import io.github.kgbis.remotecontrol.app.features.devices.model.shutdownDelayOptions
import io.github.kgbis.remotecontrol.app.ui.theme.ThemeMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.temporal.ChronoUnit

class SettingsRepositoryImpl(context: Context) : SettingsRepository {

    private val dataStore = context.applicationContext.settingsDataStore

    companion object {
        val KEY_SHUTDOWN_DELAY_AMOUNT = intPreferencesKey("delay_amount")
        val KEY_SHUTDOWN_DELAY_UNIT = stringPreferencesKey("delay_unit")
        val KEY_AUTO_REFRESH = booleanPreferencesKey("auto_refresh_enabled")
        val KEY_AUTO_REFRESH_DELAY = intPreferencesKey("auto_refresh_delay")
        val KEY_THEME = stringPreferencesKey("theme")
    }

    override val shutdownDelayFlow: Flow<Int> =
        dataStore.data.map { prefs ->
            prefs[KEY_SHUTDOWN_DELAY_AMOUNT] ?: shutdownDelayOptions[0].amount
        }

    override val shutdownUnitFlow: Flow<ChronoUnit> =
        dataStore.data.map { prefs ->
            prefs[KEY_SHUTDOWN_DELAY_UNIT]?.let { ChronoUnit.valueOf(it) }
                ?: shutdownDelayOptions[0].unit
        }

    override suspend fun saveShutdownDelay(delay: Int) {
        dataStore.edit { prefs ->
            prefs[KEY_SHUTDOWN_DELAY_AMOUNT] = delay
        }
    }

    override suspend fun saveShutdownUnit(unit: ChronoUnit) {
        dataStore.edit { prefs ->
            prefs[KEY_SHUTDOWN_DELAY_UNIT] = unit.name
        }
    }

    override val autoRefreshEnabledFlow: Flow<Boolean> =
        dataStore.data.map { prefs ->
            prefs[KEY_AUTO_REFRESH] ?: true
        }

    override val autoRefreshIntervalFlow: Flow<Int> =
        dataStore.data.map { prefs ->
            prefs[KEY_AUTO_REFRESH_DELAY] ?: 15
        }

    override suspend fun saveAutorefreshEnabled(status: Boolean) {
        dataStore.edit { prefs ->
            prefs[KEY_AUTO_REFRESH] = status
        }
    }

    override suspend fun saveAutorefreshDelay(delay: Float) {
        dataStore.edit { prefs ->
            prefs[KEY_AUTO_REFRESH_DELAY] = delay.toInt()
        }
    }

    override val theme: Flow<ThemeMode> =
        dataStore.data.map { prefs ->
            prefs[KEY_THEME]?.let { ThemeMode.valueOf(it) } ?: ThemeMode.SYSTEM
        }

    override suspend fun saveTheme(theme: ThemeMode) {
        dataStore.edit { prefs ->
            prefs[KEY_THEME] = theme.name
        }
    }
}
