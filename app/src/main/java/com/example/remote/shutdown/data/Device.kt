package com.example.remote.shutdown.data

data class Device(
    val name: String,
    val ip: String,
    val mac: String = ""
)

