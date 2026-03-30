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
 *
 * SPDX-License-Identifier: GPL-3.0-or-later
 */
package io.github.kgbis.remotecontrol.app.core.model

import com.google.gson.Gson
import com.google.gson.GsonBuilder
import com.google.gson.JsonDeserializationContext
import com.google.gson.JsonDeserializer
import com.google.gson.JsonElement
import com.google.gson.JsonObject
import com.google.gson.JsonPrimitive
import com.google.gson.JsonSerializationContext
import com.google.gson.JsonSerializer
import java.lang.reflect.Type
import java.time.Instant

object DeviceJson {

    private val instantAdapter: JsonSerializer<Instant> =
        object : JsonSerializer<Instant>, JsonDeserializer<Instant> {
            override fun serialize(
                src: Instant,
                typeOfSrc: Type,
                context: JsonSerializationContext
            ): JsonElement =
                JsonPrimitive(src.toString())

            override fun deserialize(
                json: JsonElement,
                typeOfT: Type,
                context: JsonDeserializationContext
            ): Instant =
                Instant.parse(json.asString)
        }

    private val pendingActionAdapter: JsonSerializer<PendingAction> =
        object : JsonSerializer<PendingAction>, JsonDeserializer<PendingAction> {

            override fun serialize(
                src: PendingAction,
                typeOfSrc: Type,
                context: JsonSerializationContext
            ): JsonElement {
                return when (src) {
                    PendingAction.None ->
                        JsonObject().apply {
                            addProperty("type", "None")
                        }

                    is PendingAction.ShutdownScheduled ->
                        JsonObject().apply {
                            addProperty("type", "ShutdownScheduled")
                            addProperty("cancellable", src.cancellable)
                            add("scheduledAt", context.serialize(src.scheduledAt))
                            add("executeAt", context.serialize(src.executeAt))
                        }
                }
            }

            override fun deserialize(
                json: JsonElement,
                typeOfT: Type,
                context: JsonDeserializationContext
            ): PendingAction {
                val obj = json.asJsonObject
                val type = obj["type"]?.asString ?: "None"

                return when (type) {
                    "None" -> PendingAction.None

                    "ShutdownScheduled" ->
                        PendingAction.ShutdownScheduled(
                            scheduledAt = context.deserialize(
                                obj["scheduledAt"],
                                Instant::class.java
                            ),
                            executeAt = context.deserialize(obj["executeAt"], Instant::class.java),
                            cancellable = obj["cancellable"].asBoolean
                        )

                    else -> error("Unknown PendingAction type: $type")
                }
            }
        }

    val gson: Gson = GsonBuilder()
        .registerTypeAdapter(Instant::class.java, instantAdapter)
        .registerTypeAdapter(PendingAction::class.java, pendingActionAdapter)
        .create()
}