package com.example.remote.shutdown.data

data class Device(
    var name: String,
    val ip: String,
    var mac: String? = null,
    var isOnline: Boolean?  = null,
    var canWakeup: Boolean? = null,
    var canShutdown: Boolean? = null,
)

