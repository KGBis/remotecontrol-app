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

class SettingsRepository(context: Context) {

    private val dataStore = context.applicationContext.settingsDataStore

    companion object {
        val KEY_SHUTDOWN_DELAY_AMOUNT = intPreferencesKey("delay_amount")
        val KEY_SHUTDOWN_DELAY_UNIT = stringPreferencesKey("delay_unit")
        val KEY_AUTO_REFRESH = booleanPreferencesKey("auto_refresh_enabled")
        val KEY_AUTO_REFRESH_DELAY = intPreferencesKey("auto_refresh_delay")
        val KEY_TIMEOUT = intPreferencesKey("scan_timeout")
        val KEY_THEME = stringPreferencesKey("theme")
    }

    val shutdownDelayFlow: Flow<Int> =
        dataStore.data.map { prefs ->
            prefs[KEY_SHUTDOWN_DELAY_AMOUNT] ?: shutdownDelayOptions[0].amount
        }

    val shutdownUnitFlow: Flow<ChronoUnit> =
        dataStore.data.map { prefs ->
            prefs[KEY_SHUTDOWN_DELAY_UNIT]?.let { ChronoUnit.valueOf(it) }
                ?: shutdownDelayOptions[0].unit
        }

    suspend fun saveShutdownDelay(delay: Int) {
        dataStore.edit { prefs ->
            prefs[KEY_SHUTDOWN_DELAY_AMOUNT] = delay
        }
    }

    suspend fun saveShutdownUnit(unit: ChronoUnit) {
        dataStore.edit { prefs ->
            prefs[KEY_SHUTDOWN_DELAY_UNIT] = unit.name
        }
    }

    val autoRefreshEnabledFlow: Flow<Boolean> =
        dataStore.data.map { prefs ->
            prefs[KEY_AUTO_REFRESH] ?: true
        }

    val autoRefreshIntervalFlow: Flow<Int> =
        dataStore.data.map { prefs ->
            prefs[KEY_AUTO_REFRESH_DELAY] ?: 15
        }

    suspend fun saveAutorefreshEnabled(status: Boolean) {
        dataStore.edit { prefs ->
            prefs[KEY_AUTO_REFRESH] = status
        }
    }

    suspend fun saveAutorefreshDelay(delay: Float) {
        dataStore.edit { prefs ->
            prefs[KEY_AUTO_REFRESH_DELAY] = delay.toInt()
        }
    }

    val socketTimeoutFlow: Flow<Int> =
        dataStore.data.map { prefs ->
            prefs[KEY_TIMEOUT] ?: 500
        }

    suspend fun saveSocketTimeout(delay: Float) {
        dataStore.edit { prefs ->
            prefs[KEY_TIMEOUT] = delay.toInt()
        }
    }

    val theme: Flow<ThemeMode> =
        dataStore.data.map { prefs ->
            prefs[KEY_THEME]?.let { ThemeMode.valueOf(it) } ?: ThemeMode.SYSTEM
        }

    suspend fun saveTheme(theme: ThemeMode) {
        dataStore.edit { prefs ->
            prefs[KEY_THEME] = theme.name
        }
    }
}
