package com.example.remote.shutdown.data

data class DeviceStatus(
    var isOnline: Boolean? = null,
    var canWakeup: Boolean? = null,
    var canShutdown: Boolean? = null
)