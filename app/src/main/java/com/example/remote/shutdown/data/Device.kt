package com.example.remote.shutdown.data

import java.util.Locale.getDefault

data class Device(
    var name: String,
    var ip: String,
    var mac: String
) {
    /**
     * Trims all the fields and normalizes MAC address to colon separated & lowercase (i.e. 91:75:1a:ec:9a:c7)
     */
    fun normalize() {
        name = name.trim()
        ip.trim()
        mac = mac.trim().replace('-', ':').lowercase(getDefault())
    }
}

