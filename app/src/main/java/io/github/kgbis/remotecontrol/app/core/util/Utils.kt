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
package io.github.kgbis.remotecontrol.app.core.util

import android.app.Application
import android.util.Log
import java.net.Inet4Address
import java.net.InetAddress

object Utils {

    val options = listOf("Windows", "Linux", "macOS")

    fun isValidIpv4(ip: String): Boolean =
        runCatching { InetAddress.getByName(ip) }
            .getOrNull() is Inet4Address

    fun String.ipAsInt(): Int =
        split('.').fold(0) { acc, part ->
            (acc shl 8) + part.toInt()
        }


    fun isValidMacOptional(mac: String): Boolean =
        mac.isBlank() || isValidMac(mac)

    fun isValidMac(mac: String): Boolean {
        val regex = Regex("^([0-9A-Fa-f]{2}([-:])){5}[0-9A-Fa-f]{2}$")
        return regex.matches(mac)
    }


    fun loadAboutKeys(context: Application): Map<String, String> {
        return try {
            return context.assets.open("about.sections.txt")
                .bufferedReader()
                .readLines()
                .filter {
                    it.isNotBlank() && !it.startsWith("#")
                }.mapNotNull { line ->
                    val trimmed = line.trim()
                    if (!trimmed.contains("=")) return@mapNotNull null
                    val parts = trimmed.split("=", limit = 2)
                    parts[0].trim() to parts[1].trim()
                }.toMap(LinkedHashMap())
        } catch (e: Exception) {
            Log.e("AboutKeys", "Failed to load about.sections.txt", e)
            emptyMap()
        }
    }
}