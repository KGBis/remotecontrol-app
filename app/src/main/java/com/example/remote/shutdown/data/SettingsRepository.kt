package com.example.remote.shutdown.data

import android.content.Context
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.concurrent.TimeUnit

class SettingsRepository(private val context: Context) {

    private val Context.dataStore by preferencesDataStore("settings_prefs")

    companion object {
        val KEY_DELAY_AMOUNT = intPreferencesKey("delay_amount")
        val KEY_DELAY_UNIT = stringPreferencesKey("delay_unit")
    }

    val shutdownDelayFlow: Flow<Int> =
        context.dataStore.data.map { prefs ->
            prefs[KEY_DELAY_AMOUNT] ?: 15   // valor por defecto (15s como en tu VM)
        }

    val shutdownUnitFlow: Flow<TimeUnit> =
        context.dataStore.data.map { prefs ->
            prefs[KEY_DELAY_UNIT]?.let { TimeUnit.valueOf(it) } ?: TimeUnit.SECONDS
        }

    suspend fun saveShutdownDelay(delay: Int) {
        context.dataStore.edit { prefs ->
            prefs[KEY_DELAY_AMOUNT] = delay
        }
    }

    suspend fun saveShutdownUnit(unit: TimeUnit) {
        context.dataStore.edit { prefs ->
            prefs[KEY_DELAY_UNIT] = unit.name
        }
    }
}
