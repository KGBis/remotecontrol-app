package com.example.remote.shutdown.common

import android.content.Context
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import com.example.remote.shutdown.model.Device
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import androidx.core.content.edit

class UserDevices/*(private val context: Context)*/ {

    fun saveDeviceList(context: Context, devices: List<Device>) {
        val sharedPref = context.getSharedPreferences("DevicePrefs", Context.MODE_PRIVATE)
        val json = Gson().toJson(devices)
        sharedPref.edit { putString("device_list", json) }
    }

    fun loadDeviceList(context: Context): List<Device> {
        val sharedPref = context.getSharedPreferences("DevicePrefs", Context.MODE_PRIVATE)
        val json = sharedPref.getString("device_list", null) ?: return emptyList()
        val type = object : TypeToken<List<Device>>() {}.type
        return Gson().fromJson(json, type)
    }


}
