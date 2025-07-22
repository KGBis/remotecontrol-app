package com.example.remote.shutdown.common

import android.content.Context
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.dataStore by preferencesDataStore(name = "settings")

const val COMPUTER_IP = "computer_ip"

class UserPreferences(private val context: Context) {
    private val switchKey = booleanPreferencesKey("switch_state")

    private val ipKey = stringPreferencesKey(COMPUTER_IP)

    val switchFlow: Flow<Boolean> = context.dataStore.data
        .map { prefs -> prefs[switchKey] ?: false }

    suspend fun saveSwitchState(state: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[switchKey] = state
        }
    }

    val computerIp: Flow<String> =
        context.dataStore.data.map { preferences -> preferences[ipKey] ?: ""}

    suspend fun saveComputerIp(ip: String) {
        context.dataStore.edit { prefs ->
            prefs[ipKey] = ip
        }
    }
}
