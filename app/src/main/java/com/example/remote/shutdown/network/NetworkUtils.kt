package com.example.remote.shutdown.network

import java.net.InetAddress
import java.net.InetSocketAddress
import java.net.Socket

object NetworkUtils {

    /**
     * Comprueba si un puerto TCP está abierto en la IP dada.
     */
    fun isHostReachable(ip: String, port: Int, timeoutMs: Int = 300): Boolean {
        return try {
            Socket().use { socket ->
                socket.connect(InetSocketAddress(ip, port), timeoutMs)
                true
            }
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Ping por InetAddress (suele fallar en Android pero se incluye como fallback).
     */
    fun pingInetAddress(ip: String, timeoutMs: Int = 500): Boolean {
        return try {
            InetAddress.getByName(ip).isReachable(timeoutMs)
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Ping usando el binario del sistema ("ping").
     * Suele ser más fiable en Android.
     */
    fun pingCommand(ip: String, count: Int = 1, timeoutSec: Int = 1): Boolean {
        return try {
            val process = Runtime.getRuntime()
                .exec("/system/bin/ping -c $count -W $timeoutSec $ip")

            val exitCode = process.waitFor()
            exitCode == 0
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Probar si el PC está "activo": puerto de servidor + ping como fallback.
     *
     * Ideal para Remote Shutdown Tray (por defecto puerto 5000).
     */
    fun isPcOnline(ip: String, port: Int = 5000): Boolean {
        // 1) Si el puerto responde, el PC está encendido y el servidor activo
        if (isHostReachable(ip, port)) return true

        // 2) Fallback: ping por comando
        if (pingCommand(ip)) return true

        // 3) Fallback adicional: ping InetAddress
        return pingInetAddress(ip)
    }
}
