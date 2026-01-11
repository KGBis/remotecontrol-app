package io.github.kgbis.remotecontrol.app.features.domain

import android.util.Log
import io.github.kgbis.remotecontrol.app.core.model.Device
import org.apache.commons.text.similarity.FuzzyScore
import java.util.Locale

/**
 * Device matching is probabilistic and conservative.
 *
 * Strong signals:
 *  - MAC address
 *
 * Weak signals:
 *  - Hostname (fuzzy)
 *  - OS family
 *  - IP (contextual, probe-time only)
 *
 * A match is accepted only if the accumulated score
 * exceeds MATCH_THRESHOLD to avoid false positives.
 */

private val TAG: String = "deviceScoreMatch"

class DeviceMatcher(
    private val config: MatchConfig = MatchConfig(),
    private val stored: List<Device>
) {

    fun findDeviceToAdd(inDevice: Device): Device? {
        val all = stored // _devices.value

        val bestMatch = all
            .map { device -> device to deviceScoreMatch(device, inDevice) }
            .maxByOrNull { it.second }

        val (device, score) = bestMatch ?: return null

        Log.d(TAG, "Best match score=$score for ${device.hostname}")

        val threshold = if (isIdentityUpgrade(device, inDevice)) 30 else 55

        return if (score >= threshold) device else null

    }

    private fun deviceScoreMatch(
        stored: Device,
        incoming: Device
    ): Int {
        var score = 0

        // UUID
        if (stored.id == incoming.id) {
            Log.d(TAG, "Ids match")
            return config.idExact // absolute match
        }

        // MAC
        val storedMacs = stored.interfaces.mapNotNull { it.mac }.toSet()
        val incomingMacs = incoming.interfaces.mapNotNull { it.mac }.toSet()
        if (storedMacs.intersect(incomingMacs).isNotEmpty()) {
            Log.d(TAG, "MACs match")
            score += config.macWeight
        }

        // Hostname
        val storedHost = normalizeHostname(stored.hostname)
        val incomingHost = normalizeHostname(incoming.hostname)

        if (storedHost == incomingHost) {
            Log.d(TAG, "Hostname match")
            score += config.hostnameExact
        } else {
            val fuzzy = FuzzyScore(Locale.ROOT).fuzzyScore(storedHost, incomingHost)
            when {
                fuzzy >= 8 -> score += config.hostnameFuzzyHigh
                fuzzy >= 5 -> score += config.hostnameFuzzyLow
            }
            Log.d(TAG, "Hostname similar")
        }

        // OS info
        if (
            stored.deviceInfo?.osName != null && incoming.deviceInfo?.osName != null &&
            stored.deviceInfo!!.osName.startsWith(incoming.deviceInfo!!.osName, true) ||
            incoming.deviceInfo!!.osName.startsWith(stored.deviceInfo!!.osName, true)
        ) {
            Log.d(TAG, "OS family match")
            score += config.osMatch
        }

        // IPs
        val storedIps = stored.interfaces.mapNotNull { it.ip }.toSet()
        val incomingIps = incoming.interfaces.mapNotNull { it.ip }.toSet()

        if (storedIps.intersect(incomingIps).isNotEmpty()) {
            Log.d(TAG, "IPs match")
            score += config.ipMatch
        }


        // OS Version
        if (stored.deviceInfo?.osVersion == incoming.deviceInfo?.osVersion) {
            Log.d(TAG, "OS version matchs")
            score += config.osVersion
        }

        // Interfaces count (low value)
        if (stored.interfaces.size == incoming.interfaces.size) {
            Log.d(TAG, "Same number of network interfaces")
            score += config.interfaceSize
        }

        return score
    }

    private fun normalizeHostname(name: String): String =
        name.lowercase().replace("_", "-").replace(".", "")

    private fun isIdentityUpgrade(
        stored: Device,
        incoming: Device
    ): Boolean {
        val storedHasMac = stored.interfaces.any { !it.mac.isNullOrBlank() }
        val incomingHasMac = incoming.interfaces.any { !it.mac.isNullOrBlank() }

        val storedHasTray = stored.deviceInfo?.trayVersion.isNullOrBlank().not()
        val incomingHasTray = incoming.deviceInfo?.trayVersion.isNullOrBlank().not()

        return (!storedHasMac && incomingHasMac) || (!storedHasTray && incomingHasTray)
    }
}
