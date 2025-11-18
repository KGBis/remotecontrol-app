package com.example.remote.shutdown.util

object Validators {

    fun isValidIp(ip: String): Boolean {
        val regex = Regex(
            pattern = "^((25[0-5]|2[0-4]\\d|1\\d{2}|[1-9]?\\d)\\.){3}(25[0-5]|2[0-4]\\d|1\\d{2}|[1-9]?\\d)$"
        )
        return regex.matches(ip.trim())
    }

    fun isValidMac(mac: String): Boolean {
        val regex = Regex(
            pattern = "^([0-9A-Fa-f]{2}([-:])){5}[0-9A-Fa-f]{2}$"
        )
        return regex.matches(mac.trim())
    }


}