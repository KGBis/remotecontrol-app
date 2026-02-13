package io.github.kgbis.remotecontrol.app.utils

import com.google.gson.JsonParser
import io.github.kgbis.remotecontrol.app.core.model.Device
import io.github.kgbis.remotecontrol.app.core.model.DeviceJson

object DeviceFixtures {

    private val gson = DeviceJson.gson
    private val json by lazy {
        readResource("fixtures/devices.json")
    }

    private val root = JsonParser.parseString(json).asJsonObject

    fun device(name: String): Device = gson.fromJson(root[name], Device::class.java)

    fun readResource(path: String): String =
        this::class.java.classLoader!!.getResource(path)!!.readText()

}
