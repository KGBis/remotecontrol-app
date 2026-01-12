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