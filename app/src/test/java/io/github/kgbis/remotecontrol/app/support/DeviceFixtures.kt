/*
 * Remote PC Control
 * Copyright (C) 2026 Enrique García (https://github.com/KGBis)
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program. If not, see <https://www.gnu.org/licenses/>.
 */
package io.github.kgbis.remotecontrol.app.support

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
