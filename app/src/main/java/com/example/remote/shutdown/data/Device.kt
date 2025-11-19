package com.example.remote.shutdown.data

import android.util.Log
import java.util.Locale.getDefault

data class Device(
    var name: String,
    var ip: String,
    var mac: String
) {
    /**
     * Trims all the fields and normalizes MAC address to colon separated & uppercase (i.e. 91:75:1A:EC:9A:C7)
     */
    fun normalize() {
        Log.i("normalize", "Before: name = '$name', ip = '$ip', mac = '$mac'")
        name = name.trim()
        ip.trim()
        mac = mac.trim().replace('-', ':').uppercase(getDefault())
        Log.i("normalize", "After : name = '$name', ip = '$ip', mac = '$mac'")
    }
}

