package com.example.remote.shutdown.repository

import android.content.Context
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.floatPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.time.temporal.ChronoUnit

class SettingsRepository(private val context: Context) {

    private val Context.dataStore by preferencesDataStore("settings_prefs")

    companion object {
        val KEY_DELAY_AMOUNT = intPreferencesKey("delay_amount")
        val KEY_DELAY_UNIT = stringPreferencesKey("delay_unit")

        val KEY_AUTO_REFRESH = booleanPreferencesKey("auto_refresh")

        val KEY_AUTO_REFRESH_DELAY = floatPreferencesKey("auto_refresh_delay")
    }

    /* Initialize delay and time unit values in case not saved yet */
    val shutdownDelayFlow: Flow<Int> =
        context.dataStore.data.map { prefs ->
            prefs[KEY_DELAY_AMOUNT] ?: 15   // valor por defecto (15s como en tu VM)
        }

    val shutdownUnitFlow: Flow<ChronoUnit> =
        context.dataStore.data.map { prefs ->
            prefs[KEY_DELAY_UNIT]?.let { ChronoUnit.valueOf(it) } ?: ChronoUnit.SECONDS
        }

    suspend fun saveShutdownDelay(delay: Int) {
        context.dataStore.edit { prefs ->
            prefs[KEY_DELAY_AMOUNT] = delay
        }
    }

    suspend fun saveShutdownUnit(unit: ChronoUnit) {
        context.dataStore.edit { prefs ->
            prefs[KEY_DELAY_UNIT] = unit.name
        }
    }

    /* Initialize auto refresh values in case not saved yet */
    /* Always start with auto-refresh enabled */
    val autoRefreshIntervalFlow: Flow<Float> =
        context.dataStore.data.map { prefs ->
            prefs[KEY_AUTO_REFRESH_DELAY] ?: 15f   // defaults to 15s
        }

    val autoRefreshEnabledFlow: Flow<Boolean> =
        context.dataStore.data.map { prefs ->
            prefs[KEY_AUTO_REFRESH] ?: true
        }

    suspend fun saveAutorefreshDelay(delay: Float) {
        context.dataStore.edit { prefs ->
            prefs[KEY_AUTO_REFRESH_DELAY] = delay
        }
    }

    suspend fun saveAutorefresh(status: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[KEY_AUTO_REFRESH] = status
        }
    }
}