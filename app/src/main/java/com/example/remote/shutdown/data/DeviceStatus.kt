package com.example.remote.shutdown.data

data class DeviceStatus(
    var state: State? = State.Unknown,
    var isOnline: Boolean? = false,
    var canWakeup: Boolean? = false,
    var canShutdown: Boolean? = false
)