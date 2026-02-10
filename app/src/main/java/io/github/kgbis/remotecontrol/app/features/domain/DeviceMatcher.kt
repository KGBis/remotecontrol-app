package io.github.kgbis.remotecontrol.app.features.domain

import io.github.kgbis.remotecontrol.app.core.model.Device
import org.apache.commons.text.similarity.FuzzyScore
import java.util.Locale

/**
 * Device matching is probabilistic and conservative.
 * Unmistakable signal:
 *  - Id
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
class DeviceMatcher(
    private val config: MatchConfig = MatchConfig(),
    private val stored: List<Device>
) {

    fun findDeviceToAdd(inDevice: Device): Device? {
        val all = stored

        val bestMatch = all
            .map { device -> device to deviceScoreMatch(device, inDevice) }
            .maxByOrNull { it.second }

        val (device, score) = bestMatch ?: return null
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
            return config.idExact // absolute match
        }

        // MAC
        val storedMacs =
            stored.interfaces.mapNotNull { it.mac }.map { it.trim() }.filter { it.isNotEmpty() }
                .toSet()
        val incomingMacs =
            incoming.interfaces.mapNotNull { it.mac }.map { it.trim() }.filter { it.isNotEmpty() }
                .toSet()
        if (storedMacs.intersect(incomingMacs).isNotEmpty()) {
            score += config.macWeight
        }

        // IPs
        val storedIps = stored.interfaces.mapNotNull { it.ip }.toSet()
        val incomingIps = incoming.interfaces.mapNotNull { it.ip }.toSet()

        if (storedIps.intersect(incomingIps).isNotEmpty()) {
            score += config.ipMatch
        }

        // IP + MAC mismatch
        val macConflict =
            storedMacs.isNotEmpty() &&
                    incomingMacs.isNotEmpty() &&
                    storedMacs.intersect(incomingMacs).isEmpty()

        val ipMatch = storedIps.intersect(incomingIps).isNotEmpty()

        if (macConflict && ipMatch) {
            score -= config.macConflictPenalty
        }

        // Hostname
        val storedHost = normalizeHostname(stored.hostname)
        val incomingHost = normalizeHostname(incoming.hostname)

        if (storedHost == incomingHost) {
            score += config.hostnameExact
        } else {
            val fuzzy = FuzzyScore(Locale.ROOT).fuzzyScore(storedHost, incomingHost)
            when {
                fuzzy >= 8 -> score += config.hostnameFuzzyHigh
                fuzzy >= 5 -> score += config.hostnameFuzzyLow
            }
        }

        // OS info
        if (
            stored.deviceInfo?.osName != null && incoming.deviceInfo?.osName != null &&
            stored.deviceInfo.osName.startsWith(incoming.deviceInfo.osName, true) ||
            incoming.deviceInfo!!.osName.startsWith(stored.deviceInfo!!.osName, true)
        ) {
            score += config.osMatch
        }


        // OS Version
        if (stored.deviceInfo.osVersion == incoming.deviceInfo.osVersion) {
            score += config.osVersion
        }

        // Interfaces count (low value)
        if (stored.interfaces.size == incoming.interfaces.size) {
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
