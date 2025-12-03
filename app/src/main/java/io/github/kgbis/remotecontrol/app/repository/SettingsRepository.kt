package io.github.kgbis.remotecontrol.app.repository

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import io.github.kgbis.remotecontrol.app.ui.theme.ThemeMode
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.temporal.ChronoUnit

class SettingsRepository(private val context: Context) {

    private val Context.dataStore by preferencesDataStore("settings_prefs")

    companion object {
        // Shutdown action delay amount and unit
        val KEY_SHUTDOWN_DELAY_AMOUNT = intPreferencesKey("delay_amount")
        val KEY_SHUTDOWN_DELAY_UNIT = stringPreferencesKey("delay_unit")

        // User added devices autorefresh
        val KEY_AUTO_REFRESH = booleanPreferencesKey("auto_refresh_enabled")
        val KEY_AUTO_REFRESH_DELAY = intPreferencesKey("auto_refresh_delay")

        // Network scan timeout
        val KEY_TIMEOUT = intPreferencesKey("scan_timeout")

        // Theme (light, dark or system)
        val KEY_THEME = stringPreferencesKey("theme")
    }

    /* Shutdown action delay amount and unit */
    val shutdownDelayFlow: Flow<Int> =
        context.dataStore.data.map { prefs ->
            prefs[KEY_SHUTDOWN_DELAY_AMOUNT] ?: 15
        }   // defaults to 15 seconds


    val shutdownUnitFlow: Flow<ChronoUnit> =
        context.dataStore.data.map { prefs ->
            prefs[KEY_SHUTDOWN_DELAY_UNIT]?.let { ChronoUnit.valueOf(it) } ?: ChronoUnit.SECONDS
        } // defaults to seconds

    suspend fun saveShutdownDelay(delay: Int) {
        context.dataStore.edit { prefs -> prefs[KEY_SHUTDOWN_DELAY_AMOUNT] = delay }
    }

    suspend fun saveShutdownUnit(unit: ChronoUnit) {
        context.dataStore.edit { prefs -> prefs[KEY_SHUTDOWN_DELAY_UNIT] = unit.name }
    }

    /* User added devices autorefresh */
    val autoRefreshEnabledFlow: Flow<Boolean> =
        context.dataStore.data.map { prefs -> prefs[KEY_AUTO_REFRESH] ?: true }

    val autoRefreshIntervalFlow: Flow<Int> =
        context.dataStore.data.map { prefs ->
            prefs[KEY_AUTO_REFRESH_DELAY] ?: 15
        } // defaults to 15s

    suspend fun saveAutorefreshEnabled(status: Boolean) {
        context.dataStore.edit { prefs -> prefs[KEY_AUTO_REFRESH] = status }
    }

    suspend fun saveAutorefreshDelay(delay: Float) {
        context.dataStore.edit { prefs -> prefs[KEY_AUTO_REFRESH_DELAY] = delay.toInt() }
    }


    /* Network scan timeout */
    val socketTimeoutFlow: Flow<Int> =
        context.dataStore.data.map { prefs -> prefs[KEY_TIMEOUT] ?: 500 }   // defaults to 500 ms

    suspend fun saveSocketTimeout(delay: Float) {
        context.dataStore.edit { prefs ->
            prefs[KEY_TIMEOUT] = delay.toInt()
        }
    }

    /* Theme (light, dark or system) */
    val theme: Flow<ThemeMode> = context.dataStore.data.map { prefs ->
        if (prefs[KEY_THEME] == null) {
            ThemeMode.SYSTEM
        } else {
            ThemeMode.valueOf(prefs[KEY_THEME]!!)
        }
    }

    suspend fun saveTheme(theme: ThemeMode) {
        context.dataStore.edit { prefs ->
            prefs[KEY_THEME] = theme.name
        }
    }
}